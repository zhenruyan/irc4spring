package com.irc4spring.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IRC频道模型
 */
public class IrcChannel {
    private final String name;
    private String topic;
    private String topicSetBy;
    private LocalDateTime topicSetAt;
    private String key; // 频道密码
    private int userLimit;
    private boolean inviteOnly;
    private boolean moderated;
    private boolean secret;
    private boolean privateChannel;
    private boolean topicLocked;
    private final Set<String> users;
    private final Set<String> operators;
    private final Set<String> banned;
    private final Set<String> invited;
    private LocalDateTime createdAt;
    
    public IrcChannel(String name) {
        this.name = name;
        this.users = ConcurrentHashMap.newKeySet();
        this.operators = ConcurrentHashMap.newKeySet();
        this.banned = ConcurrentHashMap.newKeySet();
        this.invited = ConcurrentHashMap.newKeySet();
        this.createdAt = LocalDateTime.now();
        this.userLimit = 0; // 0表示无限制
        this.inviteOnly = false;
        this.moderated = false;
        this.secret = false;
        this.privateChannel = false;
        this.topicLocked = false;
    }
    
    public void addUser(String nickname) {
        users.add(nickname);
    }
    
    public void removeUser(String nickname) {
        users.remove(nickname);
        operators.remove(nickname);
    }
    
    public void addOperator(String nickname) {
        if (users.contains(nickname)) {
            operators.add(nickname);
        }
    }
    
    public void removeOperator(String nickname) {
        operators.remove(nickname);
    }
    
    public boolean isOperator(String nickname) {
        return operators.contains(nickname);
    }
    
    public boolean hasUser(String nickname) {
        return users.contains(nickname);
    }
    
    public boolean isBanned(String nickname) {
        return banned.contains(nickname);
    }
    
    public void banUser(String nickname) {
        banned.add(nickname);
    }
    
    public void unbanUser(String nickname) {
        banned.remove(nickname);
    }
    
    public void inviteUser(String nickname) {
        invited.add(nickname);
    }
    
    public boolean isInvited(String nickname) {
        return invited.contains(nickname);
    }
    
    public boolean canJoin(String nickname) {
        if (isBanned(nickname)) {
            return false;
        }
        
        if (inviteOnly && !isInvited(nickname)) {
            return false;
        }
        
        if (userLimit > 0 && users.size() >= userLimit) {
            return false;
        }
        
        return true;
    }
    
    public void setTopic(String topic, String setBy) {
        this.topic = topic;
        this.topicSetBy = setBy;
        this.topicSetAt = LocalDateTime.now();
    }
    
    public String getModeString() {
        StringBuilder modes = new StringBuilder("+");
        
        if (inviteOnly) modes.append("i");
        if (moderated) modes.append("m");
        if (secret) modes.append("s");
        if (privateChannel) modes.append("p");
        if (topicLocked) modes.append("t");
        if (key != null) modes.append("k");
        if (userLimit > 0) modes.append("l");
        
        return modes.toString();
    }
    
    public boolean isEmpty() {
        return users.isEmpty();
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getTopicSetBy() {
        return topicSetBy;
    }
    
    public void setTopicSetBy(String topicSetBy) {
        this.topicSetBy = topicSetBy;
    }
    
    public LocalDateTime getTopicSetAt() {
        return topicSetAt;
    }
    
    public void setTopicSetAt(LocalDateTime topicSetAt) {
        this.topicSetAt = topicSetAt;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public int getUserLimit() {
        return userLimit;
    }
    
    public void setUserLimit(int userLimit) {
        this.userLimit = userLimit;
    }
    
    public boolean isInviteOnly() {
        return inviteOnly;
    }
    
    public void setInviteOnly(boolean inviteOnly) {
        this.inviteOnly = inviteOnly;
    }
    
    public boolean isModerated() {
        return moderated;
    }
    
    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }
    
    public boolean isSecret() {
        return secret;
    }
    
    public void setSecret(boolean secret) {
        this.secret = secret;
    }
    
    public boolean isPrivateChannel() {
        return privateChannel;
    }
    
    public void setPrivateChannel(boolean privateChannel) {
        this.privateChannel = privateChannel;
    }
    
    public boolean isTopicLocked() {
        return topicLocked;
    }
    
    public void setTopicLocked(boolean topicLocked) {
        this.topicLocked = topicLocked;
    }
    
    public Set<String> getUsers() {
        return users;
    }
    
    public Set<String> getOperators() {
        return operators;
    }
    
    public Set<String> getBanned() {
        return banned;
    }
    
    public Set<String> getInvited() {
        return invited;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "IrcChannel{" +
                "name='" + name + '\'' +
                ", topic='" + topic + '\'' +
                ", users=" + users.size() +
                ", operators=" + operators.size() +
                ", modes='" + getModeString() + '\'' +
                '}';
    }
} 