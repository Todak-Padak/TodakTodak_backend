package com.padaks.todaktodak.member.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    TodakAdmin("ROLE_ADMIN", "관리자"),
    HospitalAdmin("ROLE_HOSPITAL", "병원관리자"),
    NonUser("ROLE_NONUSER", "미인증관리자"),
    Member("ROLE_MEMBER", "회원"),
    Doctor("ROLE_DOCTOR", "의사");

    private final String key;
    private final String string;
}
