package com.padaks.todaktodak.doctor.controller;

import com.padaks.todaktodak.doctor.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorController {
    private final DoctorService doctorService;
    private final JwtTokenProvider
}
