package com.padaks.todaktodak.event.dto;

import com.padaks.todaktodak.event.domain.Event;
import com.padaks.todaktodak.event.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventUpdateReqDto {
    private String title;

    private String content;

    private Type type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

}