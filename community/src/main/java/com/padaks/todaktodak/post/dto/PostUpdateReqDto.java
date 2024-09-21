package com.padaks.todaktodak.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostUpdateReqDto {
    private String memberEmail;
    private String title;
    private String content;
    private MultipartFile postImg;
    private LocalDateTime updateTime;
}
