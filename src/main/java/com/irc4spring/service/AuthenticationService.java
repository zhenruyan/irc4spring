package com.irc4spring.service;

import com.irc4spring.model.IrcUser;
import com.irc4spring.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户认证服务
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    @Value("${irc.admin.default-username:admin}")
    private String defaultAdminUsername;
    
    @Value("${irc.admin.default-password:admin123}")
    private String defaultAdminPassword;
    
    @Value("${irc.auth.require-registration:false}")
    private boolean requireRegistration;
    
    private final Map<String, String> userCredentials = new ConcurrentHashMap<>();
    private final Map<String, UserRole> userRoles = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    public AuthenticationService() {
        // 构造函数中不调用初始化，等待@PostConstruct
    }
    
    @PostConstruct
    private void initializeDefaultAdmin() {
        try {
            String hashedPassword = hashPassword(defaultAdminPassword);
            userCredentials.put(defaultAdminUsername, hashedPassword);
            userRoles.put(defaultAdminUsername, UserRole.ADMIN);
            logger.info("默认管理员账户已初始化: {}", defaultAdminUsername);
        } catch (Exception e) {
            logger.error("初始化默认管理员账户失败", e);
        }
    }
    
    /**
     * 验证用户凭据
     */
    public boolean authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        String storedHash = userCredentials.get(username);
        if (storedHash == null) {
            return false;
        }
        
        try {
            return verifyPassword(password, storedHash);
        } catch (Exception e) {
            logger.error("密码验证失败", e);
            return false;
        }
    }
    
    /**
     * 注册新用户
     */
    public boolean registerUser(String username, String password, UserRole role) {
        if (username == null || password == null || userCredentials.containsKey(username)) {
            return false;
        }
        
        try {
            String hashedPassword = hashPassword(password);
            userCredentials.put(username, hashedPassword);
            userRoles.put(username, role != null ? role : UserRole.USER);
            logger.info("用户注册成功: {} (角色: {})", username, role);
            return true;
        } catch (Exception e) {
            logger.error("用户注册失败", e);
            return false;
        }
    }
    
    /**
     * 获取用户角色
     */
    public UserRole getUserRole(String username) {
        return userRoles.getOrDefault(username, UserRole.USER);
    }
    
    /**
     * 设置用户角色
     */
    public void setUserRole(String username, UserRole role) {
        userRoles.put(username, role);
        logger.info("用户角色已更新: {} -> {}", username, role);
    }
    
    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(String username, UserRole requiredRole) {
        UserRole userRole = getUserRole(username);
        return userRole.hasPermission(requiredRole);
    }
    
    /**
     * 验证操作员权限
     */
    public boolean authenticateOperator(String username, String password) {
        if (!authenticateUser(username, password)) {
            return false;
        }
        
        return hasPermission(username, UserRole.OPERATOR);
    }
    
    /**
     * 验证管理员权限
     */
    public boolean authenticateAdmin(String username, String password) {
        if (!authenticateUser(username, password)) {
            return false;
        }
        
        return hasPermission(username, UserRole.ADMIN);
    }
    
    /**
     * 检查是否需要注册
     */
    public boolean isRegistrationRequired() {
        return requireRegistration;
    }
    
    /**
     * 用户是否已注册
     */
    public boolean isUserRegistered(String username) {
        return userCredentials.containsKey(username);
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(String username) {
        if (defaultAdminUsername.equals(username)) {
            return false; // 不能删除默认管理员
        }
        
        boolean removed = userCredentials.remove(username) != null;
        userRoles.remove(username);
        
        if (removed) {
            logger.info("用户已删除: {}", username);
        }
        
        return removed;
    }
    
    /**
     * 更改用户密码
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticateUser(username, oldPassword)) {
            return false;
        }
        
        try {
            String hashedPassword = hashPassword(newPassword);
            userCredentials.put(username, hashedPassword);
            logger.info("用户密码已更新: {}", username);
            return true;
        } catch (Exception e) {
            logger.error("密码更新失败", e);
            return false;
        }
    }
    
    /**
     * 管理员重置用户密码
     */
    public boolean resetPassword(String username, String newPassword) {
        try {
            String hashedPassword = hashPassword(newPassword);
            userCredentials.put(username, hashedPassword);
            logger.info("管理员重置用户密码: {}", username);
            return true;
        } catch (Exception e) {
            logger.error("密码重置失败", e);
            return false;
        }
    }
    
    /**
     * 获取所有用户列表
     */
    public Map<String, UserRole> getAllUsers() {
        return new ConcurrentHashMap<>(userRoles);
    }
    
    /**
     * 哈希密码
     */
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        // 生成盐值
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        
        // 使用SHA-256哈希
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashedPassword = md.digest(password.getBytes());
        
        // 组合盐值和哈希值
        byte[] combined = new byte[salt.length + hashedPassword.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * 验证密码
     */
    private boolean verifyPassword(String password, String storedHash) throws NoSuchAlgorithmException {
        byte[] combined = Base64.getDecoder().decode(storedHash);
        
        // 提取盐值
        byte[] salt = new byte[16];
        System.arraycopy(combined, 0, salt, 0, salt.length);
        
        // 提取存储的哈希值
        byte[] storedHashBytes = new byte[combined.length - salt.length];
        System.arraycopy(combined, salt.length, storedHashBytes, 0, storedHashBytes.length);
        
        // 使用相同的盐值哈希输入密码
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashedPassword = md.digest(password.getBytes());
        
        // 比较哈希值
        return MessageDigest.isEqual(storedHashBytes, hashedPassword);
    }
} 