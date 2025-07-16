package com.irc4spring.model;

/**
 * 用户角色枚举
 */
public enum UserRole {
    USER("用户"),
    OPERATOR("操作员"),
    ADMIN("管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasPermission(UserRole requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }
} 