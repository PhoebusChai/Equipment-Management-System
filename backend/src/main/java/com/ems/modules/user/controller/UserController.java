package com.ems.modules.user.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.user.dto.UserCreateRequest;
import com.ems.modules.user.dto.UserPasswordResetRequest;
import com.ems.modules.user.dto.UserResponse;
import com.ems.modules.user.dto.UserStatusUpdateRequest;
import com.ems.modules.user.dto.UserUpdateRequest;
import com.ems.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.ok(userService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok("创建成功", userService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok("更新成功", userService.updateStatus(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateBasic(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok("更新成功", userService.updateBasic(id, request));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> resetPassword(@PathVariable Long id, @Valid @RequestBody UserPasswordResetRequest request) {
        return ApiResponse.ok("重置成功", userService.resetPassword(id, request));
    }
}

