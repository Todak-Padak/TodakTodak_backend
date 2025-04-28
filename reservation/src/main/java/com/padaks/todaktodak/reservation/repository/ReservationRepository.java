package com.padaks.todaktodak.reservation.repository;

import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.domain.ReserveType;
import com.padaks.todaktodak.reservation.domain.Status;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByMemberEmail(Pageable pageable, String email);
    Page<Reservation> findByMemberEmailAndReservationType(Pageable pageable, String email, ReserveType reserveType);
    Page<Reservation> findByDoctorEmail(Pageable pageable, String email);
    Page<Reservation> findByStatus(Pageable pageable, Status status);

    Optional<Reservation> findByMemberEmailAndReservationType(String email, ReserveType reserveType);
    Optional<Reservation> findByDoctorEmailAndReservationDateAndReservationTime
            (String doctorEmail, LocalDate reservationDate, LocalTime reservationTime);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByIdAndStatus(Long id, Status status);

    // 병원 ID로 예약 목록을 페이징 처리하여 조회
    List<Reservation> findAllByHospitalId(Long hospitalId);


    List<Reservation> findAllByReservationDateAndReservationTime(LocalDate localDate, LocalTime localTime);

//    JQPL 을 이용한 당일의 해당 시간 스케줄예약 찾는 로직
    @Query("SELECT res FROM Reservation res WHERE res.reservationTime = :targetTime AND res.reservationDate = :targetDate")
    List<Reservation> findReservationByAtSpecificTimeAndSpecificDate(@Param("targetTime") LocalTime targetTime,
                                                      @Param("targetDate") LocalDate targetDate);

    @Query("SELECT r.reservationTime FROM Reservation r " +
            "WHERE r.doctorEmail = :doctorEmail " +
            "AND r.reservationDate = :date " +
            "AND r.reservationType = 'Scheduled'")
    List<LocalTime> findScheduledReservationTimesByDoctor(@Param("doctorEmail") String doctorEmail,
                                                                @Param("reservationDate") LocalDate date);

//    오늘 이후의 예약 중 status 가 아직 예약중인 상태인 예약리스트
    List<Reservation> findByMemberEmailAndReservationDateGreaterThanEqualAndStatus(String memberEmail, LocalDate localDate, Status status);

    List<Reservation> findByMemberEmailAndReservationDateBeforeOrStatusIsNot(String memberEmail, LocalDate localDate, Status status);

    List<Reservation> findByChildId(Long id);

    Page<Reservation> findByDoctorEmailAndReservationDateAndStatus(String doctorEmail, LocalDate reservationDate, Status status, Pageable pageable);


    // 해당 날짜의 의사 예약을 모두 가져와
    List<Reservation> findByDoctorEmailAndReservationDateAndUntact(String doctorEmail, LocalDate reservationDate, boolean untact);

    List<Reservation> findByHospitalAndReservationTypeAndReservationDateAndStatus(Hospital hospital, ReserveType reserveType, LocalDate localDate, Status status);

    List<Reservation> findByHospitalAndReservationDateAndStatus(Hospital hospital, LocalDate localDate, Status status);


    Reservation findByReservationDateAndReservationTypeAndStatusAndHospitalAndChildId(LocalDate localDate, ReserveType res,Status status, Hospital hospital, Long childId);

    Reservation findByReservationDateAndReservationTypeAndHospitalAndChildId(LocalDate now, ReserveType res, Hospital hospital, Long id);
}
