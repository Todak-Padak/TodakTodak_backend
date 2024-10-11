package com.padaks.todaktodak.reservation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.common.dto.DtoMapper;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.hospital.repository.HospitalRepository;
import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.dto.*;
import com.padaks.todaktodak.reservation.domain.ReservationHistory;
import com.padaks.todaktodak.reservation.domain.Status;
import com.padaks.todaktodak.reservation.realtime.RealTimeService;
import com.padaks.todaktodak.reservation.repository.ReservationHistoryRepository;
import com.padaks.todaktodak.reservation.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.padaks.todaktodak.common.exception.exceptionType.GlobalExceptionType.JSON_PARSING_ERROR;
import static com.padaks.todaktodak.common.exception.exceptionType.HospitalExceptionType.*;
import static com.padaks.todaktodak.common.exception.exceptionType.ReservationExceptionType.*;
@Service
@Slf4j
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationHistoryRepository reservationHistoryRepository;
    private final DtoMapper dtoMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> redisScheduleTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final HospitalRepository hospitalRepository;
    private final MemberFeign memberFeign;
    private final MemberFeignClient memberFeignClient;
    private final ObjectMapper objectMapper;
    private final RealTimeService realTimeService;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationHistoryRepository reservationHistoryRepository,
                              DtoMapper dtoMapper,
                              @Qualifier("1") RedisTemplate<String, Object> redisTemplate,
                              @Qualifier("2") RedisTemplate<String, Object> redisScheduleTemplate,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              HospitalRepository hospitalRepository,
                              MemberFeign memberFeign,
                              MemberFeignClient memberFeignClient, ObjectMapper objectMapper, RealTimeService realTimeService) {
        this.reservationRepository = reservationRepository;
        this.reservationHistoryRepository = reservationHistoryRepository;
        this.dtoMapper = dtoMapper;
        this.redisTemplate = redisTemplate;
        this.redisScheduleTemplate = redisScheduleTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.hospitalRepository = hospitalRepository;
        this.memberFeign = memberFeign;
        this.memberFeignClient = memberFeignClient;
        this.objectMapper = objectMapper;
        this.realTimeService = realTimeService;
    }

    // member 객체 리턴, 토큰 포함
    public MemberFeignDto getMemberInfo() {
        MemberFeignDto member = memberFeignClient.getMemberEmail();  // Feign Client에 토큰 추가
        return member;
    }

//    진료 스케줄 예약 기능
    public void scheduleReservation(ReservationSaveReqDto dto){
        log.info("ReservationService[scheduleReservation] : 스케줄 예약 요청 처리 시작");

        DoctorResDto doctorResDto = memberFeign.getDoctor(dto.getDoctorEmail());
        dto.setDoctorName(doctorResDto.getName());

        MemberFeignDto member = getMemberInfo();

        String lockKey = dto.getDoctorEmail() + ":"+ dto.getReservationDate()+ ":" + dto.getReservationTime();

        Boolean isLockState;
        if (redisScheduleTemplate != null && redisScheduleTemplate.opsForValue() != null) {
            isLockState = redisScheduleTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 2, TimeUnit.MINUTES);
        } else {
            // 로그 출력 또는 예외 처리
            throw new BaseException(REDIS_ERROR);
        }
        if(Boolean.TRUE.equals(isLockState)){
            try{
                //        진료 예약 시 해당 의사 선생님의 예약이 존재할 경우 Exception을 발생 시키기 위한 코드
                reservationRepository.findByDoctorEmailAndReservationDateAndReservationTime
                                (dto.getDoctorEmail(), dto.getReservationDate(), dto.getReservationTime())
                        .ifPresent(reservation -> {
                            throw new BaseException(RESERVATION_DUPLICATE);
                        });
                Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                        .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));
                Reservation reservation = dtoMapper.toReservation(dto, member, hospital);
                reservationRepository.save(reservation);

                Map<String, Object> messageData = createMessageData(reservation, getMemberInfo().getName());
                String notificationMessage = objectMapper.writeValueAsString(messageData);

                kafkaTemplate.send("scheduled-reservation-success-notify", notificationMessage);
            } catch (JsonProcessingException e) {
                throw new BaseException(JSON_PARSING_ERROR);
            } catch (BaseException e){
                throw new BaseException(RESERVATION_DUPLICATE);
            } finally {
                redisScheduleTemplate.delete(lockKey);
                log.info("ReservationConsumer[consumerReservation] : 락 해제 완료");
            }
        }else{
            log.info("ReservationConsumer[consumerReservation] : 락을 얻지 못함, 예약 처리 실패");
            throw new BaseException(LOCK_OCCUPANCY);
        }
    }

