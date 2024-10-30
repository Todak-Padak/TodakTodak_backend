package com.padaks.todaktodak.chat.service;

import com.padaks.todaktodak.chat.chatmessage.domain.ChatMessage;
import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageResDto;
import com.padaks.todaktodak.chat.chatmessage.repository.ChatMessageRepository;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.chatroom.dto.ChatRoomListResDto;
import com.padaks.todaktodak.chat.chatroom.dto.ChatRoomMemberInfoResDto;
import com.padaks.todaktodak.chat.chatroom.dto.CsMemberResDto;
import com.padaks.todaktodak.chat.chatroom.repository.ChatRoomRepository;
import com.padaks.todaktodak.chat.cs.repository.CsRepository;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.notification.service.FcmService;
import com.padaks.todaktodak.notification.domain.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.padaks.todaktodak.common.exception.exceptionType.MemberExceptionType.TODAK_ADMIN_ONLY;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final CsRepository csRepository;
    private final FcmService fcmService;


    // 채팅방 생성
    public ChatRoom createChatRoom(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByMemberEmailOrThrow(memberEmail);
        log.info("line:44 memberEmail {}", memberEmail);
        // 새로운 채팅방 생성, member와 admin의 1:1 채팅방
        ChatRoom chatRoom = ChatRoom.builder()
                .member(member)
                .recentChatTime(LocalDateTime.now()) // 채팅방 생성과 함께 최근채팅시간 update (null로 두지 않기 위해서)
                .build();

        //새로운 채팅방 생성시 admin에게 알림
        Member admin = memberRepository.findByMemberEmailOrThrow("todak@test.com");
        fcmService.sendMessage(admin.getMemberEmail(), member.getName(),"새로운 문의 채팅방이 생성되었습니다.", Type.CHAT, null);

        return chatRoomRepository.save(chatRoom);
    }

    // 채팅방 모든 메시지 조회
    public List<ChatMessageResDto> getMessages(Long chatRoomId){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        return chatMessageRepository.findByChatRoom(chatRoom)
                .stream()
                .map(ChatMessageResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 채팅방 삭제
    public void deleteChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        chatRoom.setDeletedTimeAt(LocalDateTime.now()); // 채팅방 deletedAt = 현재시간
    }

    // 해당 회원이 속한 채팅방 리스트 (회원입장 채팅방 리스트)
//    public Page<ChatRoomListResDto> getMemberChatRoomList(Pageable pageable) {
//        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        System.out.println(memberEmail);
//        Member member = memberRepository.findByMemberEmailOrThrow(memberEmail);
//
//        // 해당 회원이 속한 모든 채팅방을 recentChatTime 내림차순으로 페이징 처리하여 조회
//        Page<ChatRoom> chatRoomPage = chatRoomRepository.findByMemberIdOrderByRecentChatTimeDesc(member.getId(), pageable);
//
//        // 각 채팅방의 마지막 메시지와 함께 DTO로 변환
//        return chatRoomPage.map(chatRoom -> {
//            ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomOrderByCreatedAtDesc(chatRoom).orElse(null);
//            return ChatRoomListResDto.fromEntity(chatRoom, lastMessage);
//        });
//    }
    public Page<ChatRoomListResDto> getMemberChatRoomList(Pageable pageable) {
        log.info("Inside ChatService.getMemberChatRoomList with pageable: {}", pageable);
        try {
            String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByMemberEmailOrThrow(memberEmail);
            log.info("Member found: {}", member);

            Page<ChatRoom> chatRoomPage = chatRoomRepository.findByMemberIdOrderByRecentChatTimeDesc(member.getId(), pageable);
            log.info("Retrieved chatRoomPage: {}", chatRoomPage);

            return chatRoomPage.map(chatRoom -> {
                ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomOrderByCreatedAtDesc(chatRoom).orElse(null);
                return ChatRoomListResDto.fromEntity(chatRoom, lastMessage);
            });
        } catch (Exception e) {
            log.error("Error in ChatService.getMemberChatRoomList: {}", e.getMessage(), e);
            throw e;
        }
    }


    // 채팅방 리스트(admin입장 채팅방 리스트)
    public Page<ChatRoomListResDto> getAdminChatRoomList(Pageable pageable){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!memberEmail.equals("todak@test.com")){
            // todak admin만 접근 가능
            throw new BaseException(TODAK_ADMIN_ONLY);
        }

        // 모든 채팅방을 recentChatTime 기준 내림차순으로 페이징 처리하여 조회
        Page<ChatRoom> chatRoomPage = chatRoomRepository.findAllByOrderByRecentChatTimeDesc(pageable);

        // 각 채팅방의 마지막 메시지와 함께 DTO로 변환
        return chatRoomPage.map(chatRoom -> {
            ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomOrderByCreatedAtDesc(chatRoom).orElse(null);
            return ChatRoomListResDto.fromEntity(chatRoom, lastMessage);
        });
    }

    // 채팅방 id로 채팅참여자 정보 조회
    public ChatRoomMemberInfoResDto getChatRoomMemberInfo(Long chatRoomId){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        return ChatRoomMemberInfoResDto.fromEntity(chatRoom.getMember());
    }

//      채팅방 id로 멤버 정보 조회 (todakAdmin)
    public CsMemberResDto getMemberInfo(Long id){
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(id);
        Member member = chatRoom.getMember();

        CsMemberResDto csMemberResDto = new CsMemberResDto(member.getId(), member.getName(), member.getRole(), member.getProfileImgUrl());

        return csMemberResDto;
    }



}
