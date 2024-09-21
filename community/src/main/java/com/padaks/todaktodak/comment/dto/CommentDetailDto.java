package com.padaks.todaktodak.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDetailDto {
    private Long id;
    private String doctorEmail;
    private String content;
    private LocalDateTime createdTimeAt;
    private LocalDateTime updatedTimeAt;
    private Long PostId;
}
