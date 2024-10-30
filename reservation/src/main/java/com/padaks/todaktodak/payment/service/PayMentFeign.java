package com.padaks.todaktodak.payment.service;


import com.padaks.todaktodak.common.dto.CommonResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hospital-service-client")
public interface PayMentFeign {

    @GetMapping(value = "/doctor/{email}")
    CommonResDto getDoctorByEmail(@PathVariable("email") String email); // @PathVariable 추가
}

