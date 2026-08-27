package com.jdclone.mall.controller;

import com.jdclone.mall.common.ApiResponse;
import com.jdclone.mall.dto.ReviewRequest;
import com.jdclone.mall.dto.ReviewTask;
import com.jdclone.mall.entity.Review;
import com.jdclone.mall.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ReviewTask>> tasks() {
        return ApiResponse.ok(reviewService.myReviewTasks());
    }

    @PostMapping
    public ApiResponse<Review> create(@Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.create(request));
    }
}
