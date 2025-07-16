package com.irc4spring.model;

import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IRC用户模型
 */
public class IrcUser {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    
    private final long id;
    private String nickname;
    private String username;
    private String realname;
    private String hostname;
    private String password;
    private UserRole role;
    private boolean registered;
    private boolean authenticated;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActivity;
    private Socket socket;
    private final Set<String> channels;
    
    public IrcUser() {
        this.id = ID_GENERATOR.getAndIncrement();
        this.role = UserRole.USER;
        this.registered = false;
        this.authenticated = false;
        this.connectedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.channels = ConcurrentHashMap.newKeySet();
    }
    
    public IrcUser(String nickname, String username, String realname, String hostname) {
        this();
        this.nickname = nickname;
        this.username = username;
        this.realname = realname;
        this.hostname = hostname;
    }
    
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    public void joinChannel(String channel) {
        channels.add(channel);
    }
    
    public void leaveChannel(String channel) {
        channels.remove(channel);
    }
    
    public boolean isInChannel(String channel) {
        return channels.contains(channel);
    }
    
    public String getFullMask() {
        String safeUsername = username != null ? username : "unknown";
        String safeHostname = hostname != null ? hostname : "unknown";
        return nickname + "!" + safeUsername + "@" + safeHostname;
    }
    
    public boolean hasPermission(UserRole requiredRole) {
        return role.hasPermission(requiredRole);
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRealname() {
        return realname;
    }
    
    public void setRealname(String realname) {
        this.realname = realname;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public boolean isRegistered() {
        return registered;
    }
    
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    
    public Set<String> getChannels() {
        return channels;
    }
    
    @Override
    public String toString() {
        return "IrcUser{" +
                "id=" + id +
                ", nickname='" + nickname + '\'' +
                ", username='" + username + '\'' +
                ", realname='" + realname + '\'' +
                ", hostname='" + hostname + '\'' +
                ", role=" + role +
                ", registered=" + registered +
                ", authenticated=" + authenticated +
                ", channels=" + channels +
                '}';
    }
} 