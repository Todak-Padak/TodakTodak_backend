package com.padaks.todaktodak.comment.domain;

import com.padaks.todaktodak.common.domain.BaseTimeEntity;

import com.padaks.todaktodak.communitynotification.domain.Notification;
import com.padaks.todaktodak.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Comment extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;
    @Column(nullable = false)
    private String doctorEmail;
    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToMany(mappedBy = "comment")
    @Builder.Default
    private List<Notification> communityNotificationList = new ArrayList<>();

}
