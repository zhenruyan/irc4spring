package com.irc4spring.service;

import com.irc4spring.model.IrcChannel;
import com.irc4spring.model.IrcUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 频道管理服务
 */
@Service
public class ChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);
    
    @Value("${irc.server.max-channels:100}")
    private int maxChannels;
    
    @Value("${irc.server.max-channel-name-length:50}")
    private int maxChannelNameLength;
    
    @Autowired
    private UserService userService;
    
    private final Map<String, IrcChannel> channels = new ConcurrentHashMap<>();
    
    /**
     * 创建频道
     */
    public IrcChannel createChannel(String channelName, String creatorNickname) {
        if (!isValidChannelName(channelName)) {
            logger.warn("无效的频道名称: {}", channelName);
            return null;
        }
        
        if (channels.size() >= maxChannels) {
            logger.warn("服务器已达到最大频道数: {}", maxChannels);
            return null;
        }
        
        if (channels.containsKey(channelName)) {
            return channels.get(channelName);
        }
        
        IrcChannel channel = new IrcChannel(channelName);
        channels.put(channelName, channel);
        
        // 创建者自动成为操作员
        if (creatorNickname != null) {
            channel.addUser(creatorNickname);
            channel.addOperator(creatorNickname);
        }
        
        logger.info("频道已创建: {} (创建者: {})", channelName, creatorNickname);
        return channel;
    }
    
    /**
     * 获取频道
     */
    public IrcChannel getChannel(String channelName) {
        return channels.get(channelName);
    }
    
    /**
     * 获取所有频道
     */
    public Collection<IrcChannel> getAllChannels() {
        return channels.values();
    }
    
    /**
     * 删除频道
     */
    public boolean deleteChannel(String channelName) {
        IrcChannel channel = channels.remove(channelName);
        if (channel != null) {
            // 通知所有用户频道已关闭
            for (String nickname : channel.getUsers()) {
                IrcUser user = userService.getUserByNickname(nickname);
                if (user != null) {
                    user.leaveChannel(channelName);
                    userService.sendMessageToUser(nickname, 
                        ":" + channelName + " NOTICE :频道已被删除");
                }
            }
            logger.info("频道已删除: {}", channelName);
            return true;
        }
        return false;
    }
    
    /**
     * 用户加入频道
     */
    public boolean joinChannel(String nickname, String channelName, String key) {
        IrcUser user = userService.getUserByNickname(nickname);
        if (user == null) {
            return false;
        }
        
        IrcChannel channel = getChannel(channelName);
        if (channel == null) {
            // 自动创建频道
            channel = createChannel(channelName, nickname);
            if (channel == null) {
                return false;
            }
        }
        
        // 检查是否可以加入
        if (!channel.canJoin(nickname)) {
            return false;
        }
        
        // 检查频道密码
        if (channel.getKey() != null && !channel.getKey().equals(key)) {
            return false;
        }
        
        // 加入频道
        channel.addUser(nickname);
        user.joinChannel(channelName);
        
        // 广播加入消息
        broadcastToChannel(channelName, 
            ":" + user.getFullMask() + " JOIN :" + channelName, null);
        
        logger.info("用户加入频道: {} -> {}", nickname, channelName);
        return true;
    }
    
    /**
     * 用户离开频道
     */
    public boolean leaveChannel(String nickname, String channelName, String reason) {
        IrcUser user = userService.getUserByNickname(nickname);
        IrcChannel channel = getChannel(channelName);
        
        if (user == null || channel == null || !channel.hasUser(nickname)) {
            return false;
        }
        
        // 广播离开消息
        String partMessage = ":" + user.getFullMask() + " PART " + channelName;
        if (reason != null && !reason.trim().isEmpty()) {
            partMessage += " :" + reason;
        }
        broadcastToChannel(channelName, partMessage, null);
        
        // 离开频道
        channel.removeUser(nickname);
        user.leaveChannel(channelName);
        
        // 如果频道为空，删除频道
        if (channel.isEmpty()) {
            deleteChannel(channelName);
        }
        
        logger.info("用户离开频道: {} <- {}", nickname, channelName);
        return true;
    }
    
    /**
     * 向频道广播消息
     */
    public void broadcastToChannel(String channelName, String message, String excludeNickname) {
        IrcChannel channel = getChannel(channelName);
        if (channel != null) {
            for (String nickname : channel.getUsers()) {
                if (!nickname.equals(excludeNickname)) {
                    userService.sendMessageToUser(nickname, message);
                }
            }
        }
    }
    
    /**
     * 发送私聊消息
     */
    public boolean sendPrivateMessage(String senderNickname, String targetNickname, String message) {
        IrcUser sender = userService.getUserByNickname(senderNickname);
        IrcUser target = userService.getUserByNickname(targetNickname);
        
        if (sender == null || target == null) {
            return false;
        }
        
        String privmsg = ":" + sender.getFullMask() + " PRIVMSG " + targetNickname + " :" + message;
        return userService.sendMessageToUser(targetNickname, privmsg);
    }
    
    /**
     * 发送频道消息
     */
    public boolean sendChannelMessage(String senderNickname, String channelName, String message) {
        IrcUser sender = userService.getUserByNickname(senderNickname);
        IrcChannel channel = getChannel(channelName);
        
        if (sender == null || channel == null || !channel.hasUser(senderNickname)) {
            return false;
        }
        
        // 检查频道是否被调制
        if (channel.isModerated() && !channel.isOperator(senderNickname)) {
            return false;
        }
        
        String privmsg = ":" + sender.getFullMask() + " PRIVMSG " + channelName + " :" + message;
        broadcastToChannel(channelName, privmsg, senderNickname);
        
        return true;
    }
    
    /**
     * 设置频道主题
     */
    public boolean setChannelTopic(String nickname, String channelName, String topic) {
        IrcUser user = userService.getUserByNickname(nickname);
        IrcChannel channel = getChannel(channelName);
        
        if (user == null || channel == null || !channel.hasUser(nickname)) {
            return false;
        }
        
        // 检查权限
        if (channel.isTopicLocked() && !channel.isOperator(nickname)) {
            return false;
        }
        
        channel.setTopic(topic, nickname);
        
        // 广播主题变更
        String topicMessage = ":" + user.getFullMask() + " TOPIC " + channelName + " :" + topic;
        broadcastToChannel(channelName, topicMessage, null);
        
        logger.info("频道主题已设置: {} -> {}", channelName, topic);
        return true;
    }
    
    /**
     * 踢出用户
     */
    public boolean kickUser(String operatorNickname, String channelName, String targetNickname, String reason) {
        IrcChannel channel = getChannel(channelName);
        if (channel == null || !channel.isOperator(operatorNickname) || !channel.hasUser(targetNickname)) {
            return false;
        }
        
        IrcUser operator = userService.getUserByNickname(operatorNickname);
        IrcUser target = userService.getUserByNickname(targetNickname);
        
        if (operator == null || target == null) {
            return false;
        }
        
        // 广播踢出消息
        String kickMessage = ":" + operator.getFullMask() + " KICK " + channelName + " " + targetNickname;
        if (reason != null && !reason.trim().isEmpty()) {
            kickMessage += " :" + reason;
        }
        broadcastToChannel(channelName, kickMessage, null);
        
        // 移除用户
        channel.removeUser(targetNickname);
        target.leaveChannel(channelName);
        
        logger.info("用户被踢出频道: {} <- {} (操作员: {}, 原因: {})", 
                   targetNickname, channelName, operatorNickname, reason);
        return true;
    }
    
    /**
     * 邀请用户
     */
    public boolean inviteUser(String inviterNickname, String channelName, String targetNickname) {
        IrcChannel channel = getChannel(channelName);
        if (channel == null || !channel.hasUser(inviterNickname)) {
            return false;
        }
        
        IrcUser inviter = userService.getUserByNickname(inviterNickname);
        IrcUser target = userService.getUserByNickname(targetNickname);
        
        if (inviter == null || target == null) {
            return false;
        }
        
        // 添加到邀请列表
        channel.inviteUser(targetNickname);
        
        // 发送邀请消息
        String inviteMessage = ":" + inviter.getFullMask() + " INVITE " + targetNickname + " :" + channelName;
        userService.sendMessageToUser(targetNickname, inviteMessage);
        
        logger.info("用户被邀请: {} -> {} (邀请者: {})", targetNickname, channelName, inviterNickname);
        return true;
    }
    
    /**
     * 获取频道用户列表
     */
    public List<String> getChannelUsers(String channelName) {
        IrcChannel channel = getChannel(channelName);
        if (channel != null) {
            return channel.getUsers().stream()
                    .map(nickname -> {
                        if (channel.isOperator(nickname)) {
                            return "@" + nickname;
                        }
                        return nickname;
                    })
                    .collect(Collectors.toList());
        }
        return List.of();
    }
    
    /**
     * 验证频道名称
     */
    private boolean isValidChannelName(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            return false;
        }
        
        // 频道名称必须以#开头
        if (!channelName.startsWith("#")) {
            return false;
        }
        
        // 检查长度
        if (channelName.length() > maxChannelNameLength) {
            return false;
        }
        
        // 频道名称不能包含空格、逗号、控制字符
        return !channelName.matches(".*[\\s,\\x00-\\x1F\\x7F].*");
    }
    
    /**
     * 获取频道统计信息
     */
    public Map<String, Object> getChannelStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalChannels", channels.size());
        stats.put("maxChannels", maxChannels);
        
        int totalUsers = channels.values().stream()
                .mapToInt(channel -> channel.getUsers().size())
                .sum();
        
        stats.put("totalChannelUsers", totalUsers);
        stats.put("averageUsersPerChannel", channels.isEmpty() ? 0 : totalUsers / channels.size());
        
        return stats;
    }
    
    /**
     * 获取频道详细信息
     */
    public Map<String, Object> getChannelInfo(String channelName) {
        IrcChannel channel = getChannel(channelName);
        if (channel == null) {
            return null;
        }
        
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("name", channel.getName());
        info.put("topic", channel.getTopic());
        info.put("topicSetBy", channel.getTopicSetBy());
        info.put("topicSetAt", channel.getTopicSetAt());
        info.put("userCount", channel.getUsers().size());
        info.put("operators", channel.getOperators());
        info.put("modes", channel.getModeString());
        info.put("createdAt", channel.getCreatedAt());
        info.put("hasKey", channel.getKey() != null);
        info.put("userLimit", channel.getUserLimit());
        
        return info;
    }
} 