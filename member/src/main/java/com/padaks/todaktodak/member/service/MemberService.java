package com.padaks.todaktodak.member.service;

import com.padaks.todaktodak.common.dto.*;
import com.padaks.todaktodak.common.enumdir.DayOfHoliday;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.common.feign.ReservationFeignClient;
import com.padaks.todaktodak.common.config.JwtTokenProvider;
import com.padaks.todaktodak.doctoroperatinghours.dto.DoctorOperatingHoursResDto;
import com.padaks.todaktodak.doctoroperatinghours.dto.DoctorOperatingHoursSimpleResDto;
import com.padaks.todaktodak.doctoroperatinghours.repository.DoctorOperatingHoursRepository;
import com.padaks.todaktodak.doctoroperatinghours.service.DoctorOperatingHoursService;
import com.padaks.todaktodak.member.domain.Address;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.domain.Role;
import com.padaks.todaktodak.member.dto.*;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.common.util.S3ClientFileUpload;
import com.padaks.todaktodak.notification.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.padaks.todaktodak.common.exception.exceptionType.MemberExceptionType.MEMBER_NOT_FOUND;

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
    private final ReservationFeignClient reservationFeignClient;
    private final DoctorOperatingHoursService doctorOperatingHoursService;
    private final DoctorOperatingHoursRepository doctorOperatingHoursRepository;

    // 간편하게 멤버 객체를 찾기 위한 findByMemberEmail
    public Member findByMemberEmail(String email) {
        System.out.println("이메일을 검증하는 부분:" + email);
        return memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.11"));
    }

    public MemberDetailResDto findByMemberId(Long memberId){
        Member member = memberRepository.findByIdOrThrow(memberId);
        return MemberDetailResDto.fromEntity(member);
    }

    // 회원가입 및 검증
    public void create(MemberSaveReqDto saveReqDto, MultipartFile imageSsr) {
        validateRegistration(saveReqDto);

        // 프로필 이미지 업로드 및 url로 저장 -> aws에서 이미지를 가져오는 방식
        String imageUrl = null;
        if (saveReqDto.getProfileImage() != null && !saveReqDto.getProfileImage().isEmpty()) {
            imageUrl = s3ClientFileUpload.upload(saveReqDto.getProfileImage());
            saveReqDto.setProfileImgUrl(imageUrl);
        }


        Member member = saveReqDto.toEntity(passwordEncoder.encode(saveReqDto.getPassword()));
        memberRepository.save(member);

        // 알림을 보내는 사람? -> 댓글을 단 사람 댓글을 작성한 사람의 email과 게시글 주인의 email || id
        // 커뮤니티에서 알림을 보내기 위해 필요한 내용은?
        // FCM 보내는 로직
        // 회원가입시 관리자에게 알림 전송
        String memberEmail = "todak@test.com";
        Member member1 = memberRepository.findByMemberEmail(memberEmail)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 관리자"));
        //fcm message 전송 (수신받을 멤버 id, "메세지 title", 메세지"body")
//        fcmService.sendTestMessage(member1.getId(), "회원가입", "테스트 알림");
//        fcmService.sendMessage(member1.getId(), "회원가입", saveReqDto.getMemberEmail()+"회원이 가입하였습니다", Type.REGISTER);
    }


    public Member createDoctor(DoctorSaveReqDto dto){
        validateRegistration(dto);
        MultipartFile image = dto.getProfileImage();
        String imageUrl = s3ClientFileUpload.upload(image);
        Member doctor = dto.toEntity(passwordEncoder.encode(dto.getPassword()), imageUrl);
        return memberRepository.save(doctor);
    }

    // 병원 admin 회원가입
    public Member registerHospitalAdmin(HospitalAdminSaveReqDto dto){
        validateRegistration(dto); // 회원가입 검증로직
        String temp = "정보를 변경해주세요";
        String bio = "안녕하세요. " + dto.getAdminName() + "의사입니다.";
        Address address = new Address(temp, temp, temp);
        Member hospitalAdmin = dto.toEntity(dto, passwordEncoder.encode(dto.getAdminPassword()), address, bio);
        Member unAcceptHospitalAdmin = memberRepository.save(hospitalAdmin);

        // 개발자 admin이 회원가입 승인전까지는 deletedAt에 시간 넣어서 아직 없는 회원으로 간주
//        unAcceptHospitalAdmin.setDeletedTimeAt(LocalDateTime.now());

        return unAcceptHospitalAdmin;
    }

    // 병원 admin 회원가입 승인
    public void acceptHospitalAdmin(String email){
        Member hospitalAdmin = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("이메일에 해당하는 병원 admin 회원이 없습니다."));

        // 개발자 admin이 회원가입 승인하면 deletedAt = null 처리 -> 존재하는 회원으로 간주
        hospitalAdmin.acceptHospitalAdmin();
    }

    // 로그인
    public String login(MemberLoginDto loginDto) {
        Member member = memberRepository.findByMemberEmail(loginDto.getMemberEmail())
                .orElseThrow(() -> new RuntimeException("잘못된 이메일/비밀번호 입니다."));

        if (!member.getRole().equals(Role.ADMIN)) {
            throw new SecurityException("토닥 관계자만 로그인이 가능합니다.");
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), member.getPassword())) {
            throw new RuntimeException("잘못된 이메일/비밀번호 입니다.");
        }

        if (member.getDeletedAt() != null){
            throw new IllegalStateException("탈퇴한 회원입니다.");
        }
        System.out.println("fcm토큰:" + loginDto.getFcmToken());

        member.setFcmToken(loginDto.getFcmToken());
        memberRepository.save(member);

        return jwtTokenprovider.createToken(member.getMemberEmail(), member.getRole().name(), member.getId());
    }

    // 대표 원장님 로그인
    public String hospitalLogin(MemberLoginDto loginDto) {
        Member member = memberRepository.findByMemberEmail(loginDto.getMemberEmail())
                .orElseThrow(() -> new RuntimeException("잘못된 이메일/비밀번호 입니다."));

        // Role이 HospitalAdmin 또는 NonUser가 아닌 경우에만 예외를 던짐
        if (!member.getRole().equals(Role.HOSPITAL) && !member.getRole().equals(Role.DOCTOR) && !member.getRole().equals(Role.NONUSER)) {
            throw new SecurityException("병원 관계자만 로그인이 가능합니다.");
        }

        if (!member.isVerified()) {
            throw new AuthenticationException("이메일 인증이 필요합니다.") {};
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), member.getPassword())) {
            throw new RuntimeException("잘못된 이메일/비밀번호 입니다.");
        }

        if (member.getDeletedAt() != null){
            throw new IllegalStateException("탈퇴한 회원입니다.");
        }
        System.out.println("fcm토큰:" + loginDto.getFcmToken());

        member.setFcmToken(loginDto.getFcmToken());
        memberRepository.save(member);
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

    private void validateRegistration(DoctorSaveReqDto saveReqDto) {
        if (saveReqDto.getPassword().length() <= 7) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }

        if (memberRepository.existsByMemberEmail(saveReqDto.getMemberEmail())) {
            throw new RuntimeException("이미 사용중인 이메일 입니다.");
        }
    }
    // 어드민이 병원 의사를 등록할때 검증 메서드
    private void validateRegistration(DoctorAdminSaveReqDto saveReqDto) {
        if (memberRepository.existsByMemberEmail(saveReqDto.getMemberEmail())) {
            throw new RuntimeException("이미 사용중인 이메일 입니다.");
        }
    }

    // 병원 admin 회원가입 검증로직
    private void validateRegistration(HospitalAdminSaveReqDto saveReqDto) {
        if (saveReqDto.getAdminPassword().length() <= 7) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }

        if (memberRepository.existsByMemberEmail(saveReqDto.getAdminEmail())) {
            throw new RuntimeException("이미 사용중인 이메일 입니다.");
        }
    }


    // 비밀번호 확인 및 검증 로
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

    public void updateDoctor(Member member, DoctorUpdateReqdto dto){
        boolean isupdated = false;
        // 비밀번호 수정
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()){
            validatePassword(dto.getPassword(), dto.getConfirmPassword(), member.getPassword());
            member.changePassword(passwordEncoder.encode(dto.getPassword()));
            System.out.println("비밀번호가 변경되었습니다.");
            isupdated = true;
        }
        // 프로필 이미지 수정
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()){
            String imageUrl = s3ClientFileUpload.upload(dto.getProfileImage());
            member.changePhoneNumber(imageUrl);
        }
        if (dto.getBio() != null){
            member.changeBio(dto.getBio());
            System.out.println("약력이 수정되었습니다 :" + dto.getBio());
            isupdated = true;
        }
        if (dto.getPhoneNumber() != null && dto.getPhoneNumber().isEmpty()){
            member.changePhoneNumber(dto.getPhoneNumber());
            System.out.println("전화번호가 수정되었습니다. :" + dto.getPhoneNumber());
            isupdated = true;
        }
        if (dto.getHospitalId() != null){
            member.changeHospitalId(dto.getHospitalId());
            System.out.println("병원id가 수정되었습니다. : " + dto.getHospitalId());
            isupdated = true;
        }

        if (isupdated) {
            memberRepository.save(member);
            System.out.println("회원 정보가 저장되었습니다.");
        } else {
            System.out.println("변경된 정보가 없습니다.");
        }

    }

    // 프로필 이미지 변경
    public void updateProfileImage(Member member, MultipartFile profileImage) throws Exception {
        // 프로필 이미지가 있는지 확인
        if (profileImage != null && !profileImage.isEmpty()) {
            // S3에 파일 업로드 및 이미지 URL 업데이트
            String imageUrl = s3ClientFileUpload.upload(profileImage);
            member.changeProfileImgUrl(imageUrl);

            // 변경된 회원 정보 저장
            memberRepository.save(member);
            System.out.println("프로필 이미지가 변경되었습니다: " + imageUrl);
        } else {
            System.out.println("프로필 이미지가 제공되지 않았습니다.");
            throw new Exception("프로필 이미지를 제공해주세요.");
        }
    }

    // 회원 정보 수정
    public void updateMember(Member member, MemberUpdateReqDto editReqDto) throws Exception {
        boolean isUpdated = false; // 업데이트 여부를 체크할 변수

        System.out.println("editReq는?");
        System.out.println(editReqDto);
        // 비밀번호 수정
        if (editReqDto.getPassword() != null && !editReqDto.getPassword().isEmpty()) {
            validatePassword(editReqDto.getPassword(), editReqDto.getConfirmPassword(), member.getPassword());
            member.changePassword(passwordEncoder.encode(editReqDto.getPassword()));
            System.out.println("비밀번호가 변경되었습니다.");
            isUpdated = true; // 업데이트 표시
        }

        // 프로필 이미지 수정
        if (editReqDto.getProfileImage() != null && !editReqDto.getProfileImage().isEmpty()) {
            String imageUrl = s3ClientFileUpload.upload(editReqDto.getProfileImage());
            member.changeProfileImgUrl(imageUrl);
        }

        // 이름, 전화번호, 주소 업데이트
        if (editReqDto.getName() != null) {
            if (editReqDto.getName().equals("이름을 입력해주세요")){
                throw new Exception("이름을 변경해주세요");
            }else {
                member.changeName(editReqDto.getName());
                System.out.println("이름이 변경되었습니다: " + member.getName());
                isUpdated = true;
                member.updateVerified();
            }

        }
        if (editReqDto.getPhoneNumber() != null) {
            System.out.println("real");
            System.out.println(editReqDto.getPhoneNumber());
            member.changePhoneNumber(editReqDto.getPhoneNumber());
            System.out.println("change");
            System.out.println(member.getPhoneNumber());
            System.out.println("전화번호가 변경되었습니다: " + member.getPhoneNumber());
            isUpdated = true;
        }

        if (editReqDto.getAddress() != null) {
            Address newAddress = new Address(editReqDto.getAddress().getCity(), editReqDto.getAddress().getStreet(), editReqDto.getAddress().getZipcode());
            member.changeAddress(newAddress);
            System.out.println("주소가 변경되었습니다: " + member.getAddress());
            isUpdated = true;
        }


        // 수정된 회원 정보 저장
        if (isUpdated) {
            memberRepository.save(member);
            System.out.println("회원 정보가 저장되었습니다.");
        } else {
            System.out.println("변경된 정보가 없습니다.");
        }
    }

    // 회원 탈퇴
    public void deleteAccount(String email) {
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자 정보입니다."+ email));
        member.deleteAccount();
        memberRepository.save(member);
    }

    //의사 삭제
    public void deleteDoctor(String email) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member hospitalAdmin = memberRepository.findByMemberEmailOrThrow(adminEmail);

        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자 정보입니다."+ email));

        if (hospitalAdmin.getHospitalId().equals(member.getHospitalId())){
            member.deleteAccount();
            memberRepository.save(member);
        }else {
            throw new IllegalArgumentException("본인 병원에 소속된 의사외에는 삭제 할 수 없습니다.");
        }

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
            Member member = findByMemberEmail(email);
            member.updateVerified();
            return true;
        } else {
            throw new RuntimeException("인증 코드가 유효하지 않습니다.");
        }
    }

    public boolean verifyCode(String email, String code) {
        if (redisService.verifyCode(email, code)) {
            return true;
        } else {
            throw new RuntimeException("인증 코드가 유효하지 않습니다.");
        }
    }
    // 멤버 전체 목록 조회 -> 멤버 필터
    public Page<MemberListResDto> memberList(boolean isVerified, boolean isDeleted, String roleString, Pageable pageable) {
        Role role = null;
        if (roleString != null && !roleString.isEmpty()) {
            role = Role.valueOf(roleString); // 문자열을 Role enum으로 변환
        }

        if (isDeleted) {
            // 탈퇴 회원 조회
            if (isVerified) {
                // 인증된 탈퇴 회원 조회 및 Role 필터링
                if (role != null) {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNotNullAndRole(true, role, pageable).map(MemberListResDto::fromEntity);
                } else {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNotNull(true, pageable).map(MemberListResDto::fromEntity);
                }
            } else {
                // 미인증 탈퇴 회원 조회 및 Role 필터링
                if (role != null) {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNotNullAndRole(false, role, pageable).map(MemberListResDto::fromEntity);
                } else {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNotNull(false, pageable).map(MemberListResDto::fromEntity);
                }
            }
        } else {
            // 정상 회원 조회
            if (isVerified) {
                // 인증된 정상 회원 조회 및 Role 필터링
                if (role != null) {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNullAndRole(true, role, pageable).map(MemberListResDto::fromEntity);
                } else {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNull(true, pageable).map(MemberListResDto::fromEntity);
                }
            } else {
                // 미인증 정상 회원 조회 및 Role 필터링
                if (role != null) {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNullAndRole(false, role, pageable).map(MemberListResDto::fromEntity);
                } else {
                    return memberRepository.findByIsVerifiedAndDeletedAtIsNull(false, pageable).map(MemberListResDto::fromEntity);
                }
            }
        }
    }



    // adminSearchMembers 메서드 수정 관리자 멤버 검색
    public Page<MemberListResDto> adminSearchMembers(String query, Pageable pageable) {
        // 이름 또는 이메일을 기준으로 검색하고, 삭제 여부나 인증 여부와 상관없이 검색 결과 반환
        return memberRepository.findByNameContainingOrMemberEmailContaining(query, query, pageable)
                .map(MemberListResDto::fromEntity);
    }

    public Page<DoctorListResDto> doctorList(Pageable pageable){
        Page<Member> doctors = memberRepository.findByRoleAndDeletedAtIsNull(Role.DOCTOR, pageable);
        return doctors.map(doctor ->{
            List<DoctorOperatingHoursSimpleResDto> operatingHours = doctorOperatingHoursService.getOperatingHoursByDoctorId(doctor.getId());
            return doctor.doctorListFromEntity(operatingHours);
        });
    }
  
    public Page<DoctorListResDto> doctorListByHospital(Long hospitalId, Pageable pageable){
        Page<Member> doctors = memberRepository.findByRoleAndHospitalId(Role.DOCTOR, hospitalId, pageable);
        return doctors.map(doctor ->{
            List<DoctorOperatingHoursSimpleResDto> operatingHours = doctorOperatingHoursService.getOperatingHoursByDoctorId(doctor.getId());
            return doctor.doctorListFromEntity(operatingHours);
        });
    }


    public Page<DoctorListResDto> doctorListByHospitalForAdmin(Pageable pageable){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member= memberRepository.findByMemberEmailOrThrow(email);
        if (member.getRole().equals(Role.HOSPITAL)){
            Page<Member> doctors = memberRepository.findByRoleAndHospitalIdAndDeletedAtIsNull(Role.DOCTOR, member.getHospitalId(), pageable);
            return doctors.map(doctor ->{
                List<DoctorOperatingHoursSimpleResDto> operatingHours = doctorOperatingHoursService.getOperatingHoursByDoctorId(doctor.getId());
                return doctor.doctorListFromEntity(operatingHours);
            });
        }else {
            throw new  IllegalArgumentException("병원 관리자 이외에는 조회가 불가능합니다.");
        }

    }