//    당일 예약 기능
    public void immediateReservation(ReservationSaveReqDto dto){
        log.info("ReservationService[immediateReservation] : 예약 요청 처리 시작");

        DoctorResDto doctorResDto = memberFeign.getDoctor(dto.getDoctorEmail());
        MemberFeignDto member = getMemberInfo();

        dto.setDoctorName(doctorResDto.getName());
        dto.setReservationDate(LocalDate.now());

        try {
            String key = dto.getHospitalId() + ":" + dto.getDoctorEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                Set<Object> set = redisTemplate.opsForZSet().range(key, 0, -1);
                if (set.size() > 30) {
                    throw new BaseException(TOOMANY_RESERVATION);
                }
            }
            String sequenceKey = "sequence" + dto.getHospitalId();
            Long sequence = redisTemplate.opsForValue().increment(sequenceKey, 1);

            Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                    .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));

            Reservation reservation = dtoMapper.toReservation(dto, member, hospital);
            reservationRepository.save(reservation);
            RedisDto redisDto = dtoMapper.toRedisDto(reservation);

            redisTemplate.opsForZSet().add(key, redisDto, sequence);
            realTimeService.update(reservation.getId().toString(),sequence.toString());

            Map<String, Object> messageData = createMessageData(reservation, getMemberInfo().getName());
            String notificationMessage = objectMapper.writeValueAsString(messageData);
            kafkaTemplate.send("immediate-reservation-success-notify", notificationMessage);
            log.info("KafkaListener[handleReservation] : 예약 대기열 처리 완료");
        } catch (JsonProcessingException e) {
            throw new BaseException(JSON_PARSING_ERROR);
        }
        log.info("ReservationService[immediateReservation] : 완료");
    }

//    예약 취소 기능
    public void cancelledReservation(Long id){
        log.info("ReservationService[cancelledReservation] : 시작");
//        예약의 id 로 찾고 만약 예약이 없을경우 RESERVATION_NOT_FOUND 예외를 발생 -> BaseException 에 정의
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BaseException(RESERVATION_NOT_FOUND));
//        hard delete 로 DB 상에서 완전히 지워버림
        reservationRepository.delete(reservation);
//        reservationHistory 테이블에 저장하기 위한 코드
        ReservationHistory reservationHistory = dtoMapper.toReservationHistory(reservation, 1L);
        reservationHistory.setStatus(Status.Cancelled);
//        reservationHistory 테이블에 저장.
        reservationHistoryRepository.save(reservationHistory);

        //        Redis의 예약 찾기
        String key = reservationHistory.getHospitalId() + ":" + reservationHistory.getDoctorEmail();
        RedisDto redisDto = dtoMapper.toRedisDto(reservation);
//        list 에서 해당 예약을 삭제
        redisTemplate.opsForZSet().remove(key, redisDto);
        realTimeService.delete(redisDto.getId().toString());
    }
    
