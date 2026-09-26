package com.ntro.ulpf.auth;

import com.ntro.ulpf.entity.UserAccount;
import com.ntro.ulpf.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InitialAdminService implements CommandLineRunner {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public InitialAdminService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword,
            @Value("${app.bootstrap-admin.name:LogFusion Admin}") String adminName
    ) {
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.adminEmail=adminEmail==null?"":adminEmail.trim();
        this.adminPassword=adminPassword==null?"":adminPassword;
        this.adminName=adminName==null?"LogFusion Admin":adminName.trim();
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) return;
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) return;

        userRepository.save(new UserAccount(
                UUID.randomUUID(),
                adminEmail.toLowerCase(),
                passwordEncoder.encode(adminPassword),
                adminName,
                "ADMIN",
                true,
                LocalDateTime.now()
        ));

        System.out.println("Bootstrap admin created: "+adminEmail);
    }
}
