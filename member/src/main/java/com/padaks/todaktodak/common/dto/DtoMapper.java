package com.padaks.todaktodak.common.dto;

import com.padaks.todaktodak.child.domain.Child;
import com.padaks.todaktodak.child.dto.ChildResDto;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.dto.AdminSaveDto;
import com.padaks.todaktodak.member.dto.DoctorResDto;
import com.padaks.todaktodak.member.dto.MemberResDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface DtoMapper {
    DtoMapper INSTANCE = Mappers.getMapper(DtoMapper.class);

    Member toMember(AdminSaveDto adminSaveDto);

    ChildResDto toChildResDto(Child child);

    DoctorResDto toDoctorResDto(Member member);
    MemberResDto toMemberResDto(Member member);
}
