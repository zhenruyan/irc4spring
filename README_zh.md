# IRC4Spring - Java 21 虚拟线程IRC服务器

基于Spring Boot和Java 21虚拟线程的高性能IRC服务器实现。

## 特性

- ✅ **Java 21虚拟线程**: 使用最新的Java 21虚拟线程技术，支持高并发连接
- ✅ **权限验证**: 完整的用户认证和权限管理系统
- ✅ **管理员操作**: 支持管理员命令和REST API管理
- ✅ **最小依赖**: 仅使用Spring Boot核心组件，无额外外部依赖
- ✅ **IRC协议兼容**: 支持标准IRC协议命令
- ✅ **频道管理**: 完整的频道创建、加入、离开、踢人等功能
- ✅ **实时消息**: 支持私聊和频道消息
- ✅ **REST API**: 提供完整的管理REST API接口

## 系统要求

- Java 21或更高版本
- Maven 3.6+
- 内存: 最少512MB

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd irc4spring
```

### 2. 构建项目

```bash
mvn clean package
```

### 3. 启动服务器

```bash
# 使用启动脚本
./start.sh

# 或直接运行JAR
java -jar target/irc-server-1.0.0.jar
```

### 4. 连接测试

服务器启动后：
- IRC端口: `6667`
- HTTP API端口: `8081`
- 默认管理员账户: `admin/admin123`

使用IRC客户端连接到 `localhost:6667`

## 配置

编辑 `src/main/resources/application.yml` 文件来修改配置：

### 非注册用户频道访问

通过设置 `irc.auth.allow-unregistered-channels: true`，可以允许非注册用户进入和创建频道。在此模式下：

- 用户只需设置昵称（NICK命令）即可加入频道
- 不需要完整的用户注册（USER命令）
- 不需要密码认证
- 仍然支持所有频道功能（发送消息、设置主题等）

这对于临时聊天或测试环境特别有用。

```yaml
server:
  port: 8081  # HTTP API端口

irc:
  server:
    name: "IRC4Spring"
    port: 6667  # IRC服务端口
    max-connections: 1000
    max-channels: 100
    max-nickname-length: 30
    max-channel-name-length: 50
    
  admin:
    default-username: "admin"
    default-password: "admin123"
    
  auth:
    require-registration: false
    session-timeout: 3600000  # 1小时
    allow-unregistered-channels: true  # 允许非注册用户进入/创建频道
