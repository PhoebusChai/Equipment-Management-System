package com.ems.modules.system.init;

import com.ems.modules.common.enums.UserStatus;
import com.ems.modules.user.entity.UserEntity;
import com.ems.modules.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@ems.com";
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        UserEntity admin = new UserEntity();
        admin.setRoleCode("admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        admin.setRealName("系统管理员");
        admin.setStatus(UserStatus.ACTIVE.name());
        userRepository.save(admin);
    }
}

