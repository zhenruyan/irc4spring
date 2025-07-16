package com.irc4spring.handler;

import com.irc4spring.constant.IrcCommand;
import com.irc4spring.model.IrcMessage;
import com.irc4spring.model.IrcUser;
import com.irc4spring.model.UserRole;
import com.irc4spring.service.AuthenticationService;
import com.irc4spring.service.ChannelService;
import com.irc4spring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * IRC命令处理器
 */
@Component
public class IrcCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(IrcCommandHandler.class);
    
    @Value("${irc.server.name:IRC4Spring}")
    private String serverName;
    
    @Value("${irc.server.version:1.0.0}")
    private String serverVersion;
    
    @Value("${irc.server.motd:欢迎来到IRC4Spring服务器！}")
    private String motd;
    
    @Value("${irc.auth.allow-unregistered-channels:true}")
    private boolean allowUnregisteredChannels;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * 处理IRC命令
     */
    public void handleCommand(Socket socket, IrcMessage message) {
        if (message == null || message.getCommand() == null) {
            return;
        }
        
        IrcUser user = userService.getUserBySocket(socket);
        String command = message.getCommand().toUpperCase();
        
        try {
            switch (command) {
                case IrcCommand.NICK -> handleNick(socket, message);
                case IrcCommand.USER -> handleUser(socket, message);
                case IrcCommand.PASS -> handlePass(socket, message);
                case IrcCommand.PING -> handlePing(socket, message);
                case IrcCommand.PONG -> handlePong(socket, message);
                case IrcCommand.JOIN -> handleJoin(socket, message);
                case IrcCommand.PART -> handlePart(socket, message);
                case IrcCommand.PRIVMSG -> handlePrivmsg(socket, message);
                case IrcCommand.NOTICE -> handleNotice(socket, message);
                case IrcCommand.TOPIC -> handleTopic(socket, message);
                case IrcCommand.NAMES -> handleNames(socket, message);
                case IrcCommand.LIST -> handleList(socket, message);
                case IrcCommand.WHO -> handleWho(socket, message);
                case IrcCommand.WHOIS -> handleWhois(socket, message);
                case IrcCommand.MODE -> handleMode(socket, message);
                case IrcCommand.KICK -> handleKick(socket, message);
                case IrcCommand.INVITE -> handleInvite(socket, message);
                case IrcCommand.QUIT -> handleQuit(socket, message);
                case IrcCommand.OPER -> handleOper(socket, message);
                case IrcCommand.KILL -> handleKill(socket, message);
                case IrcCommand.WALLOPS -> handleWallops(socket, message);
                default -> handleUnknownCommand(socket, command);
            }
        } catch (Exception e) {
            logger.error("处理命令时发生错误: {}", command, e);
            sendErrorToUser(socket, "服务器内部错误");
        }
    }
    
    /**
     * 处理NICK命令
     */
    private void handleNick(Socket socket, IrcMessage message) {
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NONICKNAMEGIVEN, "No nickname given");
            return;
        }
        
        String nickname = message.getParams().get(0);
        IrcUser user = userService.getUserBySocket(socket);
        
        if (!userService.isNicknameAvailable(nickname)) {
            if (userService.getUserByNickname(nickname) != null) {
                sendErrorToUser(socket, IrcCommand.ERR_NICKNAMEINUSE, nickname, "Nickname is already in use");
            } else {
                sendErrorToUser(socket, IrcCommand.ERR_ERRONEUSNICKNAME, nickname, "Erroneous nickname");
            }
            return;
        }
        
        if (user == null) {
            // 新用户
            user = new IrcUser();
            user.setNickname(nickname);
            user.setSocket(socket);
            user.setHostname(socket.getInetAddress().getHostAddress());
            userService.addUser(user);
        } else {
            // 更改昵称
            String oldNickname = user.getNickname();
            if (userService.changeNickname(oldNickname, nickname)) {
                // 广播昵称更改
                String nickMessage = ":" + user.getFullMask() + " NICK :" + nickname;
                for (String channelName : user.getChannels()) {
                    channelService.broadcastToChannel(channelName, nickMessage, null);
                }
            }
        }
        
        checkRegistration(user);
    }
    
    /**
     * 处理USER命令
     */
    private void handleUser(Socket socket, IrcMessage message) {
        if (message.getParams().size() < 3 || message.getTrailing() == null) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.USER, "Not enough parameters");
            return;
        }
        
        IrcUser user = userService.getUserBySocket(socket);
        if (user == null) {
            user = new IrcUser();
            user.setSocket(socket);
            user.setHostname(socket.getInetAddress().getHostAddress());
            userService.addUser(user);
        }
        
        if (user.isRegistered()) {
            sendErrorToUser(socket, IrcCommand.ERR_ALREADYREGISTRED, "You may not reregister");
            return;
        }
        
        user.setUsername(message.getParams().get(0));
        user.setRealname(message.getTrailing());
        
        checkRegistration(user);
    }
    
    /**
     * 处理PASS命令
     */
    private void handlePass(Socket socket, IrcMessage message) {
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.PASS, "Not enough parameters");
            return;
        }
        
        IrcUser user = userService.getUserBySocket(socket);
        if (user == null) {
            user = new IrcUser();
            user.setSocket(socket);
            user.setHostname(socket.getInetAddress().getHostAddress());
            userService.addUser(user);
        }
        
        user.setPassword(message.getParams().get(0));
    }
    
    /**
     * 处理PING命令
     */
    private void handlePing(Socket socket, IrcMessage message) {
        String server = message.getParams().isEmpty() ? serverName : message.getParams().get(0);
        IrcMessage pong = new IrcMessage(serverName, IrcCommand.PONG, List.of(serverName), server);
        sendToUser(socket, pong.toIrcString());
    }
    
    /**
     * 处理PONG命令
     */
    private void handlePong(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (user != null) {
            user.updateActivity();
        }
    }
    
    /**
     * 处理JOIN命令
     */
    private void handleJoin(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            if (allowUnregisteredChannels) {
                sendErrorToUser(socket, "You need to set a nickname first");
            } else {
                sendErrorToUser(socket, "You have not registered");
            }
            return;
        }
        
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.JOIN, "Not enough parameters");
            return;
        }
        
        String[] channels = message.getParams().get(0).split(",");
        String[] keys = message.getParams().size() > 1 ? message.getParams().get(1).split(",") : new String[0];
        
        for (int i = 0; i < channels.length; i++) {
            String channelName = channels[i].trim();
            String key = i < keys.length ? keys[i] : null;
            
            if (channelService.joinChannel(user.getNickname(), channelName, key)) {
                // 发送主题信息
                sendChannelTopic(socket, channelName);
                // 发送用户列表
                sendChannelNames(socket, channelName);
            } else {
                sendErrorToUser(socket, IrcCommand.ERR_NOSUCHCHANNEL, channelName, "No such channel");
            }
        }
    }
    
    /**
     * 处理PART命令
     */
    private void handlePart(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            return;
        }
        
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.PART, "Not enough parameters");
            return;
        }
        
        String[] channels = message.getParams().get(0).split(",");
        String reason = message.getTrailing();
        
        for (String channelName : channels) {
            channelName = channelName.trim();
            if (!channelService.leaveChannel(user.getNickname(), channelName, reason)) {
                sendErrorToUser(socket, IrcCommand.ERR_NOTONCHANNEL, channelName, "You're not on that channel");
            }
        }
    }
    
    /**
     * 处理PRIVMSG命令
     */
    private void handlePrivmsg(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            return;
        }
        
        if (message.getParams().isEmpty() || message.getTrailing() == null) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.PRIVMSG, "Not enough parameters");
            return;
        }
        
        String target = message.getParams().get(0);
        String text = message.getTrailing();
        
        if (target.startsWith("#")) {
            // 频道消息
            if (!channelService.sendChannelMessage(user.getNickname(), target, text)) {
                sendErrorToUser(socket, IrcCommand.ERR_CANNOTSENDTOCHAN, target, "Cannot send to channel");
            }
        } else {
            // 私聊消息
            if (!channelService.sendPrivateMessage(user.getNickname(), target, text)) {
                sendErrorToUser(socket, IrcCommand.ERR_NOSUCHNICK, target, "No such nick/channel");
            }
        }
    }
    
    /**
     * 处理NOTICE命令
     */
    private void handleNotice(Socket socket, IrcMessage message) {
        // NOTICE与PRIVMSG类似，但不应该产生自动回复
        handlePrivmsg(socket, message);
    }
    
    /**
     * 处理TOPIC命令
     */
    private void handleTopic(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            return;
        }
        
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.TOPIC, "Not enough parameters");
            return;
        }
        
        String channelName = message.getParams().get(0);
        
        if (message.getTrailing() == null) {
            // 查询主题
            sendChannelTopic(socket, channelName);
        } else {
            // 设置主题
            String topic = message.getTrailing();
            if (!channelService.setChannelTopic(user.getNickname(), channelName, topic)) {
                sendErrorToUser(socket, IrcCommand.ERR_CHANOPRIVSNEEDED, channelName, "You're not channel operator");
            }
        }
    }
    
    /**
     * 处理NAMES命令
     */
    private void handleNames(Socket socket, IrcMessage message) {
        if (message.getParams().isEmpty()) {
            // 列出所有频道的用户
            for (var channel : channelService.getAllChannels()) {
                sendChannelNames(socket, channel.getName());
            }
        } else {
            String[] channels = message.getParams().get(0).split(",");
            for (String channelName : channels) {
                sendChannelNames(socket, channelName.trim());
            }
        }
    }
    
    /**
     * 处理LIST命令
     */
    private void handleList(Socket socket, IrcMessage message) {
        for (var channel : channelService.getAllChannels()) {
            if (!channel.isSecret()) {
                String listReply = String.format("322 %s %s %d :%s", 
                    getCurrentNickname(socket), channel.getName(), 
                    channel.getUsers().size(), channel.getTopic() != null ? channel.getTopic() : "");
                sendToUser(socket, ":" + serverName + " " + listReply);
            }
        }
        
        String endOfList = String.format("323 %s :End of /LIST", getCurrentNickname(socket));
        sendToUser(socket, ":" + serverName + " " + endOfList);
    }
    
    /**
     * 处理WHO命令
     */
    private void handleWho(Socket socket, IrcMessage message) {
        // 简化的WHO实现
        String endOfWho = String.format("315 %s * :End of /WHO list", getCurrentNickname(socket));
        sendToUser(socket, ":" + serverName + " " + endOfWho);
    }
    
    /**
     * 处理WHOIS命令
     */
    private void handleWhois(Socket socket, IrcMessage message) {
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.WHOIS, "Not enough parameters");
            return;
        }
        
        String targetNickname = message.getParams().get(0);
        IrcUser target = userService.getUserByNickname(targetNickname);
        String currentNickname = getCurrentNickname(socket);
        
        if (target == null) {
            sendErrorToUser(socket, IrcCommand.ERR_NOSUCHNICK, targetNickname, "No such nick");
            return;
        }
        
        // 发送WHOIS信息
        String whoisUser = String.format("311 %s %s %s %s * :%s", 
            currentNickname, target.getNickname(), target.getUsername(), 
            target.getHostname(), target.getRealname());
        sendToUser(socket, ":" + serverName + " " + whoisUser);
        
        String whoisServer = String.format("312 %s %s %s :%s", 
            currentNickname, target.getNickname(), serverName, serverName);
        sendToUser(socket, ":" + serverName + " " + whoisServer);
        
        if (target.hasPermission(UserRole.OPERATOR)) {
            String whoisOperator = String.format("313 %s %s :is an IRC operator", 
                currentNickname, target.getNickname());
            sendToUser(socket, ":" + serverName + " " + whoisOperator);
        }
        
        String endOfWhois = String.format("318 %s %s :End of /WHOIS list", 
            currentNickname, target.getNickname());
        sendToUser(socket, ":" + serverName + " " + endOfWhois);
    }
    
    /**
     * 处理MODE命令
     */
    private void handleMode(Socket socket, IrcMessage message) {
        // 简化的MODE实现
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.MODE, "Not enough parameters");
            return;
        }
        
        String target = message.getParams().get(0);
        if (target.startsWith("#")) {
            // 频道模式
            var channel = channelService.getChannel(target);
            if (channel != null) {
                String modeReply = String.format("324 %s %s %s", 
                    getCurrentNickname(socket), target, channel.getModeString());
                sendToUser(socket, ":" + serverName + " " + modeReply);
            }
        }
    }
    
    /**
     * 处理KICK命令
     */
    private void handleKick(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            return;
        }
        
        if (message.getParams().size() < 2) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.KICK, "Not enough parameters");
            return;
        }
        
        String channelName = message.getParams().get(0);
        String targetNickname = message.getParams().get(1);
        String reason = message.getTrailing();
        
        if (!channelService.kickUser(user.getNickname(), channelName, targetNickname, reason)) {
            sendErrorToUser(socket, IrcCommand.ERR_CHANOPRIVSNEEDED, channelName, "You're not channel operator");
        }
    }
    
    /**
     * 处理INVITE命令
     */
    private void handleInvite(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (!canPerformChannelOperations(user)) {
            return;
        }
        
        if (message.getParams().size() < 2) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.INVITE, "Not enough parameters");
            return;
        }
        
        String targetNickname = message.getParams().get(0);
        String channelName = message.getParams().get(1);
        
        channelService.inviteUser(user.getNickname(), channelName, targetNickname);
    }
    
    /**
     * 处理QUIT命令
     */
    private void handleQuit(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (user != null) {
            String quitMessage = ":" + user.getFullMask() + " QUIT";
            if (message.getTrailing() != null) {
                quitMessage += " :" + message.getTrailing();
            }
            
            // 广播退出消息到所有频道
            for (String channelName : user.getChannels()) {
                channelService.broadcastToChannel(channelName, quitMessage, user.getNickname());
                channelService.leaveChannel(user.getNickname(), channelName, null);
            }
            
            userService.removeUser(user.getNickname());
        }
    }
    
    /**
     * 处理OPER命令（管理员权限）
     */
    private void handleOper(Socket socket, IrcMessage message) {
        if (message.getParams().size() < 2) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.OPER, "Not enough parameters");
            return;
        }
        
        String username = message.getParams().get(0);
        String password = message.getParams().get(1);
        
        if (authService.authenticateOperator(username, password)) {
            IrcUser user = userService.getUserBySocket(socket);
            if (user != null) {
                user.setRole(authService.getUserRole(username));
                user.setAuthenticated(true);
                
                String operReply = String.format("381 %s :You are now an IRC operator", user.getNickname());
                sendToUser(socket, ":" + serverName + " " + operReply);
                
                logger.info("用户获得操作员权限: {} (角色: {})", user.getNickname(), user.getRole());
            }
        } else {
            sendErrorToUser(socket, IrcCommand.ERR_PASSWDMISMATCH, "Password incorrect");
        }
    }
    
    /**
     * 处理KILL命令（管理员命令）
     */
    private void handleKill(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (user == null || !user.hasPermission(UserRole.OPERATOR)) {
            sendErrorToUser(socket, IrcCommand.ERR_NOPRIVILEGES, "Permission Denied- You're not an IRC operator");
            return;
        }
        
        if (message.getParams().isEmpty()) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.KILL, "Not enough parameters");
            return;
        }
        
        String targetNickname = message.getParams().get(0);
        String reason = message.getTrailing() != null ? message.getTrailing() : "Killed by operator";
        
        if (userService.kickUser(targetNickname, reason)) {
            logger.info("用户被管理员踢出: {} (操作员: {}, 原因: {})", targetNickname, user.getNickname(), reason);
        } else {
            sendErrorToUser(socket, IrcCommand.ERR_NOSUCHNICK, targetNickname, "No such nick");
        }
    }
    
    /**
     * 处理WALLOPS命令（管理员广播）
     */
    private void handleWallops(Socket socket, IrcMessage message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (user == null || !user.hasPermission(UserRole.OPERATOR)) {
            sendErrorToUser(socket, IrcCommand.ERR_NOPRIVILEGES, "Permission Denied- You're not an IRC operator");
            return;
        }
        
        if (message.getTrailing() == null) {
            sendErrorToUser(socket, IrcCommand.ERR_NEEDMOREPARAMS, IrcCommand.WALLOPS, "Not enough parameters");
            return;
        }
        
        String wallopsMessage = ":" + user.getFullMask() + " WALLOPS :" + message.getTrailing();
        userService.sendMessageToRole(wallopsMessage, UserRole.OPERATOR);
        
        logger.info("管理员广播: {} -> {}", user.getNickname(), message.getTrailing());
    }
    
    /**
     * 处理未知命令
     */
    private void handleUnknownCommand(Socket socket, String command) {
        sendErrorToUser(socket, IrcCommand.ERR_UNKNOWNCOMMAND, command, "Unknown command");
    }
    
    /**
     * 检查用户是否可以执行频道操作
     */
    private boolean canPerformChannelOperations(IrcUser user) {
        if (user == null) {
            return false;
        }
        
        // 如果允许非注册用户使用频道，只需要有昵称即可
        if (allowUnregisteredChannels) {
            return user.getNickname() != null && !user.getNickname().trim().isEmpty();
        }
        
        // 否则需要完全注册
        return user.isRegistered();
    }
    
    /**
     * 检查用户注册状态
     */
    private void checkRegistration(IrcUser user) {
        if (!user.isRegistered() && user.getNickname() != null && user.getUsername() != null) {
            // 检查认证
            if (authService.isRegistrationRequired()) {
                if (user.getPassword() != null && authService.authenticateUser(user.getUsername(), user.getPassword())) {
                    user.setAuthenticated(true);
                    user.setRole(authService.getUserRole(user.getUsername()));
                } else {
                    sendErrorToUser(user.getSocket(), IrcCommand.ERR_PASSWDMISMATCH, "Password incorrect");
                    return;
                }
            }
            
            user.setRegistered(true);
            sendWelcomeMessages(user);
        } else if (allowUnregisteredChannels && !user.isRegistered() && user.getNickname() != null && user.getUsername() == null) {
            // 在允许非注册频道模式下，只有昵称也可以进行基本操作
            // 但仍需要发送欢迎消息让用户知道连接成功
            sendWelcomeMessages(user);
        }
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessages(IrcUser user) {
        String nickname = user.getNickname();
        
        // 001 RPL_WELCOME
        String welcome = String.format("001 %s :Welcome to the %s Network %s", 
            nickname, serverName, user.getFullMask());
        sendToUser(user.getSocket(), ":" + serverName + " " + welcome);
        
        // 002 RPL_YOURHOST
        String yourHost = String.format("002 %s :Your host is %s, running version %s", 
            nickname, serverName, serverVersion);
        sendToUser(user.getSocket(), ":" + serverName + " " + yourHost);
        
        // 003 RPL_CREATED
        String created = String.format("003 %s :This server was created %s", 
            nickname, "sometime");
        sendToUser(user.getSocket(), ":" + serverName + " " + created);
        
        // 004 RPL_MYINFO
        String myInfo = String.format("004 %s %s %s oiwszcrkfydnxbauglZCD bkloveqjfI", 
            nickname, serverName, serverVersion);
        sendToUser(user.getSocket(), ":" + serverName + " " + myInfo);
        
        // 发送MOTD
        sendMotd(user.getSocket());
    }
    
    /**
     * 发送MOTD
     */
    private void sendMotd(Socket socket) {
        String nickname = getCurrentNickname(socket);
        
        String motdStart = String.format("375 %s :- %s Message of the day - ", nickname, serverName);
        sendToUser(socket, ":" + serverName + " " + motdStart);
        
        String[] motdLines = motd.split("\n");
        for (String line : motdLines) {
            String motdLine = String.format("372 %s :- %s", nickname, line);
            sendToUser(socket, ":" + serverName + " " + motdLine);
        }
        
        String motdEnd = String.format("376 %s :End of /MOTD command", nickname);
        sendToUser(socket, ":" + serverName + " " + motdEnd);
    }
    
    /**
     * 发送频道主题
     */
    private void sendChannelTopic(Socket socket, String channelName) {
        var channel = channelService.getChannel(channelName);
        String nickname = getCurrentNickname(socket);
        
        if (channel != null) {
            if (channel.getTopic() != null) {
                String topicReply = String.format("332 %s %s :%s", 
                    nickname, channelName, channel.getTopic());
                sendToUser(socket, ":" + serverName + " " + topicReply);
            } else {
                String noTopicReply = String.format("331 %s %s :No topic is set", 
                    nickname, channelName);
                sendToUser(socket, ":" + serverName + " " + noTopicReply);
            }
        }
    }
    
    /**
     * 发送频道用户列表
     */
    private void sendChannelNames(Socket socket, String channelName) {
        List<String> users = channelService.getChannelUsers(channelName);
        String nickname = getCurrentNickname(socket);
        
        if (!users.isEmpty()) {
            String userList = String.join(" ", users);
            String namesReply = String.format("353 %s = %s :%s", 
                nickname, channelName, userList);
            sendToUser(socket, ":" + serverName + " " + namesReply);
        }
        
        String endOfNames = String.format("366 %s %s :End of /NAMES list", 
            nickname, channelName);
        sendToUser(socket, ":" + serverName + " " + endOfNames);
    }
    
    /**
     * 发送错误消息
     */
    private void sendErrorToUser(Socket socket, String errorCode, String... params) {
        String nickname = getCurrentNickname(socket);
        StringBuilder error = new StringBuilder();
        error.append(errorCode).append(" ").append(nickname);
        
        for (String param : params) {
            error.append(" ").append(param);
        }
        
        sendToUser(socket, ":" + serverName + " " + error);
    }
    
    /**
     * 发送错误消息（简化版）
     */
    private void sendErrorToUser(Socket socket, String message) {
        sendToUser(socket, "ERROR :" + message);
    }
    
    /**
     * 向用户发送消息
     */
    private void sendToUser(Socket socket, String message) {
        IrcUser user = userService.getUserBySocket(socket);
        if (user != null && user.getNickname() != null) {
            userService.sendMessageToUser(user.getNickname(), message);
        } else {
            // 直接通过socket发送消息
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(message);
            } catch (IOException e) {
                logger.error("发送消息失败", e);
            }
        }
    }
    
    /**
     * 获取当前用户昵称
     */
    private String getCurrentNickname(Socket socket) {
        IrcUser user = userService.getUserBySocket(socket);
        return user != null && user.getNickname() != null ? user.getNickname() : "*";
    }
} 