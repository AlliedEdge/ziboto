package com.ziboto.backend.user.entity;

public enum UserStatus {
    PENDING,    // awaiting email verification — no login allowed
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED
}