//    유저 회원가입은 소셜만 되어있기 때문에 TodakAdmin만 따로 initialDataLoader로 선언해 주었음.
    public void adminCreate(AdminSaveDto dto){
        log.info("MemberService[adminCreate] 실행 ");
//        DtoMapper의 toMember 를 사용하여 member에 자동으로 mapping 해준다.
        Member member = dtoMapper.toMember(dto);
        memberRepository.save(member);
    }

    //    reservation 에서 doctor를 찾기 위한 로직
    public Object memberDetail(String email){
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new BaseException(MEMBER_NOT_FOUND));
        if(member.getRole().equals(Role.DOCTOR)){
            return dtoMapper.toDoctorResDto(member);
        }
        else if(member.getRole().equals(Role.MEMBER)){
            return dtoMapper.toMemberResDto(member);
        }
        else{
            throw new BaseException(MEMBER_NOT_FOUND);
        }
    }
    // 어드민이 자신이 속한 병원에 의사를 등록하는 메서드
    public Member doctorAdminCreate(DoctorAdminSaveReqDto dto){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member adminMember = findByMemberEmail(email);
        if (adminMember.getRole().equals("Member")){
            throw new SecurityException("의사를 등록할 이 없습니다.");
        }
        HospitalFeignDto hospitalFeignDto = getHospital();
        String password = hospitalFeignDto.getPhoneNumber();
        password = passwordEncoder.encode(password);

        validateRegistration(dto);
        Long hosId = adminMember.getHospitalId();
        // 주소, 전화번호,
        String temp = "정보를 변경해주세요";
        Address address = new Address(temp, temp, temp);
        String bio = "안녕하세요." + dto.getName() + "의사입니다.";
        Member doctor = dto.toEntity(password, hosId, address, temp, bio);
        return memberRepository.save(doctor);
    }

    // 병원 정보 가져오는 feign
    public HospitalFeignDto getHospital(){
        HospitalFeignDto hospitalFeignDto = reservationFeignClient.getHospitalInfo();
        return hospitalFeignDto;
    }


    //의사 정보
    public DoctorInfoDto doctorInfo(String doctorEmail){
        System.out.println("의사 이메일은? : " + doctorEmail);
        Member doctor = memberRepository.findByMemberEmailOrThrow(doctorEmail);
        Long hospitalId = doctor.getHospitalId();
        System.out.println("의사의 병원 id는??? : " + hospitalId);

        HospitalInfoDto hospitalInfoDto = reservationFeignClient.getHospitalinfoById(hospitalId);
        String hospitalName = hospitalInfoDto.getName();
        System.out.println("병원 이름은? : " + hospitalName);

        CheckHospitalListReservationReqDto checkHospitalListReservationReqDto  = CheckHospitalListReservationReqDto.builder()
                .doctorEmail(doctorEmail)
                .build();
        System.out.println("의사 이메일 잘 넣음?");
        System.out.println(checkHospitalListReservationReqDto.getDoctorEmail());
//        ReservationFeignDto reservationFeignDto = reservationFeignClient.getReservation(checkHospitalListReservationReqDto);
//        System.out.println("예약리스트 가졍옴??");
//        System.out.println(reservationFeignDto.getReservationList().toString());
        System.out.println("지금부터 ㅅ작~!!");
//        System.out.println(reservationFeignDto.toString());
//        int reservationCount = reservationFeignDto.getReservationList().size();
//        System.out.println(reservationCount);

        ReviewDetailDto reviewFeignDto = reservationFeignClient.getReview(doctorEmail);
        double reviewRate = reviewFeignDto.getAverageRating();
        long totalCount = reviewFeignDto.getCount1Star() + reviewFeignDto.getCount2Stars() + reviewFeignDto.getCount3Stars() +reviewFeignDto.getCount4Stars() + reviewFeignDto.getCount5Stars();
        System.out.println(reviewRate);
        DoctorInfoDto doctorInfoDto = new DoctorInfoDto(doctorEmail, doctor.getName(), doctor.getId(), doctor.getProfileImgUrl(), hospitalName, hospitalId, totalCount,reviewRate);
        return doctorInfoDto;
    }

    // email 찾기
    public String findId(MemberFindIdDto findIdDto) {
        Member member = memberRepository.findByNameAndPhoneNumber(findIdDto.getName(), findIdDto.getPhoneNumber())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        return maskEmail(member.getMemberEmail());
    }

    // email 마스킹 처리 메서드
    private String maskEmail(String email) {
        return email.substring(0, 4) + "******" + email.substring(email.indexOf("@"));
    }

    // 비밀번호 재설정 링크 전송
    public void sendPasswordResetLink(MemberFindPasswordDto dto) {
        Member member = memberRepository.findByMemberEmail(dto.getMemberEmail())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        // memberId를 추가 인자로 전달하여 토큰 생성
        String resetToken = jwtTokenprovider.createToken(member.getMemberEmail(), member.getRole().name(), member.getId());

        // Redis에 저장
        redisService.saveVerificationCode(dto.getMemberEmail(), resetToken);

        // 이메일 전송
//        String passwordResetLink = "https://www.teenkiri.site/user/reset-password?token=" + resetToken;
//        "http:localhost8080://member-service/member/reset/password"
        // 비밀번호 재설정 링크 URL 수정
        String passwordResetLink = "http://localhost:8081/member/reset/password?token=" + resetToken;

        emailService.sendSimpleMessage(dto.getMemberEmail(), "비밀번호 재설정", "비밀번호 재설정 링크: " + passwordResetLink);
    }

    // 비밀번호 재설정
    public void resetPassword(PasswordResetDto dto) {
        String email = jwtTokenprovider.getEmailFromToken(dto.getToken());
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        validatePassword(dto.getNewPassword(), dto.getConfirmPassword(), member.getPassword());

        member.resetPassword(passwordEncoder.encode(dto.getNewPassword()));
        memberRepository.save(member);
    }
