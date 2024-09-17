package com.padaks.todaktodak.doctor.service;

import com.padaks.todaktodak.doctor.domain.Doctor;
import com.padaks.todaktodak.doctor.dto.DoctorListDto;
import com.padaks.todaktodak.doctor.dto.DoctorSaveDto;
import com.padaks.todaktodak.doctor.dto.DoctorUpdateDto;
import com.padaks.todaktodak.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @Transactional
    public void updateDoctor(DoctorUpdateDto dto, MultipartFile imageSsr){
        Doctor doctor = doctorRepository.findById(dto.getId()).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 의사입니다."));

        MultipartFile image = (imageSsr != null) ? imageSsr : dto.getProfileImgUrl();
        try{
            if (image!=null && !image.isEmpty()){
                String ImagePathFileName = doctor.getId() + "_" + image.getOriginalFilename();
                byte[] ImagePathByte = image.getBytes();
                //S3 Image upload 필요..
//                String s3ImagePath = uploadAwsFileService.UploadAwsFileAndReturnPath(bgImagePathFileName, ImagePathByte);
//                doctor.toUpdate(dto, s3ImagePath);
            }else {
                doctor.toUpdate(dto, doctor.getProfileImgUrl());
            }
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()){
                doctor.updatePassword(passwordEncoder.encode(dto.getPassword()));
            }
        }catch (IOException e){
            throw new RuntimeException("의사 정보 수정에 실패하였습니다.", e);
        }
        doctorRepository.save(doctor);
    }
}

//if (editReqDto.getPassword() != null && !editReqDto.getPassword().isEmpty()) {
//            validatePassword(editReqDto.getPassword(), editReqDto.getConfirmPassword(), user.getPassword());
//            savedUser.setPassword(passwordEncoder.encode(editReqDto.getPassword()));
//        }