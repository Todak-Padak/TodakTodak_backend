package com.padaks.todaktodak.doctor.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDetailDto {
    private Long id;
    private String doctorEmail;
    private String name;
    private String profileImgUrl;
    private String bio;
}
