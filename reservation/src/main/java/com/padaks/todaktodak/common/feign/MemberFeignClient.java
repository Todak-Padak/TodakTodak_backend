package com.padaks.todaktodak.common.feign;

import com.padaks.todaktodak.common.config.FeignConfig;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

//k8s배포시에는 url = "http://member-service" 추가
@FeignClient(name = "member-service", url = "http://member-service",  configuration = FeignConfig.class)
public interface MemberFeignClient {

}