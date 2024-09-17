package com.padaks.todaktodak.doctor.dto;

import com.padaks.todaktodak.doctor.domain.Doctor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSaveDto {

    @NotEmpty(message = "name is essential")
    private String name;

    @NotEmpty(message = "email is essential")
    private String doctorEmail;

    @NotEmpty(message = "password is essential")
    private String password;

    private String profileImgUrl;

    private String bio;

    //? 출근, 퇴근시간 어떻게 받지..

    @Builder
    public Doctor toEntity(String password){
        return Doctor.builder()
                .password(password)
                .name(this.name)
                .doctorEmail(this.doctorEmail)
                .profileImgUrl(this.profileImgUrl)
                .bio(this.bio)
                .build();
    }
}
