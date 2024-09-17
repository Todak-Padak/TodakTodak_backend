package com.padaks.todaktodak.doctor.repository;

import com.padaks.todaktodak.doctor.domain.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
//    Page<Doctor> fildAll(Pageable pageable);
    Optional<Doctor> findByDoctorEmail(String doctorEmail);
}
