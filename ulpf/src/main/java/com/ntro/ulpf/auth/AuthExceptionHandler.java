package com.ntro.ulpf.auth;

import java.util.UUID;

public record AuthUserResponse(UUID id, String name, String email, String role) {}
