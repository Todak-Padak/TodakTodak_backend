package com.padaks.todaktodak.hospital.service;

import com.padaks.todaktodak.common.config.FeignConfig;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.hospital.dto.HospitalAdminSaveReqDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// eureka에 등록된 name
@FeignClient(name="member-service", configuration = FeignConfig.class)
public interface MemberFeign {
    @PostMapping(value = "/member/hospital-admin/register")
    CommonResDto registerHospitalAdmin(@RequestBody HospitalAdminSaveReqDto dto);

    @PutMapping("/member/hospital-admin/accept")
    void acceptHospitalAdmin(@RequestBody String email);

}
