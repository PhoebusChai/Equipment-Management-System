package com.ems.modules.booking.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.booking.dto.*;
import com.ems.modules.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/conflict-check")
    public ApiResponse<BookingConflictCheckResponse> conflictCheck(@Valid @RequestBody BookingConflictCheckRequest request) {
        return ApiResponse.ok(bookingService.checkConflict(request));
    }

    @PostMapping
    public ApiResponse<BookingResponse> create(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.ok("创建成功", bookingService.createBooking(request));
    }

    @GetMapping
    public ApiResponse<List<BookingResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long approverId
    ) {
        return ApiResponse.ok(bookingService.listBookings(userId, approverId));
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<BookingResponse> approve(@PathVariable Long id, @Valid @RequestBody BookingAuditRequest request) {
        return ApiResponse.ok("审批通过", bookingService.approve(id, request));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<BookingResponse> reject(@PathVariable Long id, @Valid @RequestBody BookingAuditRequest request) {
        return ApiResponse.ok("已驳回", bookingService.reject(id, request));
    }

    @PatchMapping("/{id}/finish")
    public ApiResponse<BookingResponse> finish(@PathVariable Long id, @Valid @RequestBody BookingAuditRequest request) {
        return ApiResponse.ok("已完成", bookingService.finish(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long id, @Valid @RequestBody BookingCancelRequest request) {
        return ApiResponse.ok("已取消", bookingService.cancel(id, request));
    }
}

