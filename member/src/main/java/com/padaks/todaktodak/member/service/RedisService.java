package com.padaks.todaktodak.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveVerificationCode(String memberEmail, String code) {
        if (memberEmail == null || code == null) {
            throw new IllegalArgumentException("Email and code must not be null");
        }
        redisTemplate.opsForValue().set(memberEmail, code, 10, TimeUnit.MINUTES);
        System.out.println("Redis에 저장된 코드: " + code);  // 인증 코드 디버깅 출력, 시간 제한
    }

    public boolean verifyCode(String memberEmail, String code) {
        if (memberEmail == null || code == null) {
            throw new IllegalArgumentException("Email and code must not be null");
        }
        Object cachedCode = redisTemplate.opsForValue().get(memberEmail);
        System.out.println("Redis에서 가져온 코드: " + cachedCode);  // 저장된 코드 디버깅 출력
        if (cachedCode != null && cachedCode.equals(code)) {
            redisTemplate.delete(memberEmail);  // 검증 후 코드 삭제
            return true;
        }
        return false;
    }

}
