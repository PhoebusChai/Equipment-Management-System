package com.ems.modules.notice.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.notice.dto.NoticeAuditRequest;
import com.ems.modules.notice.dto.NoticeCreateRequest;
import com.ems.modules.notice.dto.NoticeResponse;
import com.ems.modules.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<NoticeResponse>> list() {
        return ApiResponse.ok(noticeService.list());
    }

    @PostMapping
    public ApiResponse<NoticeResponse> create(@Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.ok("创建成功", noticeService.create(request));
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<NoticeResponse> publish(@PathVariable Long id, @Valid @RequestBody NoticeAuditRequest request) {
        return ApiResponse.ok("已发布", noticeService.publish(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<NoticeResponse> archive(@PathVariable Long id, @Valid @RequestBody NoticeAuditRequest request) {
        return ApiResponse.ok("已归档", noticeService.archive(id, request));
    }
}

