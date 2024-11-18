package com.re_book.board.controller;


import com.re_book.board.dto.request.ReviewPostRequestDTO;
import com.re_book.board.dto.request.ReviewUpdateRequestDTO;
import com.re_book.board.dto.response.ReviewResponseDTO;
import com.re_book.board.service.ReviewService;
import com.re_book.common.auth.TokenUserInfo;
import com.re_book.common.dto.CommonErrorDto;
import com.re_book.common.auth.JwtTokenProvider;
import com.re_book.common.dto.CommonResDto;
import com.re_book.entity.Review;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;


    // 리뷰 작성
    @PostMapping("/{bookId}")
    public ResponseEntity<?> createReview(
            @PathVariable String bookId,
            @RequestBody @Valid ReviewPostRequestDTO dto,
            @RequestHeader("Authorization") String authorization) {


        log.info("bookId: {}", bookId);
        log.info("dto: {}", dto);

        Map<String, Object> response = new HashMap<>();

        // Authorization 헤더가 없거나, 'Bearer ' 접두어가 없는 경우 처리
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "Authorization 헤더가 없거나 잘못된 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
        }

        String token = authorization.substring(7);  // 'Bearer '를 제거하여 토큰만 추출

        TokenUserInfo userInfo = null;
        try {
            userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(token);// 토큰 유효성 검사 및 사용자 정보 추출
        } catch (ExpiredJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        }

        try {
            String memberUuid = userInfo.getId();
            Review savedReview = reviewService.register(bookId, dto, memberUuid);
            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 작성되었습니다.");
            response.put("reviewId", savedReview.getId());
            response.put("nickname", savedReview.getMember().getName());
            response.put("content", savedReview.getContent());
            response.put("rating", savedReview.getRating());

            CommonResDto resDto
                    = new CommonResDto(HttpStatus.OK, "리뷰 작성 완료", response);
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 작성 중 오류가 발생했습니다.");
            log.error("Error creating review", e);

            CommonErrorDto errorDto
                    = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);

        }
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewUpdateRequestDTO dto,
            @RequestHeader("Authorization") String authorization) {

        Map<String, Object> response = new HashMap<>();

        // Authorization 헤더가 없거나, 'Bearer ' 접두어가 없는 경우 처리
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "Authorization 헤더가 없거나 잘못된 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
        }

        String token = authorization.substring(7);  // 'Bearer '를 제거하여 토큰만 추출

        TokenUserInfo userInfo = null;
        try {
            userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(token);// 토큰 유효성 검사 및 사용자 정보 추출
        } catch (ExpiredJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        }

        try {
            String memberUuid = userInfo.getId();
            reviewService.updateReview(reviewId, dto.getContent(), memberUuid);
            Review updatedReview = reviewService.findById(reviewId);

            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 수정되었습니다.");
            response.put("reviewId", updatedReview.getId());
            response.put("nickname", updatedReview.getMember().getName());
            response.put("content", updatedReview.getContent());
            response.put("rating", updatedReview.getRating());

            CommonResDto resDto
                    = new CommonResDto(HttpStatus.OK, "리뷰 수정 완료", response);
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());

            CommonErrorDto errorDto
                    = new CommonErrorDto(HttpStatus.FORBIDDEN, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 수정 중 오류가 발생했습니다.");
            log.error("Error updating review", e);

            CommonErrorDto errorDto
                    = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable String reviewId,
            @RequestHeader("Authorization") String authorization) {
        Map<String, Object> response = new HashMap<>();

        // Authorization 헤더가 없거나, 'Bearer ' 접두어가 없는 경우 처리
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "Authorization 헤더가 없거나 잘못된 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
        }

        String token = authorization.substring(7);  // 'Bearer '를 제거하여 토큰만 추출

        TokenUserInfo userInfo = null;
        try {
            userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(token);// 토큰 유효성 검사 및 사용자 정보 추출
        } catch (ExpiredJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰 형식입니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다.");
            return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
        }

        try {
            String memberUuid = userInfo.getId();
            reviewService.deleteReview(reviewId, memberUuid);
            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 삭제되었습니다.");

            CommonResDto resDto
                    = new CommonResDto(HttpStatus.OK, "리뷰 삭제 완료", response);
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());

            CommonErrorDto errorDto
                    = new CommonErrorDto(HttpStatus.FORBIDDEN, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
            log.error("Error deleting review", e);

            CommonErrorDto errorDto
                    = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/book/{bookId}")//detail에서 인지 book에서인지 확인할것
    public ResponseEntity<?> printReviewList(
            @PathVariable String bookId,
            @PageableDefault(size = 10) Pageable pageable) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 서비스 호출로 페이징된 리뷰 리스트 가져오기
            Page<ReviewResponseDTO> reviewPage = reviewService.getReviewList(bookId, pageable);

            // 응답 데이터 구성
            response.put("success", true);
            response.put("message", "리뷰 목록을 성공적으로 조회했습니다.");
            response.put("reviews", reviewPage.getContent()); // 리뷰 데이터
            response.put("currentPage", reviewPage.getNumber()); // 현재 페이지 번호
            response.put("totalItems", reviewPage.getTotalElements()); // 전체 아이템 수
            response.put("totalPages", reviewPage.getTotalPages()); // 전체 페이지 수

            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "리뷰 목록 조회 완료", response);
            return new ResponseEntity<>(resDto, HttpStatus.OK);

        } catch (Exception e) {
            // 예외 처리
            response.put("success", false);
            response.put("message", "리뷰 목록 조회 중 오류가 발생했습니다.");
            log.error("Error retrieving review list", e);

            CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}



