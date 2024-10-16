package com.padaks.todaktodak.member.dto;


import com.padaks.todaktodak.member.domain.Address;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberListResDto {
    private Long id;
    private String name;
    private String phone;
    private Address address;
    private String memberEmail;
    private String profileImgUrl;
    private Role role;
    private LocalDateTime deletedAt;
    private boolean isVerified;

    // 생성자
    public MemberListResDto(Long id, String name, String memberEmail, String phone, Address address, boolean isVerified, LocalDateTime deletedAt, Role role) {
        this.id = id;
        this.name = name;
        this.memberEmail = memberEmail;
        this.address = address;
        this.phone = phone;
        this.isVerified = isVerified;
        this.role = role;
        this.deletedAt = deletedAt;
    }
    // 엔티티로부터 DTO를 생성하는 메서드
    public static MemberListResDto fromEntity(Member member) {
        return new MemberListResDto(
                member.getId(),
                member.getName(),
                member.getMemberEmail(),
                member.getPhoneNumber(),
                member.getAddress(),
                member.isVerified(),
                member.getDeletedAt(),
                member.getRole()
        );
    }
}