//    Noshow 카운트 증가
    @Scheduled(cron = "0 0,30 9-12,13-22 * * *")
    public void updateNoShowCount(){
        log.info("노쇼 카운트 스케줄 시작");
        String token = jwtTokenprovider.createToken("todka@test.com", Role.ADMIN.name(), 0L);
        List<String> mem = reservationFeignClient.getMember("Bearer " + token);
        for(String email : mem){
            Member member = memberRepository.findByMemberEmail(email)
                    .orElseThrow(() -> new BaseException(MEMBER_NOT_FOUND));
            member.incressNoShowCount();
            overNoShow(member);
        }
        log.info("노쇼 카운트 스케줄 완료");
    }

    private void overNoShow(Member member){
        log.info("멤버 탈퇴 처리 요청 시작");
        if(member.getNoShowCount() >= 5){
            member.deleteAccount();
        }
        log.info("멤버 탈퇴 처리 요청 완료");
    }

    @Scheduled(cron = "0 0 * 1 * *")
    public void clearNoShowCount(){
        log.info("노쇼 카운트 초기화 실행");
        List<Member> memberList = memberRepository.findAllByNoShowCountGreaterThanEqualAndDeletedAtIsNull(1);
        for(Member member : memberList){
            member.clearNoShowCount();
        }
        log.info("노쇼 카운트 초기화 완료");
    }

    // 신고 카운트 증가시키는 메서드
    public int reportCountUp(String email) {
        log.info(email);
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        return member.reportCountUp();
    }

    public int memberReportCount(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByMemberEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
        return member.getReportCount();
    }

    public List<MemberDetailResDto> searchMembers(String keyword) {
        log.info(keyword);
        List<Member> members = memberRepository.searchMembers(keyword);
        return members.stream().map(MemberDetailResDto::fromEntity).collect(Collectors.toList());
    }

    public List<DoctorUntactListDto> untactDoctorList(DayOfHoliday today, String search, String sortBy) {

        DayOfHoliday dayOfWeek = today;

        // 오늘의 untact가 true인 의사들 가져오기
        List<Member> doctors = doctorOperatingHoursRepository.findUntactMembersByDayOfWeekAndDeletedAtIsNull(dayOfWeek);
        // DoctorInfoDto 리스트 생성
        List<DoctorUntactListDto> doctorInfoDtoList = new ArrayList<>();

        // 의사 리스트 순회하며 각 의사의 병원 정보와 리뷰 정보 가져오기
        for (Member doctor : doctors) {
            Long hospitalId = doctor.getHospitalId();
            HospitalInfoDto hospitalInfoDto = reservationFeignClient.getHospitalinfoById(hospitalId);
            String hospitalName = hospitalInfoDto.getName();
            // 리뷰 정보 가져오기
            ReviewDetailDto reviewFeignDto = reservationFeignClient.getReview(doctor.getMemberEmail());
            double reviewRate = reviewFeignDto.getAverageRating();
            long totalCount = reviewFeignDto.getCount1Star() + reviewFeignDto.getCount2Stars() + reviewFeignDto.getCount3Stars() +
                    reviewFeignDto.getCount4Stars() + reviewFeignDto.getCount5Stars();

            DoctorOperatingHoursResDto operatingHoursResDto = doctorOperatingHoursService.getTodayOperatingHourByDoctorId(doctor.getId());

            // DoctorInfoDto 생성 및 리스트에 추가
            doctorInfoDtoList.add(new DoctorUntactListDto().fromEntity(doctor,hospitalName,totalCount,reviewRate,operatingHoursResDto));
        }
        // 검색 조건이 있을 경우 필터링
        if (search != null && !search.isEmpty() && !search.equals("")) {
            doctorInfoDtoList = doctorInfoDtoList.stream()
                    .filter(dto -> dto.getDoctorName().contains(search) || dto.getHospitalName().contains(search))
                    .collect(Collectors.toList());
        }
        // 정렬 기준에 따른 정렬 로직
        if (sortBy != null) {
            switch (sortBy) {
                case "reviewRate":
                    doctorInfoDtoList.sort(Comparator.comparingDouble(DoctorUntactListDto::getReviewPoint).reversed());
                    break;
                case "reviewCount":
                    doctorInfoDtoList.sort(Comparator.comparingLong(DoctorUntactListDto::getReviewCount).reversed());
                    break;
                default:
                    // 기본 정렬 로직 (예: 리뷰 점수 기준으로 정렬)
                    doctorInfoDtoList.sort(Comparator.comparingDouble(DoctorUntactListDto::getReviewPoint).reversed());
                    break;
            }
        }
        // DoctorInfoDto 리스트 반환
        return doctorInfoDtoList;
    }
    public List<DoctorInfoDto> famousUntactList(DayOfHoliday today) {

        DayOfHoliday dayOfWeek = today;

        // 오늘의 untact가 true인 의사들 가져오기
        List<Member> doctors = doctorOperatingHoursRepository.findUntactMembersByDayOfWeekAndDeletedAtIsNull(dayOfWeek);
        // DoctorInfoDto 리스트 생성
        List<DoctorInfoDto> doctorInfoDtoList = new ArrayList<>();

        // 의사 리스트 순회하며 각 의사의 병원 정보와 리뷰 정보 가져오기
        for (Member doctor : doctors) {
            Long hospitalId = doctor.getHospitalId();
            HospitalInfoDto hospitalInfoDto = reservationFeignClient.getHospitalinfoById(hospitalId);
            String hospitalName = hospitalInfoDto.getName();
            // 리뷰 정보 가져오기
            ReviewDetailDto reviewFeignDto = reservationFeignClient.getReview(doctor.getMemberEmail());
            double reviewRate = reviewFeignDto.getAverageRating();
            long totalCount = reviewFeignDto.getCount1Star() + reviewFeignDto.getCount2Stars() + reviewFeignDto.getCount3Stars() +
                    reviewFeignDto.getCount4Stars() + reviewFeignDto.getCount5Stars();

            // DoctorInfoDto 생성 및 리스트에 추가
            doctorInfoDtoList.add(new DoctorInfoDto().fromEntity(doctor,hospitalName,totalCount,reviewRate));
        }

        // reviewRate 기준으로 DoctorInfoDto 리스트를 높은 순으로 정렬
        doctorInfoDtoList.sort(Comparator.comparingDouble(DoctorInfoDto::getReviewPoint).reversed());

        // 리스트의 크기를 8로 제한
        if (doctorInfoDtoList.size() > 8) {
            doctorInfoDtoList = doctorInfoDtoList.subList(0, 8);
        }
        // DoctorInfoDto 리스트 반환
        return doctorInfoDtoList;
    }
    public DoctorDetailDto doctorDetail(String doctorEmail) {
        Member doctor = memberRepository.findByMemberEmail(doctorEmail).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        HospitalInfoDto hospitalInfoDto = reservationFeignClient.getHospitalinfoById(doctor.getHospitalId());
        List<DoctorOperatingHoursSimpleResDto> operatingHours = doctorOperatingHoursService.getOperatingHoursByDoctorId(doctor.getId());
        ReviewDetailDto reviewFeignDto = reservationFeignClient.getReview(doctorEmail);
        double reviewRate = reviewFeignDto.getAverageRating();
        long totalCount = reviewFeignDto.getCount1Star() + reviewFeignDto.getCount2Stars() + reviewFeignDto.getCount3Stars() +reviewFeignDto.getCount4Stars() + reviewFeignDto.getCount5Stars();
        return new DoctorDetailDto().fromEntity(doctor,hospitalInfoDto, reviewRate, totalCount, operatingHours);
    }

    public HospitalInfoDto getHospitalName() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = findByMemberEmail(email);
        return reservationFeignClient.getHospitalinfoById(member.getHospitalId());
    }

    // 멤버 역할 업데이트 및 새로운 토큰 생성
    public String updateMemberRoleAndGenerateNewToken(Long memberId) {
        // 멤버 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        // 멤버 역할 변경 (결제 성공 후)
        member.updateRole(Role.HOSPITAL);

        memberRepository.save(member);

        // 새로운 JWT 토큰 생성
        return jwtTokenprovider.createToken(member.getMemberEmail(), member.getRole().name(), member.getId());
    }

    public List<MontlyMemberCountDto> getMonthlyMemberCount() {
        List<Role> roles = Arrays.asList(Role.MEMBER, Role.HOSPITAL, Role.DOCTOR);
        return memberRepository.findMonthlyMemberCount(roles);
    }

}