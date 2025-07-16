package com.irc4spring.server;

import com.irc4spring.handler.IrcCommandHandler;
import com.irc4spring.model.IrcMessage;
import com.irc4spring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IRC服务器主类
 */
@Component
public class IrcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(IrcServer.class);
    
    @Value("${irc.server.port:6667}")
    private int port;
    
    @Value("${irc.server.max-connections:1000}")
    private int maxConnections;
    
    @Autowired
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;
    
    @Autowired
    private IrcCommandHandler commandHandler;
    
    @Autowired
    private UserService userService;
    
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService acceptorExecutor;
    
    @PostConstruct
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            
            // 使用虚拟线程处理连接接受
            acceptorExecutor = Executors.newVirtualThreadPerTaskExecutor();
            acceptorExecutor.submit(this::acceptConnections);
            
            logger.info("IRC服务器已启动，监听端口: {}", port);
            logger.info("最大连接数: {}", maxConnections);
            logger.info("使用Java 21虚拟线程处理连接");
            
        } catch (IOException e) {
            logger.error("启动IRC服务器失败", e);
            throw new RuntimeException("无法启动IRC服务器", e);
        }
    }
    
    @PreDestroy
    public void stop() {
        running.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("关闭服务器Socket失败", e);
            }
        }
        
        if (acceptorExecutor != null) {
            acceptorExecutor.shutdown();
        }
        
        logger.info("IRC服务器已停止");
    }
    
    /**
     * 接受客户端连接
     */
    private void acceptConnections() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // 检查连接数限制
                if (userService.getOnlineUserCount() >= maxConnections) {
                    logger.warn("达到最大连接数限制，拒绝新连接: {}", 
                               clientSocket.getInetAddress().getHostAddress());
                    
                    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        out.println("ERROR :服务器连接数已满");
                    }
                    clientSocket.close();
                    continue;
                }
                
                logger.info("接受新连接: {}", clientSocket.getInetAddress().getHostAddress());
                
                // 使用虚拟线程处理每个客户端连接
                Thread.ofVirtual()
                      .name("irc-client-" + clientSocket.getInetAddress().getHostAddress())
                      .start(() -> handleClient(clientSocket));
                
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("接受客户端连接时发生错误", e);
                }
            }
        }
    }
    
    /**
     * 处理客户端连接
     */
    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        logger.debug("开始处理客户端: {}", clientAddress);
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String inputLine;
            while ((inputLine = in.readLine()) != null && !clientSocket.isClosed()) {
                
                // 处理IRC消息
                handleIrcMessage(clientSocket, inputLine.trim());
                
                // 检查连接状态
                if (clientSocket.isClosed()) {
                    break;
                }
            }
            
        } catch (IOException e) {
            logger.debug("客户端连接异常: {} - {}", clientAddress, e.getMessage());
        } finally {
            // 清理用户连接
            cleanupClient(clientSocket);
        }
    }
    
    /**
     * 处理IRC消息
     */
    private void handleIrcMessage(Socket clientSocket, String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return;
        }
        
        try {
            logger.debug("收到消息: {} <- {}", 
                        clientSocket.getInetAddress().getHostAddress(), rawMessage);
            
            // 解析IRC消息
            IrcMessage message = IrcMessage.parse(rawMessage);
            if (message != null) {
                // 使用虚拟线程处理命令
                Thread.ofVirtual()
                      .name("irc-command-" + message.getCommand())
                      .start(() -> commandHandler.handleCommand(clientSocket, message));
            } else {
                logger.warn("无法解析IRC消息: {}", rawMessage);
            }
            
        } catch (Exception e) {
            logger.error("处理IRC消息时发生错误: {}", rawMessage, e);
            
            // 发送错误响应
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                out.println("ERROR :消息处理错误");
            } catch (IOException ioException) {
                logger.error("发送错误响应失败", ioException);
            }
        }
    }
    
    /**
     * 清理客户端连接
     */
    private void cleanupClient(Socket clientSocket) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        
        try {
            // 从用户服务中移除用户
            var user = userService.getUserBySocket(clientSocket);
            if (user != null) {
                logger.info("用户断开连接: {} ({})", user.getNickname(), clientAddress);
                
                // 广播退出消息到所有频道
                String quitMessage = ":" + user.getFullMask() + " QUIT :Connection closed";
                for (String channelName : user.getChannels()) {
                    // 这里需要通过ChannelService来处理
                    // channelService.broadcastToChannel(channelName, quitMessage, user.getNickname());
                }
                
                userService.removeUser(user.getNickname());
            }
            
            // 关闭socket
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
            
        } catch (Exception e) {
            logger.error("清理客户端连接时发生错误: {}", clientAddress, e);
        }
    }
    
    /**
     * 获取服务器状态
     */
    public ServerStatus getServerStatus() {
        return new ServerStatus(
            running.get(),
            port,
            userService.getOnlineUserCount(),
            maxConnections
        );
    }
    
    /**
     * 服务器状态记录
     */
    public record ServerStatus(
        boolean running,
        int port,
        int onlineUsers,
        int maxConnections
    ) {}
    
    /**
     * 优雅关闭服务器
     */
    public void gracefulShutdown() {
        logger.info("开始优雅关闭IRC服务器...");
        
        // 通知所有用户服务器即将关闭
        userService.broadcastMessage("NOTICE :服务器即将关闭，请保存您的工作");
        
        // 等待一段时间让用户处理
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 强制断开所有连接
        for (var user : userService.getAllUsers()) {
            try {
                userService.sendMessageToUser(user.getNickname(), "ERROR :服务器关闭");
                userService.removeUser(user.getNickname());
            } catch (Exception e) {
                logger.error("断开用户连接时发生错误: {}", user.getNickname(), e);
            }
        }
        
        // 停止服务器
        stop();
        
        logger.info("IRC服务器已优雅关闭");
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 获取当前连接数
     */
    public int getCurrentConnections() {
        return userService.getOnlineUserCount();
    }
} 