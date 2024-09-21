package com.padaks.todaktodak.common.domain;

import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public class BaseTimeEntity {

    @CreationTimestamp
    private LocalDateTime createdTimeAt;

    @CreationTimestamp
    private LocalDateTime updatedTimeAt;

    private LocalDateTime deletedTimeAt;

    public void updateDeleteAt(){
        this.deletedTimeAt = LocalDateTime.now();
    }
}
