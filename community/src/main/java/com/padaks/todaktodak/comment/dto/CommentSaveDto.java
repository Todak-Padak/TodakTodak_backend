package com.padaks.todaktodak.comment.dto;

import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentSaveDto {
    @NotEmpty(message = "postId is essential")
//    게시글 ID
    private Long postId;
    //    댓글작성한 의사 이메일
    @NotEmpty(message = "doctor Email is essential")
    private String doctorEmail;
//    댓글 내용
    @NotEmpty(message = "content is essential")
    private String content;

    public Comment toEntity(Post post, Comment parent, String writerEmail, String name, String profileImg){
        return Comment.builder()
                .post(post)
                .doctorEmail(writerEmail)
                .content(this.content)
                .name(name)
                .profileImg(profileImg)
                .parent(parent)
                .build();
    }
}
