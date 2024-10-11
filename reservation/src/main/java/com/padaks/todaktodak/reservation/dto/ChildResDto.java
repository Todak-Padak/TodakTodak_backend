package com.padaks.todaktodak.reservation.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChildResDto {
    private Long id;
    private String name;
    private String ssn;
}
