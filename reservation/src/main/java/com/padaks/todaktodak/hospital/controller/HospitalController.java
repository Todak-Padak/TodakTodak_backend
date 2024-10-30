package com.padaks.todaktodak.hospital.controller;

import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.common.dto.HospitalNameFeignDto;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.hospital.adminDto.AdminHospitalListDetailResDto;
import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.hospital.dto.*;
import com.padaks.todaktodak.hospital.repository.HospitalRepository;
import com.padaks.todaktodak.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hospital")
public class HospitalController {
    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;

    // 병원생성 (병원 admin만 가능) 이후 주석해제 예정
    @PostMapping("/register")
    public ResponseEntity<Object> registerHospital(@ModelAttribute HospitalRegisterReqDto hospitalRegisterReqDto){
        Hospital hospital = hospitalService.registerHospital(hospitalRegisterReqDto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "병원등록성공", hospital.getId()), HttpStatus.CREATED);
    }

    // 병원 admin + 병원 등록 (미승인 상태)
    @PostMapping("/hospital-admin/register")
    public ResponseEntity<Object> registerHospitalAndAdmin(@RequestBody HospitalAndAdminRegisterReqDto dto){
        try{
            HospitalAndAdminRegisterResDto hospitalAndAdminRegisterResDto
                    = hospitalService.registerHospitalAndAdmin(dto);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "병원, 병원 admin 등록 성공 (미승인)", hospitalAndAdminRegisterResDto), HttpStatus.CREATED);
        }catch (IllegalArgumentException e) {
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }
    // 병원 admin + 병원 승인처리 (토닥 admin만 가능)
    // 병원 : isAccept = true, 병원 admin : deletedAt = null
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/accept/{id}")
    public ResponseEntity<Object> acceptHospital(@PathVariable Long id){
        hospitalService.acceptHospital(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원, 병원 admin 가입 승인처리 성공", null),HttpStatus.OK);
    }

    // 병원 detail 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<Object> getHospitalDetail(@PathVariable Long id,
                                                    @RequestParam BigDecimal latitude,
                                                    @RequestParam BigDecimal longitude) {
        HospitalDetailResDto hospitalDetail = hospitalService.getHospitalDetail(id, latitude, longitude);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원정보 조회성공", hospitalDetail), HttpStatus.OK);
    }

    // 병원 어드민 detail 조회
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @GetMapping("/admin/detail")
    public ResponseEntity<Object> hospitalAdminDetail() {
        // 인증된 authentication 객체에서 해당 member의 email을 추출해서 인가된 사용자만이 접근할 수 있도록
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String adminEmail = userDetails.getUsername();
        HospitalAdminDetailResDto hospitalDetail = hospitalService.hospitalAdminDetail(adminEmail);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원정보 조회성공", hospitalDetail), HttpStatus.OK);
    }

    // 병원정보 수정 (병원 admin, 토닥admin만 가능)
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    @PostMapping("/update")
    public ResponseEntity<Object> updateHospital(@ModelAttribute HospitalUpdateReqDto hospitalUpdateReqDto){
        HospitalUpdateResDto hospitalUpdateResDto = hospitalService.updateHospital(hospitalUpdateReqDto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원정보 수정성공", hospitalUpdateResDto), HttpStatus.OK);
    }

    // 병원삭제 (토닥 admin만 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Object> deleteHospital(@PathVariable Long id){
        hospitalService.deleteHospital(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 삭제성공", id), HttpStatus.OK);
    }

    // 병원리스트 조회
    @GetMapping("/list")
    public ResponseEntity<Object> getList(@RequestParam String dong,
                                          @RequestParam BigDecimal latitude,
                                          @RequestParam BigDecimal longitude){
        List<HospitalListResDto> hospitalList = hospitalService.getHospitalList(dong, latitude, longitude);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 리스트 조회성공", hospitalList), HttpStatus.OK);
    }

    // 병원 정보를 멤버로 전송
    @GetMapping("/get/hospital")
    public HospitalFeignDto getHospital(){
        MemberFeignDto memberFeignDto = hospitalService.getMemberInfo();
        Long id = memberFeignDto.getHospitalId();
        Hospital hospital = hospitalRepository.findByIdOrThrow(id);
        return new HospitalFeignDto(hospital.getId(), hospital.getName(), hospital.getPhoneNumber());
    }

    @GetMapping("/get/info/{id}")
    public HospitalInfoDto getHospitalinfo(@RequestParam Long id){
        System.out.println("reservation-service는 왔나요");
        Hospital hospital = hospitalRepository.findByIdOrThrow(id);

        HospitalInfoDto dto = HospitalInfoDto.builder()
                .address(hospital.getAddress())
                .name(hospital.getName())
                .dong(hospital.getDong())
                .profileImg(hospital.getHospitalImageUrl())
                .build();
        return dto;
    }

    // 정렬된 병원리스트 조회
    @GetMapping("/sorted/list")
    public ResponseEntity<Object> getSortedHospitalList(@RequestParam String dong,
                                          @RequestParam BigDecimal latitude,
                                          @RequestParam BigDecimal longitude,
                                          @RequestParam String sort,
                                          @RequestParam Boolean isOperating){
        List<HospitalListResDto> hospitalList = hospitalService.getSortedHospitalList(dong, latitude, longitude, sort, isOperating);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 리스트 조회성공", hospitalList), HttpStatus.OK);
    }


    @GetMapping("/get/hospitalName/{id}")
    public HospitalNameFeignDto getHospital(@PathVariable Long id){
         return hospitalService.getHospitalName(id);
    }

    // 인기병원 리스트 (메인페이지)
    @GetMapping("/good/list")
    public ResponseEntity<Object> getFamousHospitalList(@RequestParam String dong,
                                          @RequestParam BigDecimal latitude,
                                          @RequestParam BigDecimal longitude){
        List<HospitalListResDto> hospitalList = hospitalService.getFamousHospitalList(dong, latitude, longitude);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 리스트 조회성공", hospitalList), HttpStatus.OK);
    }


    // admin hospital list
    // admin이 보는 병원리스트 (토닥 admin만 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/hospital/list")
    public ResponseEntity<?> adminHospitalList(
            @RequestParam(name = "accept", required = false) String isAccept, // true/false
            Pageable pageable) {

        Boolean acceptStatus = null;
        if (isAccept != null) {
            if (isAccept.equals("true")) {
                acceptStatus = true;
            } else if (isAccept.equals("false")) {
                acceptStatus = false;
            }
        }
        System.out.println("isAccept 파라미터 값: " + acceptStatus);
        Page<AdminHospitalListDetailResDto> hospitalListDetailResDtos = hospitalService.adminHospitalListResDtos(acceptStatus, pageable);
        CommonResDto dto = new CommonResDto(HttpStatus.OK, "병원 목록을 조회합니다 조회합니다.", hospitalListDetailResDtos);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }


    // admin hospital list search (토닥admin만 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("admin/hospital/search")
    public ResponseEntity<?> searchHospitals(
            @RequestParam(name = "query", required = false) String query,
            Pageable pageable) {
        System.out.println("여기");
        System.out.println(query);
//        String trimmedQuery = query.trim();  // 검색어 앞뒤 공백 제거
        Page<AdminHospitalListDetailResDto> result = hospitalService.adminSearchHospital(query, pageable);
        CommonResDto dto = new CommonResDto(HttpStatus.OK, "병원 목록을 조회합니다 조회합니다.", result);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/hospital/detail/{id}")
    public ResponseEntity<?> adminHospitalDetail(@PathVariable Long id){
        AdminHospitalListDetailResDto adminHospitalListDetailResDto = hospitalService.adminHospitalDetailResDto(id);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원정보 조회성공", adminHospitalListDetailResDto), HttpStatus.OK);
    }

//    미승인 병원 수량 조회 (토닥admin만 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/accept")
    public ResponseEntity<?> getAcceptList(){
        Long count = hospitalService.getHospitalNoAcceptList();

        return new ResponseEntity<>(count, HttpStatus.OK);
    }

}
