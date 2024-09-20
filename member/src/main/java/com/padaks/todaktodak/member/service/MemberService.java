package com.padaks.todaktodak.member.service;

import com.padaks.todaktodak.config.JwtTokenProvider;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.dto.*;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.util.S3ClientFileUpload;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.util.Random;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenprovider;
    private final PasswordEncoder passwordEncoder;
    private final JavaEmailService emailService;
    private final RedisService redisService;
    private final DtoMapper dtoMapper;
    private final S3ClientFileUpload s3ClientFileUpload;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName; // S3 버킷 이름 가져오기

    // 간편하게 멤버 객체를 찾기 위한 findByMemberEmail
    public Member findByMemberEmail(String email) {
        return memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.11"));
    }

    // 회원가입 및 검증
    public void create(MemberSaveReqDto saveReqDto, MultipartFile imageSsr) {
        validateRegistration(saveReqDto);
        // 프로필 이미지 업로드 및 url로 저장 -> aws에서 이미지를 가져오는 방식
        String imageUrl = null;
        if (saveReqDto.getProfileImage() != null && !saveReqDto.getProfileImage().isEmpty()) {
            imageUrl = s3ClientFileUpload.upload(saveReqDto.getProfileImage(), bucketName);
            saveReqDto.setProfileImgUrl(imageUrl);
        }

        Member member = saveReqDto.toEntity(passwordEncoder.encode(saveReqDto.getPassword()));
//        member.setProfileImgUrl(imageUrl); ------- 지우면 울어요.
        memberRepository.save(member);
    }

    // 로그인
    public String login(MemberLoginDto loginDto) {
        Member member = memberRepository.findByMemberEmail(loginDto.getMemberEmail())
                .orElseThrow(() -> new RuntimeException("잘못된 이메일/비밀번호 입니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), member.getPassword())) {
            throw new RuntimeException("잘못된 이메일/비밀번호 입니다.");
        }

        if (member.getDeletedAt() != null){
            throw new IllegalStateException("탈퇴한 회원입니다.");
        }

        return jwtTokenprovider.createToken(member.getMemberEmail(), member.getRole().name(), member.getId());
    }

    // 회원가입 검증 로직
    private void validateRegistration(MemberSaveReqDto saveReqDto) {
        if (saveReqDto.getPassword().length() <= 7) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }

        if (memberRepository.existsByMemberEmail(saveReqDto.getMemberEmail())) {
            throw new RuntimeException("이미 사용중인 이메일 입니다.");
        }
    }
    // 비밀번호 확인 및 검증 로렢
    private void validatePassword(String newPassword, String confirmPassword, String currentPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("동일하지 않은 비밀번호 입니다.");
        }

        if (newPassword.length() <= 7) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }

        if (passwordEncoder.matches(newPassword, currentPassword)) {
            throw new RuntimeException("이전과 동일한 비밀번호로 설정할 수 없습니다.");
        }
    }

    public void updateMember(MemberUpdateReqDto editReqDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member savedMember = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다"));

        // 비밀번호 수정
        if (editReqDto.getPassword() != null && !editReqDto.getPassword().isEmpty()) {
            validatePassword(editReqDto.getPassword(), editReqDto.getConfirmPassword(), savedMember.getPassword());
//            savedMember.setPassword(passwordEncoder.encode(editReqDto.getPassword()));
            savedMember.changPass(passwordEncoder.encode(editReqDto.getPassword()));
        }

        // 프로필 이미지 수정
        if (editReqDto.getProfileImage() != null && !editReqDto.getProfileImage().isEmpty()) {
            String imageUrl = s3ClientFileUpload.upload(editReqDto.getProfileImage(), bucketName);
//            savedMember.setProfileImgUrl(imageUrl); // 새로운 URL로 업데이트
            savedMember.changeImgUrl(imageUrl);
        }

        // 이름, 전화번호, 주소 업데이트
        savedMember.updateEntity(editReqDto.getAddress(),editReqDto.getName(), editReqDto.getPhone());
//        savedMember.setName(editReqDto.getName());
//        savedMember.setPhoneNumber(editReqDto.getPhone());
//        savedMember.setAddress(editReqDto.getAddress());

        memberRepository.save(savedMember); // 수정된 회원 정보 저장
    }

    // 회원 탈퇴
    public void deleteAccount(String email) {
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자 정보입니다."+ email));
        member.deleteAccount();
        memberRepository.save(member);
    }

    // java 라이브러리 메일 서비스
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(9999 - 1000 + 1) + 1000;
        return String.valueOf(code);
    }
    // java 라이브러리 메일 서비스
    public void sendVerificationEmail(String email) {
        String code = generateVerificationCode();
        redisService.saveVerificationCode(email, code);
        emailService.sendSimpleMessage(email, "이메일 인증 코드", "인증 코드: " + code);
    }
    // java 라이브러리 메일 서비스
    public boolean verifyEmail(String email, String code) {
        if (redisService.verifyCode(email, code)) {
            return true;
        } else {
            throw new RuntimeException("인증 코드가 유효하지 않습니다.");
        }
    }

    // member list 조회
    public Page<MemberListResDto> memberList(Pageable pageable){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        System.out.println(email);
//        Member member = memberRepository.findByMemberEmail(email)
//                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
//        if (!member.getRole().toString().equals("TodakAdmin")){
//            throw new SecurityException("관리자만 접근이 가능합니다.");
//        }
        Page<Member> members = memberRepository.findAll(pageable);
        return members.map(a -> a.listFromEntity());
    }



//    유저 회원가입은 소셜만 되어있기 때문에 TodakAdmin만 따로 initialDataLoader로 선언해 주었음.
    public void adminCreate(AdminSaveDto dto){
        log.info("MemberService[adminCreate] 실행 ");
//        DtoMapper의 toMember 를 사용하여 member에 자동으로 mapping 해준다.
        Member member = dtoMapper.toMember(dto);
        memberRepository.save(member);
    }
}
