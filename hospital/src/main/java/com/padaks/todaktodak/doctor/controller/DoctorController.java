package com.padaks.todaktodak.doctor.controller;

import com.padaks.todaktodak.common.auth.JwtTokenProvider;
import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.doctor.domain.Doctor;
import com.padaks.todaktodak.doctor.dto.DoctorListDto;
import com.padaks.todaktodak.doctor.dto.DoctorSaveDto;
import com.padaks.todaktodak.doctor.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctor")
public class DoctorController {
    private final DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService){
        this.doctorService = doctorService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> doctorCreate(@RequestBody DoctorSaveDto dto){
        try {
            Doctor doctor = doctorService.doctorCreate(dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "의사등록에 성공하였습니다.", doctor.getId());
            return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Object> doctorList(Pageable pageable){
        Page<DoctorListDto> doctorListDtos = doctorService.doctorList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "의사 목록을 조회합니다.", doctorListDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

}
