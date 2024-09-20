package com.padaks.todaktodak.member.domain;

import com.padaks.todaktodak.common.domain.BaseTimeEntity;
import com.padaks.todaktodak.member.dto.MemberListResDto;
import lombok.*;
import com.padaks.todaktodak.childparentsrelationship.domain.ChildParentsRelationship;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;
//    @Column
//    private Long familyId;
    private String name;
    @Column(nullable = false, unique = true)
    private String memberEmail;
    //    @Column(nullable = false)
    private String password;
    @Column
    private String profileImgUrl;
    //    @Column(nullable = false, unique = true)
    private String phoneNumber;
    //    @Column(nullable = false)
    private String ssn;
    //    @Column(nullable = false)
    @Column(length = 255)
    private Address address;
    @ColumnDefault("0")
    private int noShowCount;
    @Column
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ChildParentsRelationship> childParentsRelationshipList;

    @Builder
    public Member(String name, String email, Role role) {
        this.name = name;
        this.memberEmail = email;
        this.role = role;
    }

    public Member update(String name) {
        this.name = name;
        return this;
    }

    public Member updatePass(String uuid) {
        this.password = uuid;
        return this;
    }

    public Member updateAddress(String temp) {
        this.address = new Address(temp, temp, temp);
        return this;
    }
    public Member updateName(String name) {
        this.name = name;
        return this;
    }

    // member update 사용
    public Member changPass(String password){
        this.password = password;
        return this;
    }
    // member update 사용
    public Member changeImgUrl(String imageUrl){
        this.profileImgUrl = imageUrl;
        return this;
    }
    public Member updateEntity(Address address, String name, String phone){
        return Member.builder()
                .address(address)
                .name(name)
                .phoneNumber(phone)
                .build();
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    // 탈퇴 처리 메서드
    public void deleteAccount() {
        this.setDeletedTimeAt(LocalDateTime.now());  // 현재 시간을 삭제 시간으로 설정
    }

    // 유저 목록 조히
    public MemberListResDto listFromEntity(){
        return MemberListResDto.builder()
                .id(this.id)
                .name(this.name)
                .phone(this.phoneNumber)
                .address(this.address)
                .memberEmail(this.memberEmail)
                .role(this.role)
                .build();
    }
}