```

## IRC命令支持

### 基本命令
- `NICK <nickname>` - 设置昵称
- `USER <username> <hostname> <servername> <realname>` - 用户注册
- `PASS <password>` - 设置密码
- `QUIT [message]` - 退出服务器
- `PING <server>` - 心跳检测
- `PONG <server>` - 心跳响应

### 频道命令
- `JOIN <channel>[,<channel>] [key]` - 加入频道
- `PART <channel>[,<channel>] [message]` - 离开频道
- `PRIVMSG <target> <message>` - 发送消息
- `NOTICE <target> <message>` - 发送通知
- `TOPIC <channel> [topic]` - 查看/设置频道主题
- `NAMES [channel]` - 查看频道用户列表
- `LIST` - 列出所有频道
- `KICK <channel> <user> [reason]` - 踢出用户
- `INVITE <user> <channel>` - 邀请用户

### 查询命令
- `WHO [target]` - 查询用户信息
- `WHOIS <nickname>` - 查询用户详细信息
- `MODE <target> [modes]` - 查看/设置模式

### 管理员命令
- `OPER <username> <password>` - 获取操作员权限
- `KILL <user> [reason]` - 强制断开用户连接
- `WALLOPS <message>` - 向所有操作员发送消息

## REST API

### 服务器状态
```bash
GET /api/admin/status
```

### 用户管理
```bash
GET /api/admin/users              # 获取所有用户
GET /api/admin/users/{nickname}   # 获取用户信息
POST /api/admin/users/{nickname}/kick  # 踢出用户
POST /api/admin/users/{nickname}/role  # 设置用户角色
POST /api/admin/users             # 创建用户账户
DELETE /api/admin/users/{username}  # 删除用户账户
```

### 频道管理
```bash
GET /api/admin/channels                    # 获取所有频道
GET /api/admin/channels/{channelName}      # 获取频道信息
DELETE /api/admin/channels/{channelName}   # 删除频道
POST /api/admin/channels/{channelName}/message  # 发送管理员消息
```

### 系统管理
```bash
POST /api/admin/broadcast  # 全服广播
POST /api/admin/shutdown   # 关闭服务器
GET /api/admin/runtime     # 获取运行时信息
```

## 架构设计

### 虚拟线程使用

项目充分利用Java 21的虚拟线程特性：

1. **连接处理**: 每个客户端连接使用独立的虚拟线程
2. **命令处理**: 每个IRC命令在独立的虚拟线程中处理
3. **消息广播**: 异步消息发送使用虚拟线程池

### 核心组件

- **IrcServer**: TCP服务器主类，处理客户端连接
- **IrcCommandHandler**: IRC命令解析和处理
- **UserService**: 用户管理服务
- **ChannelService**: 频道管理服务
- **AuthenticationService**: 认证和权限管理
- **AdminController**: REST API管理接口

### 数据模型

- **IrcUser**: 用户模型，包含昵称、权限、连接信息
- **IrcChannel**: 频道模型，包含用户列表、主题、模式
- **IrcMessage**: IRC消息模型，支持协议解析
- **UserRole**: 用户角色枚举（USER, OPERATOR, ADMIN）

## 性能特性

- **高并发**: 支持1000+并发连接
- **低延迟**: 虚拟线程减少线程切换开销
- **内存效率**: 虚拟线程占用内存更少
- **可扩展**: 基于Spring Boot，易于扩展

## 安全特性

- **密码加密**: 使用SHA-256+盐值加密存储
- **权限控制**: 基于角色的权限管理
- **会话管理**: 自动清理超时连接
- **输入验证**: 昵称和频道名称格式验证

## 开发

### 项目结构

```
src/main/java/com/irc4spring/
├── IrcServerApplication.java      # 主应用类
├── constant/
│   └── IrcCommand.java            # IRC命令常量
├── controller/
│   └── AdminController.java       # REST API控制器
├── handler/
│   └── IrcCommandHandler.java     # IRC命令处理器
├── model/
│   ├── IrcChannel.java           # 频道模型
│   ├── IrcMessage.java           # 消息模型
│   ├── IrcUser.java              # 用户模型
│   └── UserRole.java             # 用户角色
├── server/
│   └── IrcServer.java            # TCP服务器
└── service/
    ├── AuthenticationService.java # 认证服务
    ├── ChannelService.java       # 频道服务
    └── UserService.java          # 用户服务
```

### 构建和测试

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn package

# 运行
java -jar target/irc-server-1.0.0.jar
```

## 示例使用

### 连接到服务器

```irc
NICK testuser
USER testuser 0 * :Test User
JOIN #general
PRIVMSG #general :Hello, world!
```

### 管理员操作

```bash
# 获取服务器状态
curl http://localhost:8081/api/admin/status

# 踢出用户
curl -X POST http://localhost:8081/api/admin/users/testuser/kick \
  -H "Content-Type: application/json" \
  -d '{"reason": "违规行为"}'

# 全服广播
curl -X POST http://localhost:8081/api/admin/broadcast \
  -H "Content-Type: application/json" \
  -d '{"message": "服务器维护通知"}'
```

## 故障排除

### 常见问题

1. **Java版本错误**: 确保使用Java 21或更高版本
2. **端口占用**: 检查6667和8081端口是否被占用
3. **内存不足**: 增加JVM内存设置 `-Xmx1g`
4. **连接超时**: 检查防火墙设置

### 日志级别

在`application.yml`中调整日志级别：

```yaml
logging:
  level:
    com.irc4spring: DEBUG  # 详细日志
    org.springframework: INFO
```

## 贡献

欢迎提交Issue和Pull Request！

## 许可证

本项目采用MIT许可证。

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本IRC协议
- 用户认证和权限管理
- 频道管理功能
- REST API管理接口
- Java 21虚拟线程支持 