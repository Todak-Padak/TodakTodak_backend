package com.padaks.todaktodak.payment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReqDto {

    private Long id;
    private String memberEmail;   // 구매자 이메일
    private String buyerName;     // 구매자 이름
    private String buyerTel;      // 구매자 전화번호
    private String merchantUid;
    private String impUid;
    private int amount;
    private String paymentStatus;  // 문자열로 상태 전달 해야함
    private String paymentMethod;
    private LocalDateTime requestTimeStamp;
    private LocalDateTime approvalTimeStamp;
    private String responseDetails;
}