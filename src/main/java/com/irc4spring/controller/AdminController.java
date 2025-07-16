package com.irc4spring.controller;

import com.irc4spring.model.UserRole;
import com.irc4spring.server.IrcServer;
import com.irc4spring.service.AuthenticationService;
import com.irc4spring.service.ChannelService;
import com.irc4spring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员REST API控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private IrcServer ircServer;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * 获取服务器状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        var status = ircServer.getServerStatus();
        var userStats = userService.getUserStatistics();
        var channelStats = channelService.getChannelStatistics();
        
        Map<String, Object> response = Map.of(
            "server", Map.of(
                "running", status.running(),
                "port", status.port(),
                "onlineUsers", status.onlineUsers(),
                "maxConnections", status.maxConnections()
            ),
            "users", userStats,
            "channels", channelStats
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers() {
        var users = userService.getAllUsers().stream()
                .map(user -> Map.of(
                    "nickname", user.getNickname(),
                    "username", user.getUsername(),
                    "hostname", user.getHostname(),
                    "role", user.getRole().name(),
                    "registered", user.isRegistered(),
                    "authenticated", user.isAuthenticated(),
                    "connectedAt", user.getConnectedAt(),
                    "lastActivity", user.getLastActivity(),
                    "channels", user.getChannels()
                ))
                .toList();
        
        return ResponseEntity.ok(Map.of("users", users));
    }
    
    /**
     * 获取用户详细信息
     */
    @GetMapping("/users/{nickname}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String nickname) {
        var userInfo = userService.getUserInfo(nickname);
        if (userInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 踢出用户
     */
    @PostMapping("/users/{nickname}/kick")
    public ResponseEntity<Map<String, String>> kickUser(
            @PathVariable String nickname,
            @RequestBody Map<String, String> request) {
        
        String reason = request.getOrDefault("reason", "Kicked by admin");
        boolean success = userService.kickUser(nickname, reason);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "用户已被踢出"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在或踢出失败"));
        }
    }
    
    /**
     * 设置用户角色
     */
    @PostMapping("/users/{nickname}/role")
    public ResponseEntity<Map<String, String>> setUserRole(
            @PathVariable String nickname,
            @RequestBody Map<String, String> request) {
        
        try {
            UserRole role = UserRole.valueOf(request.get("role"));
            authService.setUserRole(nickname, role);
            
            var user = userService.getUserByNickname(nickname);
            if (user != null) {
                user.setRole(role);
            }
            
            return ResponseEntity.ok(Map.of("message", "用户角色已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的角色"));
        }
    }
    
    /**
     * 获取所有频道列表
     */
    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> getChannels() {
        var channels = channelService.getAllChannels().stream()
                .map(channel -> Map.of(
                    "name", channel.getName(),
                    "topic", channel.getTopic() != null ? channel.getTopic() : "",
                    "userCount", channel.getUsers().size(),
                    "operators", channel.getOperators(),
                    "modes", channel.getModeString(),
                    "createdAt", channel.getCreatedAt()
                ))
                .toList();
        
        return ResponseEntity.ok(Map.of("channels", channels));
    }
    
    /**
     * 获取频道详细信息
     */
    @GetMapping("/channels/{channelName}")
    public ResponseEntity<Map<String, Object>> getChannelInfo(@PathVariable String channelName) {
        var channelInfo = channelService.getChannelInfo(channelName);
        if (channelInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(channelInfo);
    }
    
    /**
     * 删除频道
     */
    @DeleteMapping("/channels/{channelName}")
    public ResponseEntity<Map<String, String>> deleteChannel(@PathVariable String channelName) {
        boolean success = channelService.deleteChannel(channelName);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "频道已删除"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "频道不存在或删除失败"));
        }
    }
    
    /**
     * 向频道发送管理员消息
     */
    @PostMapping("/channels/{channelName}/message")
    public ResponseEntity<Map<String, String>> sendChannelMessage(
            @PathVariable String channelName,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息内容不能为空"));
        }
        
        String adminMessage = ":SERVER NOTICE " + channelName + " :[管理员] " + message;
        channelService.broadcastToChannel(channelName, adminMessage, null);
        
        return ResponseEntity.ok(Map.of("message", "消息已发送"));
    }
    
    /**
     * 全服广播消息
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcastMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息内容不能为空"));
        }
        
        String broadcastMessage = ":SERVER NOTICE * :[系统广播] " + message;
        userService.broadcastMessage(broadcastMessage);
        
        return ResponseEntity.ok(Map.of("message", "广播消息已发送"));
    }
    
    /**
     * 创建用户账户
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String roleStr = request.getOrDefault("role", "USER");
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }
        
        try {
            UserRole role = UserRole.valueOf(roleStr);
            boolean success = authService.registerUser(username, password, role);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "用户创建成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "用户已存在或创建失败"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的角色"));
        }
    }
    
    /**
     * 删除用户账户
     */
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username) {
        boolean success = authService.deleteUser(username);
        
        if (success) {
            // 如果用户在线，踢出用户
            userService.kickUser(username, "账户已被删除");
            return ResponseEntity.ok(Map.of("message", "用户已删除"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在或删除失败"));
        }
    }
    
    /**
     * 重置用户密码
     */
    @PostMapping("/users/{username}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> request) {
        
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码不能为空"));
        }
        
        boolean success = authService.resetPassword(username, newPassword);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "密码重置成功"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在或密码重置失败"));
        }
    }
    
    /**
     * 获取所有注册用户
     */
    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getAllAccounts() {
        var accounts = authService.getAllUsers();
        return ResponseEntity.ok(Map.of("accounts", accounts));
    }
    
    /**
     * 优雅关闭服务器
     */
    @PostMapping("/shutdown")
    public ResponseEntity<Map<String, String>> shutdownServer() {
        // 在新线程中执行关闭操作，避免阻塞HTTP响应
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(1000); // 给HTTP响应时间返回
                ircServer.gracefulShutdown();
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return ResponseEntity.ok(Map.of("message", "服务器正在关闭..."));
    }
    
    /**
     * 获取服务器运行时信息
     */
    @GetMapping("/runtime")
    public ResponseEntity<Map<String, Object>> getRuntimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> runtimeInfo = Map.of(
            "javaVersion", System.getProperty("java.version"),
            "javaVendor", System.getProperty("java.vendor"),
            "osName", System.getProperty("os.name"),
            "osVersion", System.getProperty("os.version"),
            "osArch", System.getProperty("os.arch"),
            "availableProcessors", runtime.availableProcessors(),
            "totalMemory", runtime.totalMemory(),
            "freeMemory", runtime.freeMemory(),
            "maxMemory", runtime.maxMemory(),
            "usedMemory", runtime.totalMemory() - runtime.freeMemory()
        );
        
        return ResponseEntity.ok(runtimeInfo);
    }
} 