package com.padaks.todaktodak.member.repository;

import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.doctoroperatinghours.domain.DoctorOperatingHours;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByMemberEmail(String memberEmail);
    Optional<Member> findByMemberEmail(String memberEmail);
    Page<Member> findByRole(Role role, Pageable pageable);
    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    default Member findByIdOrThrow(Long id){
        return findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 member를 찾을 수 없습니다."));
    }

    // 탈퇴하지 않은 멤버 이메일찾기
    Optional<Member> findByMemberEmailAndDeletedAtIsNull(String memberEmail);

    default Member findByMemberEmailOrThrow(String memberEmail){
        return findByMemberEmailAndDeletedAtIsNull(memberEmail)
                .orElseThrow(() -> new EntityNotFoundException("email에 해당하는 회원을 찾을 수 없습니다."));
    }

    List<Member> findAllByNoShowCountGreaterThanEqualAndDeletedAtIsNull(int noShowCount);
}
