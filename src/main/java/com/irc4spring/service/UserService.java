package com.irc4spring.service;

import com.irc4spring.model.IrcUser;
import com.irc4spring.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理服务
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Value("${irc.server.max-connections:1000}")
    private int maxConnections;
    
    @Value("${irc.server.max-nickname-length:30}")
    private int maxNicknameLength;
    
    @Value("${irc.auth.session-timeout:3600000}")
    private long sessionTimeout;
    
    private final Map<String, IrcUser> usersByNickname = new ConcurrentHashMap<>();
    private final Map<Socket, IrcUser> usersBySocket = new ConcurrentHashMap<>();
    private final Map<String, IrcUser> usersByUsername = new ConcurrentHashMap<>();
    
    /**
     * 添加新用户
     */
    public boolean addUser(IrcUser user) {
        if (usersByNickname.size() >= maxConnections) {
            logger.warn("服务器已达到最大连接数: {}", maxConnections);
            return false;
        }
        
        if (user.getNickname() != null && usersByNickname.containsKey(user.getNickname())) {
            logger.warn("昵称已被使用: {}", user.getNickname());
            return false;
        }
        
        if (user.getNickname() != null) {
            usersByNickname.put(user.getNickname(), user);
        }
        
        if (user.getUsername() != null) {
            usersByUsername.put(user.getUsername(), user);
        }
        
        if (user.getSocket() != null) {
            usersBySocket.put(user.getSocket(), user);
        }
        
        logger.info("用户已添加: {}", user.getNickname());
        return true;
    }
    
    /**
     * 移除用户
     */
    public boolean removeUser(String nickname) {
        IrcUser user = usersByNickname.remove(nickname);
        if (user != null) {
            // 只有username不为null时才从usersByUsername中移除
            if (user.getUsername() != null) {
                usersByUsername.remove(user.getUsername());
            }
            usersBySocket.remove(user.getSocket());
            
            // 关闭socket连接
            if (user.getSocket() != null && !user.getSocket().isClosed()) {
                try {
                    user.getSocket().close();
                } catch (IOException e) {
                    logger.error("关闭socket连接失败", e);
                }
            }
            
            logger.info("用户已移除: {}", nickname);
            return true;
        }
        return false;
    }
    
    /**
     * 根据昵称获取用户
     */
    public IrcUser getUserByNickname(String nickname) {
        return usersByNickname.get(nickname);
    }
    
    /**
     * 根据用户名获取用户
     */
    public IrcUser getUserByUsername(String username) {
        return usersByUsername.get(username);
    }
    
    /**
     * 根据socket获取用户
     */
    public IrcUser getUserBySocket(Socket socket) {
        return usersBySocket.get(socket);
    }
    
    /**
     * 获取所有用户
     */
    public Collection<IrcUser> getAllUsers() {
        return usersByNickname.values();
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return usersByNickname.size();
    }
    
    /**
     * 检查昵称是否可用
     */
    public boolean isNicknameAvailable(String nickname) {
        return nickname != null && 
               !nickname.trim().isEmpty() && 
               nickname.length() <= maxNicknameLength &&
               !usersByNickname.containsKey(nickname) &&
               isValidNickname(nickname);
    }
    
    /**
     * 验证昵称格式
     */
    private boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        
        // 昵称不能以数字开头
        if (Character.isDigit(nickname.charAt(0))) {
            return false;
        }
        
        // 昵称只能包含字母、数字、下划线、连字符
        return nickname.matches("^[a-zA-Z][a-zA-Z0-9_-]*$");
    }
    
    /**
     * 更改用户昵称
     */
    public boolean changeNickname(String oldNickname, String newNickname) {
        if (!isNicknameAvailable(newNickname)) {
            return false;
        }
        
        IrcUser user = usersByNickname.remove(oldNickname);
        if (user != null) {
            user.setNickname(newNickname);
            usersByNickname.put(newNickname, user);
            logger.info("用户昵称已更改: {} -> {}", oldNickname, newNickname);
            return true;
        }
        
        return false;
    }
    
    /**
     * 向用户发送消息
     */
    public boolean sendMessageToUser(String nickname, String message) {
        IrcUser user = getUserByNickname(nickname);
        if (user != null && user.getSocket() != null && !user.getSocket().isClosed()) {
            try {
                PrintWriter writer = new PrintWriter(user.getSocket().getOutputStream(), true);
                writer.println(message);
                user.updateActivity();
                return true;
            } catch (IOException e) {
                logger.error("发送消息失败: {}", nickname, e);
                // 连接异常，移除用户
                removeUser(nickname);
            }
        }
        return false;
    }
    
    /**
     * 向所有用户广播消息
     */
    public void broadcastMessage(String message) {
        usersByNickname.values().forEach(user -> {
            sendMessageToUser(user.getNickname(), message);
        });
    }
    
    /**
     * 向指定角色的用户发送消息
     */
    public void sendMessageToRole(String message, UserRole role) {
        usersByNickname.values().stream()
                .filter(user -> user.hasPermission(role))
                .forEach(user -> sendMessageToUser(user.getNickname(), message));
    }
    
    /**
     * 踢出用户
     */
    public boolean kickUser(String nickname, String reason) {
        IrcUser user = getUserByNickname(nickname);
        if (user != null) {
            String kickMessage = "ERROR :You have been kicked from the server" + 
                               (reason != null ? " (" + reason + ")" : "");
            sendMessageToUser(nickname, kickMessage);
            removeUser(nickname);
            logger.info("用户已被踢出: {} (原因: {})", nickname, reason);
            return true;
        }
        return false;
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String nickname) {
        return usersByNickname.containsKey(nickname);
    }
    
    /**
     * 清理超时用户
     */
    public void cleanupTimeoutUsers() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(sessionTimeout / 1000);
        
        usersByNickname.values().removeIf(user -> {
            if (user.getLastActivity().isBefore(timeout)) {
                logger.info("清理超时用户: {}", user.getNickname());
                removeUser(user.getNickname());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalUsers", usersByNickname.size());
        stats.put("maxConnections", maxConnections);
        
        long adminCount = usersByNickname.values().stream()
                .mapToLong(user -> user.getRole() == UserRole.ADMIN ? 1 : 0)
                .sum();
        
        long operatorCount = usersByNickname.values().stream()
                .mapToLong(user -> user.getRole() == UserRole.OPERATOR ? 1 : 0)
                .sum();
        
        stats.put("adminCount", adminCount);
        stats.put("operatorCount", operatorCount);
        stats.put("regularUserCount", usersByNickname.size() - adminCount - operatorCount);
        
        return stats;
    }
    
    /**
     * 获取用户详细信息
     */
    public Map<String, Object> getUserInfo(String nickname) {
        IrcUser user = getUserByNickname(nickname);
        if (user == null) {
            return null;
        }
        
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("nickname", user.getNickname());
        info.put("username", user.getUsername());
        info.put("realname", user.getRealname());
        info.put("hostname", user.getHostname());
        info.put("role", user.getRole());
        info.put("registered", user.isRegistered());
        info.put("authenticated", user.isAuthenticated());
        info.put("connectedAt", user.getConnectedAt());
        info.put("lastActivity", user.getLastActivity());
        info.put("channels", user.getChannels());
        
        return info;
    }
} 