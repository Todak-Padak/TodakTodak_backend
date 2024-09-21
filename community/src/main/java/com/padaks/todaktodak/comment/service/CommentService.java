package com.padaks.todaktodak.comment.service;

import com.padaks.todaktodak.comment.domain.Comment;
import com.padaks.todaktodak.comment.dto.CommentDetailDto;
import com.padaks.todaktodak.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public List<CommentDetailDto> getCommentByPostId(Long postId){
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(comment -> CommentDetailDto.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .doctorEmail(comment.getDoctorEmail())
                        .createdTimeAt(comment.getCreatedTimeAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<CommentDetailDto> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(comment -> CommentDetailDto.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .doctorEmail(comment.getDoctorEmail())
                        .createdTimeAt(comment.getCreatedTimeAt())
                        .updatedTimeAt(comment.getUpdatedTimeAt())
                        .build())
                .collect(Collectors.toList());
    }
}
