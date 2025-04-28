package com.padaks.todaktodak.common.feign;

import com.padaks.todaktodak.common.config.FeignConfig;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.dto.MemberFeignNameDto;
import com.padaks.todaktodak.common.dto.MemberInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

//k8s배포시에는 url = "http://member-service" 추가
@FeignClient(name = "member-service", url = "http://member-service", configuration = FeignConfig.class)
public interface MemberFeignClient {
    @GetMapping("/member/get/member")
        // member-service에 구현된 경로
    MemberFeignDto getMemberEmail();

    @GetMapping("/member/get/{memberEmail}")
    MemberFeignNameDto getMemberName(@PathVariable String memberEmail);

    @GetMapping("/member/getInfo/{email}")
    MemberInfoDto getMemberByEmail(@PathVariable String email);

    @PostMapping("/member/report/count/{memberEmail}")
    int reportCountUp(@PathVariable String memberEmail);
}