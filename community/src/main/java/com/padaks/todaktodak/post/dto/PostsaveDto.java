package com.padaks.todaktodak.post.dto;

import com.padaks.todaktodak.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.Column;
import javax.validation.constraints.NotEmpty;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostsaveDto {

    @NotEmpty(message = "email is essential")
    private String memberEmail;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    private MultipartFile postImage;


    public Post toEntity(PostsaveDto dto,String postImgUrl, String memberEmail) {
        return Post.builder()
                .memberEmail(memberEmail)
                .title(dto.getTitle())
                .content(dto.getContent())
                .postImgUrl(postImgUrl)
                .build();
    }
}
