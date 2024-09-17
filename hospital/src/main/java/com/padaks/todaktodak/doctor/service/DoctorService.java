package com.padaks.todaktodak.doctor.service;

import com.padaks.todaktodak.doctor.domain.Doctor;
import com.padaks.todaktodak.doctor.dto.DoctorListDto;
import com.padaks.todaktodak.doctor.dto.DoctorSaveDto;
import com.padaks.todaktodak.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional

public class DoctorService {
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository, PasswordEncoder passwordEncoder){
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Doctor doctorCreate(DoctorSaveDto dto){
        if (doctorRepository.findByDoctorEmail(dto.getDoctorEmail()).isPresent()){
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        Doctor doctor = doctorRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
        return doctor;
    }

    public Page<DoctorListDto> doctorList(Pageable pageable){
        Page<Doctor> doctors = doctorRepository.findAll(pageable);
        return doctors.map(a->a.listFromEntity());

    }
}
