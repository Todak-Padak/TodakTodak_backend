package com.padaks.todaktodak.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorListDto {
    private Long id;
    private String name;
    private String profileImgUrl;
}
