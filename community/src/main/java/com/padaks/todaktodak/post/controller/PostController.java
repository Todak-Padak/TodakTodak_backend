package com.padaks.todaktodak.post.controller;

import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.post.dto.PostDetailDto;
import com.padaks.todaktodak.post.dto.PostListDto;
import com.padaks.todaktodak.post.dto.PostUpdateReqDto;
import com.padaks.todaktodak.post.dto.PostsaveDto;
import com.padaks.todaktodak.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("post")
public class PostController {
    private final PostService postService;

    @PostMapping("/create")
    public ResponseEntity<?> register(@RequestBody PostsaveDto dto, @RequestPart(value = "image", required = false)MultipartFile imageSsr){
        try {
            postService.create(dto, imageSsr);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post 등록 성공", null);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "post 등록 실패" + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> postList(Pageable pageable){
        Page<PostListDto> postListDtos = postService.postList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post 목록을 조회합니다.", postListDtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long id){
        try {
            PostDetailDto postDetail = postService.getPostDetail(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "Post 상세정보를 조회합니다.", postDetail);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> updatePost (@PathVariable Long id, @RequestBody PostUpdateReqDto dto){
        try{
            postService.postUpdate(id, dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post가 성공적으로 업데이트 되었습니다.", id);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id){
        try {
            postService.deletePost(id);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "post가 삭제되었습니다.",id);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }

    }


}