//    예약 조회 기능
    public List<?> checkListReservation(CheckListReservationResDto resDto, Pageable pageable){
//        feign 으로 연결 되면 여기에 email 로 해당 user 찾는 로직이 들어갈 예정
        MemberResDto member = memberFeign.getMember(resDto.getEmail());
//        여기서 페이징 처리할 예정 -> 페이징 처리하면서 예약
//        여기서 미리 예약 , 당일 예약 분기처리도 해줄 예정
        Page<Reservation> reservationPage;
        if(resDto.getType().toString().equals("All")){
            reservationPage = reservationRepository.findByMemberEmail(pageable, resDto.getEmail());
        }
        else{
            reservationPage = reservationRepository
                    .findByMemberEmailAndReservationType(
                            pageable,
                            resDto.getEmail(),
                            dtoMapper.resTypeToReserveType(resDto.getType()));
        }


        List<CheckListReservationReqDto> dto = reservationPage.stream()
                .map(dtoMapper::toListReservation)
                .collect(Collectors.toList());

        return dto;
    }

//    스케줄 예약 노쇼 스케줄 동작 구현
    public List<String> reservationNoShowSchedule(){
        log.info("예약 노쇼 스케줄 동작");
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now().withNano(0).withSecond(0);

        List<Reservation> reservation =
                reservationRepository.findAllByReservationDateAndReservationTime(localDate,localTime);
        List<String> member = new ArrayList<>();

        for(Reservation res : reservation){
            if(!res.getStatus().equals(Status.Completed)) {
                res.updateStatus(Status.Noshow);
                member.add(res.getMemberEmail());
            }
        }
        log.info("예약 노쇼 스케줄 종료");
        return member;
    }

//    예약 30분 전에 알림 보내는 스케줄 로직
    @Scheduled(cron = "0 0,30 8-12,13-22 * * *")
    public void notifyBeforeReservation(){
//        오늘 날짜
        LocalDate targetDate = LocalDate.now();
//        현재 시간 스케줄로 인해 9시~12시, 13시~22시 0분,30분 고정
        LocalTime currentTime = LocalTime.now();
//        현재 시간으로 부터 30분 이후 ex) 09:00 -> 09:30, 10:30 -> 11:00
        LocalTime targetTime = currentTime.plusMinutes(30);
//        오늘 날짜와 현재 시간을 기준으로 예약을 찾아오는 로직 (JQPL 사용함)
        List<Reservation> reservations = reservationRepository.findReservationByAtSpecificTimeAndSpecificDate(targetTime,targetDate);

        for(Reservation res : reservations){
//            카프카로 전송해줄 메시지 생성
            Map<String, String> messageDate = new HashMap<>();
            messageDate.put("memberEmail", res.getMemberEmail());
            messageDate.put("reservationDate", res.getReservationDate().toString());
            messageDate.put("reservationTime", res.getReservationTime().toString());
            messageDate.put("hospitalName", res.getHospital().getName());
            messageDate.put("doctorName", res.getDoctorName());
            messageDate.put("message", "예약 30분 전입니다");
//            카프카로 메시지 전송.
            kafkaTemplate.send("scheduled-reservation-before-notify", messageDate);
        }
    }
    private Map<String, Object> createMessageData(Reservation reservation, String memberName) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("adminEmail", reservation.getHospital().getAdminEmail());
        messageData.put("doctorName", reservation.getDoctorName());
        messageData.put("memberName", memberName);
        messageData.put("hospitalName", reservation.getHospital().getName());
        messageData.put("reservationType", reservation.getReservationType());
        messageData.put("reservationDate", reservation.getReservationDate());
        messageData.put("medicalItem", reservation.getMedicalItem());
        messageData.put("childId", reservation.getChildId());

        if(reservation.getReservationTime() != null){
            messageData.put("reservationTime", reservation.getReservationTime());
        }
        return messageData;
    }

    public List<LocalTime> reservationTimes(DoctorTimeRequestDto dto){

        return reservationRepository.
                findScheduledReservationTimesByDoctor(dto.getDoctorEmail(), dto.getDate());
    }
}
