package com.padaks.todaktodak.medicalchart.service;


import com.padaks.todaktodak.common.dto.CommonResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "hospital-service", url = "http://member-service")
public interface MedicalChartFeign {

    @GetMapping(value = "/doctor/{email}")
    CommonResDto getDoctorByEmail(String email);
}
