package com.padaks.todaktodak.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorUpdateDto {
//    private String doctorEmail;
    private Long id;
    private String password;
    private MultipartFile profileImgUrl;
    private String bio;
}
