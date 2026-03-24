package com.ems.modules.auth.service;

import com.ems.modules.auth.dto.LoginRequest;
import com.ems.modules.auth.dto.LoginResponse;
import com.ems.modules.auth.dto.RegisterRequest;
import com.ems.modules.auth.dto.ResetPasswordRequest;
import com.ems.config.security.JwtService;
import com.ems.modules.common.enums.UserStatus;
import com.ems.modules.user.entity.UserEntity;
import com.ems.modules.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 当前为首批接口：密码按明文比对（用于快速联调）
     * 后续建议替换为 BCrypt + JWT
     */
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE.name())
                .orElseThrow(() -> new IllegalArgumentException("账号不存在或已被锁定"));
        boolean ok;
        try {
            ok = passwordEncoder.matches(request.password(), user.getPasswordHash());
        } catch (Exception ex) {
            // 兼容历史明文密码（例如早期初始化的 admin），避免 BCrypt 解析异常导致 500
            ok = user.getPasswordHash() != null && user.getPasswordHash().equals(request.password());
            if (ok) {
                user.setPasswordHash(passwordEncoder.encode(request.password()));
                userRepository.save(user);
            }
        }
        if (!ok) throw new IllegalArgumentException("密码错误");
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRoleCode(),
                jwtService.issueToken(user.getId(), user.getRoleCode(), user.getEmail())
        );
    }

    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("邮箱已被注册");
        }
        String role = request.roleCode().trim().toUpperCase();
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            throw new IllegalArgumentException("roleCode 仅支持 STUDENT/TEACHER");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setRoleCode(role);
        user.setStatus(UserStatus.ACTIVE.name());
        UserEntity saved = userRepository.save(user);

        return new LoginResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRoleCode(),
                jwtService.issueToken(saved.getId(), saved.getRoleCode(), saved.getEmail())
        );
    }

    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}

