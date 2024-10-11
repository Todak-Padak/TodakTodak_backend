package com.padaks.todaktodak.review.controller;

import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.review.domain.Review;
import com.padaks.todaktodak.review.dto.*;
import com.padaks.todaktodak.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/create")
    public ResponseEntity<?> createReview(@RequestBody ReviewSaveReqDto dto) {
        try {
            Review review = reviewService.createReview(dto);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "리뷰가 작성되었습니다.", review.getId()), HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.CONFLICT, e.getMessage()), HttpStatus.CONFLICT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 병원 ID 기준으로 리뷰 목록 조회
    @GetMapping("/list/{hospital_id}")
    public ResponseEntity<?> getReviews(@PathVariable("hospital_id") Long hospitalId,
                                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ReviewListResDto> reviews = reviewService.reviewListResDtos(hospitalId, pageable);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "리뷰 목록입니다.", reviews), HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.NOT_FOUND, "리뷰 또는 예약 내역을 찾을 수 없습니다: " + e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();  // 서버 콘솔에 오류 출력
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // 리뷰 수정
    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody ReviewUpdateReqDto dto) {
        try {
            reviewService.updateReview(id, dto);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "리뷰가 성공적으로 수정되었습니다.", id), HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    // 리뷰 삭제 (soft delete)
    @GetMapping("/delete/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deletedReview(id);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "리뷰가 삭제되었습니다.", id), HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    // 병원의 리뷰 통계 (평균 점수 및 평점별 개수) 조회
    @GetMapping("/detail/{hospitalId}")
    public ResponseEntity<ReviewDetailDto> getReviewStatistics(
            @PathVariable Long hospitalId,
            Pageable pageable) {
        ReviewDetailDto reviewDetailDto = reviewService.reviewDetail(hospitalId, pageable);
        return ResponseEntity.ok(reviewDetailDto);
    }

    // 사용자의 리뷰 리스트 조회
    @GetMapping("/my/list")
    public ResponseEntity<Page<ReviewMyListResDto>> getMyReviewList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewMyListResDto> reviewList = reviewService.reviewMyListResDtos(pageable);
        return ResponseEntity.ok(reviewList);
    }

    // 의사별 전체 리뷰 조회
    @GetMapping("/doctor/{doctorEmail}")
    public ResponseEntity<Page<ReviewListResDto>> getDoctorReviews(
            @PathVariable String doctorEmail,
            Pageable pageable
    ) {
        Page<ReviewListResDto> reviews = reviewService.reviewDoctorList(pageable, doctorEmail);
        return ResponseEntity.ok(reviews);
    }

    // 의사의 대면 진료 리뷰 조회 (untact = false)
    @GetMapping("/untact/false/{doctorEmail}")
    public ResponseEntity<Page<ReviewListResDto>> getDoctorFaceToFaceReviews(
            @PathVariable String doctorEmail,
            Pageable pageable
    ) {
        Page<ReviewListResDto> reviews = reviewService.reviewDoctorListUntactFalse(pageable, doctorEmail);
        return ResponseEntity.ok(reviews);
    }

    // 의사의 비대면 진료 리뷰 조회 (untact = true)
    @GetMapping("/untact/true/{doctorEmail}")
    public ResponseEntity<Page<ReviewListResDto>> getDoctorTelemedicineReviews(
            @PathVariable String doctorEmail,
            Pageable pageable
    ) {
        Page<ReviewListResDto> reviews = reviewService.reviewDoctorListUntactTrue(pageable, doctorEmail);
        return ResponseEntity.ok(reviews);
    }

    // 의사의 리뷰 통계 (평균 점수 및 평점별 개수) 조회
    @GetMapping("doctor/detail/{doctorEmail}")
    public ResponseEntity<ReviewDetailDto> getReviewDoctorDetail(
            @PathVariable String doctorEmail,
            Pageable pageable) {
        ReviewDetailDto reviewDetailDto = reviewService.reviewDoctorDetail(doctorEmail, pageable);
        return ResponseEntity.ok(reviewDetailDto);
    }

}
