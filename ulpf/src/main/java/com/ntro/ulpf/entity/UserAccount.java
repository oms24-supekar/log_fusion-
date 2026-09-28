package com.ntro.ulpf.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_users_email", columnNames="email"))
public class UserAccount {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=190) private String email;
    @Column(name="password_hash", nullable=false, length=255) private String passwordHash;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, length=40) private String role;
    @Column(nullable=false) private boolean enabled;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;

    public UserAccount() {}

    public UserAccount(UUID id, String email, String passwordHash, String name, String role, boolean enabled, LocalDateTime createdAt) {
        this.id=id; this.email=email; this.passwordHash=passwordHash; this.name=name; this.role=role; this.enabled=enabled; this.createdAt=createdAt;
    }

    public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getRole(){return role;} public void setRole(String role){this.role=role;}
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
