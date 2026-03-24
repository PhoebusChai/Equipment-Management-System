package com.ems.modules.review.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.review.dto.ReviewResponse;
import com.ems.modules.review.dto.ReviewUpsertRequest;
import com.ems.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<ReviewResponse> upsert(@Valid @RequestBody ReviewUpsertRequest request) {
        return ApiResponse.ok("评价提交成功", reviewService.upsert(request));
    }

    @PostMapping("/upsert")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<ReviewResponse> upsertAlias(@Valid @RequestBody ReviewUpsertRequest request) {
        return ApiResponse.ok("评价提交成功", reviewService.upsert(request));
    }

    @GetMapping
    public ApiResponse<List<ReviewResponse>> listByResource(
            @RequestParam String resourceType,
            @RequestParam Long resourceId
    ) {
        return ApiResponse.ok(reviewService.listByResource(resourceType, resourceId));
    }

    @GetMapping("/resource")
    public ApiResponse<List<ReviewResponse>> listByResourceAlias(
            @RequestParam String resourceType,
            @RequestParam Long resourceId
    ) {
        return ApiResponse.ok(reviewService.listByResource(resourceType, resourceId));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<ReviewResponse>> listTeacher(
            @RequestParam Long teacherId,
            @RequestParam(required = false) String resourceType
    ) {
        return ApiResponse.ok(reviewService.listTeacherReviews(teacherId, resourceType));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ReviewResponse>> listAdmin(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Integer rating
    ) {
        return ApiResponse.ok(reviewService.listAdminReviews(resourceType, rating));
    }

    @GetMapping("/booking/{bookingId}")
    public ApiResponse<ReviewResponse> getByBooking(@PathVariable Long bookingId) {
        return ApiResponse.ok(reviewService.getByBookingId(bookingId));
    }
}
