package com.padaks.todaktodak.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.common.dto.HospitalNameFeignDto;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.common.exception.exceptionType.HospitalExceptionType;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.hospital.adminDto.AdminHospitalListDetailResDto;
import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.hospital.dto.*;
import com.padaks.todaktodak.hospital.repository.HospitalRepository;
import com.padaks.todaktodak.review.repository.ReviewRepository;
import com.padaks.todaktodak.common.util.DistanceCalculator;
import com.padaks.todaktodak.common.util.S3ClientFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final S3ClientFileUpload s3ClientFileUpload;
    private final DistanceCalculator distanceCalculator;
    private final MemberFeign memberFeign;
    private final MemberFeignClient memberFeignClient;
    private final ReviewRepository reviewRepository;

    public MemberFeignDto getMemberInfo() {
        MemberFeignDto member = memberFeignClient.getMemberEmail();  // Feign Client에 토큰 추가
        return member;
    }

    // 병원등록
    public Hospital registerHospital(HospitalRegisterReqDto dto){

        MultipartFile hospitalImage = dto.getHospitalImage();

        // S3에 파일 업로드
        String imageUrl = s3ClientFileUpload.upload(hospitalImage); // S3Client를 통해 S3에 이미지 업로드

        // Hospital 엔티티 생성 및 저장
        Hospital hospital = dto.toEntity(dto, imageUrl);

        return hospitalRepository.save(hospital); // Hospital 저장
    }

    // 병원등록 + 병원 admin 등록(Member에게 feign 요청)
    // 회원가입 승인요청 -> 병원데이터 등록, 승인여부 false (미승인)
    public HospitalAndAdminRegisterResDto registerHospitalAndAdmin(HospitalAndAdminRegisterReqDto dto){

        Hospital hospital = hospitalRepository.save(dto.toEntity(dto)); // 병원 + 병원admin DTO -> 병원 엔티티로 조립
        HospitalAdminSaveReqDto adminSaveReqDto = HospitalAdminSaveReqDto.fromDto(dto, hospital.getId()); // 병원 admin(Member) 등록 request DTO

        CommonResDto commonResDto = memberFeign.registerHospitalAdmin(adminSaveReqDto);
        ObjectMapper objectMapper = new ObjectMapper();
        Long hospitalAdminId = objectMapper.convertValue(commonResDto.getResult(), Long.class);

        System.out.println("feign : 병원admin 회원 등록성공 회원id " + hospitalAdminId);

        return HospitalAndAdminRegisterResDto.fromEntity(hospital, hospitalAdminId);
    }

    // 병원 승인
    public void acceptHospital(Long id){
        Hospital hospital = hospitalRepository.findByIdDeletedAtIsNullOrThrow(id);
        hospital.acceptHospital(); // isAccept = true (승인처리)
        memberFeign.acceptHospitalAdmin(hospital.getAdminEmail()); // 해당병원 admin deletedAt = null 처리 (존재하는 회원 처리)
    }

    // 병원 detail 조회
    public HospitalDetailResDto getHospitalDetail(Long id, BigDecimal latitude, BigDecimal longitude) {
        Hospital hospital = hospitalRepository.findByIdOrThrow(id);

        // 병원과 사용자 간의 직선 거리 계산
        String distance = distanceCalculator.calculateDistance(
                hospital.getLatitude(), hospital.getLongitude(), latitude, longitude);

        // TODO : 이후 레디스에서 실시간 대기자수 값 가져오기
        Long standby = 0L; // 병원 실시간 대기자 수

        // 병원별 리뷰 평균평점과 리뷰 개수 조회 (null 체크 후 0으로 세팅)
        Double averageRating = Optional.ofNullable(reviewRepository.findAverageRatingByHospitalId(hospital.getId())).orElse(0.0);
        Long reviewCount = Optional.ofNullable(reviewRepository.countByHospitalId(hospital.getId())).orElse(0L);

        return HospitalDetailResDto.fromEntity(hospital, standby, distance, averageRating, reviewCount);
    }

    // 병원 수정
    public HospitalUpdateResDto updateHospital(HospitalUpdateReqDto dto){
        String imageUrl = null;
        Hospital hospital = hospitalRepository.findByIdOrThrow(dto.getId());

        if(dto.getHospitalImage() != null && !dto.getHospitalImage().isEmpty()){
            imageUrl = s3ClientFileUpload.upload(dto.getHospitalImage()); // S3에 이미지 업로드
            hospital.updateHospitalImageUrl(imageUrl);
        }

        hospital.updateHospitalInfo(dto);

        Hospital updatedHospital = hospitalRepository.save(hospital);
        return HospitalUpdateResDto.fromEntity(updatedHospital);
    }

    // 병원 삭제
    public void deleteHospital(Long id){
        Hospital hospital = hospitalRepository.findById(id)
                            .orElseThrow(()-> new BaseException(HospitalExceptionType.HOSPITAL_NOT_FOUND));

        // 이미 삭제된 병원 일경우
        if(hospital.getDeletedAt() != null){
            throw new BaseException(HospitalExceptionType.ALREADY_DELETED_HOSPITAL);
        }
        // deleteAt에 현재 시각으로 update 해줌 (삭제)
        hospital.updateDeleteAt();
    }

    // 병원 리스트 조회 ('~~동' 주소기준)
    // TODO - 정렬조건 : 거리가까운 순, 대기자 적은 순
    public List<HospitalListResDto> getHospitalList(String dong,
                                                    BigDecimal latitude,
                                                    BigDecimal longitude){

        List<Hospital> hospitalList = hospitalRepository.findByDongAndDeletedAtIsNullAndIsAcceptIsTrue(dong);
        List<HospitalListResDto> dtoList = new ArrayList<>();
        String distance = null;
        Long standby = null; // TODO : 실시간 대기자 수 이후 redis 붙일예정
        Double averageRating = null;
        Long reviewCount = null;

        for(Hospital hospital : hospitalList){
            // 병원과 사용자 간의 직선 거리 계산
            distance = distanceCalculator.calculateDistance(hospital.getLatitude(), hospital.getLongitude(), latitude, longitude);

            // 병원별 리뷰 평균평점과 리뷰 개수 조회 (null 체크 후 0으로 세팅)
            averageRating = Optional.ofNullable(reviewRepository.findAverageRatingByHospitalId(hospital.getId())).orElse(0.0);
            reviewCount = Optional.ofNullable(reviewRepository.countByHospitalId(hospital.getId())).orElse(0L);

            HospitalListResDto dto = HospitalListResDto.fromEntity(hospital, standby, distance, averageRating, reviewCount);
            dtoList.add(dto);
        }
        return dtoList;
    }

    public HospitalNameFeignDto getHospitalName(Long id){
        Hospital hospital = hospitalRepository.findByIdOrThrow(id);
        return HospitalNameFeignDto.builder()
                .hospitalName(hospital.getName())
                .build();
    }

    // 병원 리스트 조회 ('~~동' 주소기준) // 인기순 size=8
    // TODO - 정렬조건 : 거리가까운 순, 대기자 적은 순
    public List<HospitalListResDto> getFamousHospitalList(String dong,
                                                    BigDecimal latitude,
                                                    BigDecimal longitude){

        List<Hospital> hospitalList = hospitalRepository.findByDongAndDeletedAtIsNullAndIsAcceptIsTrue(dong);
        List<HospitalListResDto> dtoList = new ArrayList<>();
        String distance = null;
        Long standby = null; // TODO : 실시간 대기자 수 이후 redis 붙일예정
        Double averageRating = null;
        Long reviewCount = null;

        for(Hospital hospital : hospitalList){
            // 병원과 사용자 간의 직선 거리 계산
            distance = distanceCalculator.calculateDistance(hospital.getLatitude(), hospital.getLongitude(), latitude, longitude);

            // 병원별 리뷰 평균평점과 리뷰 개수 조회 (null 체크 후 0으로 세팅)
            averageRating = Optional.ofNullable(reviewRepository.findAverageRatingByHospitalId(hospital.getId())).orElse(0.0);
            reviewCount = Optional.ofNullable(reviewRepository.countByHospitalId(hospital.getId())).orElse(0L);

            HospitalListResDto dto = HospitalListResDto.fromEntity(hospital, standby, distance, averageRating, reviewCount);
            dtoList.add(dto);
        }
        // 별점 순으로 정렬
        dtoList.sort(Comparator.comparingDouble(HospitalListResDto::getAverageRating).reversed());

        // 리스트의 크기를 6으로 제한
        if (dtoList.size() > 6) {
            dtoList = dtoList.subList(0, 6);
        }

        return dtoList;
    }

    // admin hospital list
    public Page<AdminHospitalListDetailResDto> adminHospitalListResDtos(Boolean isAccept, Pageable pageable) {

        if (isAccept == null) {
            return hospitalRepository.findAll(pageable)
                    .map(AdminHospitalListDetailResDto::listFromEntity);
        }

        // isAccept 값에 따른 필터링
        if (isAccept) {
            return hospitalRepository.findByIsAccept(true, pageable)
                    .map(AdminHospitalListDetailResDto::listFromEntity);
        } else {
            return hospitalRepository.findByIsAccept(false, pageable)
                    .map(AdminHospitalListDetailResDto::listFromEntity);
        }
    }

    // 관리자 멤버 검색
    public Page<AdminHospitalListDetailResDto> adminSearchHospital(String query, Pageable pageable) {
//        if (query == null || query.isEmpty()) {
//            // 전체 병원 목록 반환
//            return hospitalRepository.findAll(pageable)
//                    .map(AdminHospitalListDetailResDto::fromEntity);
//        }
        System.out.println(query);
        System.out.println(hospitalRepository.findByRepresentativeNameContainingOrNameContainingOrAdminEmailContaining(
                        query, query, query, pageable)
                .map(AdminHospitalListDetailResDto::listFromEntity));
        // 검색어가 있는 경우 검색 처리
        return hospitalRepository.findByRepresentativeNameContainingOrNameContainingOrAdminEmailContaining(
                        query, query, query, pageable)
                .map(AdminHospitalListDetailResDto::listFromEntity);
    }

    // admin hospital detail
    public AdminHospitalListDetailResDto adminHospitalDetailResDto(Long id){
        System.out.println();
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 병원입니다."));
//        Hospital hospital = hospitalRepository.findByIdOrThrow(id);
        // 조회된 Hospital 엔티티를 AdminHospitalListDetailResDto로 변환
        return AdminHospitalListDetailResDto.detailFromEntity(hospital);
    }
}
