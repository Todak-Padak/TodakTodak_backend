package com.padaks.todaktodak.chat.service;

import com.padaks.todaktodak.chat.chatmessage.dto.ChatMessageReqDto;
import com.padaks.todaktodak.chat.chatroom.domain.ChatRoom;
import com.padaks.todaktodak.chat.chatroom.repository.ChatRoomRepository;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaListener { // Kafka

    private final WebSocketService webSocketService;
//    private static final Long ADMIN_ID = 1L; // Admin 고정 ID
    private final MemberRepository memberRepository;
    private final FcmService fcmService;
    private final ChatRoomRepository chatRoomRepository;

    @KafkaListener(topics = "chat-topic", containerFactory = "chatKafkaListenerContainerFactory")
    public void listenChatMessages(@Payload ChatMessageReqDto chatMessageReqDto) {
        log.info("Kafka received message: {}", chatMessageReqDto); // 수신된 메시지 로그

        // Kafka로 수신한 메시지를 WebSocket을 통해 전달
        Long chatRoomId = chatMessageReqDto.getChatRoomId();
//        String memberEmail = chatMessageReqDto.getMemberEmail(); // token을 통해 email을 얻는 로직 필요

        // WebSocket을 통해 메시지를 채팅방에 전달
        // 이 시점?
        webSocketService.sendMessage(chatRoomId, chatMessageReqDto);

        String adminEmail = "todak@test.com";
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        String userEmail = chatRoom.getMember().getMemberEmail();

//        if (!memberEmail.equals(adminEmail)){
//            fcmService.sendMessage(adminEmail, chatRoom.getMember().getName()+"님께서 문의 채팅에 내용을 남겼습니다.", chatMessageReqDto.getContents(), Type.CHAT, null);
//        }else {
//            fcmService.sendMessage(userEmail, "문의에 대한 답변이 도착했습니다.", chatMessageReqDto.getContents(), Type.CHAT, null);
//        }

    }
}
