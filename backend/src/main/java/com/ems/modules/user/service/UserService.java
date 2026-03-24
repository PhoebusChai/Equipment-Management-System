package com.ems.modules.user.service;

import com.ems.modules.common.enums.UserStatus;
import com.ems.modules.user.dto.UserCreateRequest;
import com.ems.modules.user.dto.UserPasswordResetRequest;
import com.ems.modules.user.dto.UserResponse;
import com.ems.modules.user.dto.UserStatusUpdateRequest;
import com.ems.modules.user.dto.UserUpdateRequest;
import com.ems.modules.user.entity.UserEntity;
import com.ems.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("邮箱已存在");
        });
        UserEntity e = new UserEntity();
        e.setEmail(request.email().trim().toLowerCase());
        e.setRealName(request.realName().trim());
        e.setRoleCode(request.role().trim().toLowerCase());
        e.setStatus(UserStatus.ACTIVE.name());
        e.setPasswordHash("123456");
        return toResponse(userRepository.save(e));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        UserEntity e = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String normalized = request.status().trim().toUpperCase();
        if (!UserStatus.ACTIVE.name().equals(normalized) && !UserStatus.LOCKED.name().equals(normalized)) {
            throw new IllegalArgumentException("状态仅支持 ACTIVE/LOCKED");
        }
        e.setStatus(normalized);
        return toResponse(userRepository.save(e));
    }

    @Transactional
    public UserResponse updateBasic(Long id, UserUpdateRequest request) {
        UserEntity e = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String role = request.role().trim().toLowerCase();
        if (!"student".equals(role) && !"teacher".equals(role) && !"admin".equals(role)) {
            throw new IllegalArgumentException("角色仅支持 student/teacher/admin");
        }
        e.setRealName(request.realName().trim());
        e.setRoleCode(role);
        return toResponse(userRepository.save(e));
    }

    @Transactional
    public UserResponse resetPassword(Long id, UserPasswordResetRequest request) {
        UserEntity e = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        e.setPasswordHash(request.newPassword());
        return toResponse(userRepository.save(e));
    }

    private UserResponse toResponse(UserEntity e) {
        return new UserResponse(
                e.getId(),
                e.getEmail(),
                e.getRealName(),
                e.getRoleCode(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }
}

