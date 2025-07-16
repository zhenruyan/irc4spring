# IRC4Spring - Java 21 Virtual Thread IRC Server

High-performance IRC server implementation based on Spring Boot and Java 21 virtual threads.

## Features

- ✅ **Java 21 Virtual Threads**: Uses the latest Java 21 virtual thread technology for high concurrency
- ✅ **Authentication**: Complete user authentication and permission management system
- ✅ **Admin Operations**: Supports admin commands and REST API management
- ✅ **Minimal Dependencies**: Uses only Spring Boot core components, no additional external dependencies
- ✅ **IRC Protocol Compatible**: Supports standard IRC protocol commands
- ✅ **Channel Management**: Complete channel creation, joining, leaving, kicking functionality
- ✅ **Real-time Messaging**: Supports private messages and channel messages
- ✅ **REST API**: Provides complete management REST API interface

## System Requirements

- Java 21 or higher
- Maven 3.6+
- Memory: Minimum 512MB

## Quick Start

### 1. Clone the Project

```bash
git clone <repository-url>
cd irc4spring
```

### 2. Build the Project

```bash
mvn clean package
```

### 3. Start the Server

```bash
# Using startup script
./start.sh

# Or run JAR directly
java -jar target/irc-server-1.0.0.jar
```

### 4. Connection Test

After server startup:
- IRC port: `6667`
- HTTP API port: `8081`
- Default admin account: `admin/admin123`

Use an IRC client to connect to `localhost:6667`

## Configuration

Edit the `src/main/resources/application.yml` file to modify configuration:

### Unregistered User Channel Access

By setting `irc.auth.allow-unregistered-channels: true`, you can allow unregistered users to enter and create channels. In this mode:

- Users only need to set a nickname (NICK command) to join channels
- No complete user registration (USER command) required
- No password authentication needed
- Still supports all channel features (sending messages, setting topics, etc.)

This is particularly useful for temporary chat or testing environments.

```yaml
server:
  port: 8081  # HTTP API port

irc:
  server:
    name: "IRC4Spring"
    port: 6667  # IRC server port
    max-connections: 1000
    max-channels: 100
    max-nickname-length: 30
    max-channel-name-length: 50
    
  admin:
    default-username: "admin"
    default-password: "admin123"
    
  auth:
    require-registration: false
    session-timeout: 3600000  # 1 hour
    allow-unregistered-channels: true  # Allow unregistered users to enter/create channels
```

## IRC Command Support

### Basic Commands
- `NICK <nickname>` - Set nickname
- `USER <username> <hostname> <servername> <realname>` - User registration
- `PASS <password>` - Set password
- `QUIT [message]` - Quit server
- `PING <server>` - Heartbeat check
- `PONG <server>` - Heartbeat response

### Channel Commands
- `JOIN <channel>[,<channel>] [key]` - Join channel
- `PART <channel>[,<channel>] [message]` - Leave channel
- `PRIVMSG <target> <message>` - Send message
- `NOTICE <target> <message>` - Send notice
- `TOPIC <channel> [topic]` - View/set channel topic
- `NAMES [channel]` - View channel user list
- `LIST` - List all channels
- `KICK <channel> <user> [reason]` - Kick user
- `INVITE <user> <channel>` - Invite user

### Query Commands
- `WHO [target]` - Query user information
- `WHOIS <nickname>` - Query detailed user information
- `MODE <target> [modes]` - View/set modes

### Admin Commands
- `OPER <username> <password>` - Get operator privileges
- `KILL <user> [reason]` - Force disconnect user
- `WALLOPS <message>` - Send message to all operators

## REST API

### Server Status
```bash
GET /api/admin/status
```

### User Management
```bash
GET /api/admin/users              # Get all users
GET /api/admin/users/{nickname}   # Get user information
POST /api/admin/users/{nickname}/kick  # Kick user
POST /api/admin/users/{nickname}/role  # Set user role
POST /api/admin/users             # Create user account
DELETE /api/admin/users/{username}  # Delete user account
```

### Channel Management
```bash
GET /api/admin/channels                    # Get all channels
GET /api/admin/channels/{channelName}      # Get channel information
DELETE /api/admin/channels/{channelName}   # Delete channel
POST /api/admin/channels/{channelName}/message  # Send admin message
```

### System Management
```bash
POST /api/admin/broadcast  # Server-wide broadcast
POST /api/admin/shutdown   # Shutdown server
GET /api/admin/runtime     # Get runtime information
```

## Architecture Design

### Virtual Thread Usage

The project fully utilizes Java 21's virtual thread features:

1. **Connection Handling**: Each client connection uses an independent virtual thread
2. **Command Processing**: Each IRC command is processed in an independent virtual thread
3. **Message Broadcasting**: Asynchronous message sending uses virtual thread pools

### Core Components

- **IrcServer**: Main TCP server class, handles client connections
- **IrcCommandHandler**: IRC command parsing and processing
- **UserService**: User management service
- **ChannelService**: Channel management service
- **AuthenticationService**: Authentication and permission management
- **AdminController**: REST API management interface

### Data Models

- **IrcUser**: User model, contains nickname, permissions, connection info
- **IrcChannel**: Channel model, contains user list, topic, modes
- **IrcMessage**: IRC message model, supports protocol parsing
- **UserRole**: User role enumeration (USER, OPERATOR, ADMIN)

## Performance Features

- **High Concurrency**: Supports 1000+ concurrent connections
- **Low Latency**: Virtual threads reduce thread switching overhead
- **Memory Efficiency**: Virtual threads use less memory
- **Scalable**: Based on Spring Boot, easy to extend

## Security Features

- **Password Encryption**: Uses SHA-256 + salt for encrypted storage
- **Permission Control**: Role-based permission management
- **Session Management**: Automatic cleanup of timeout connections
- **Input Validation**: Nickname and channel name format validation

## Development

### Project Structure

```
src/main/java/com/irc4spring/
├── IrcServerApplication.java      # Main application class
├── constant/
│   └── IrcCommand.java            # IRC command constants
├── controller/
│   └── AdminController.java       # REST API controller
├── handler/
│   └── IrcCommandHandler.java     # IRC command handler
├── model/
│   ├── IrcChannel.java           # Channel model
│   ├── IrcMessage.java           # Message model
│   ├── IrcUser.java              # User model
│   └── UserRole.java             # User role
├── server/
│   └── IrcServer.java            # TCP server
└── service/
    ├── AuthenticationService.java # Authentication service
    ├── ChannelService.java       # Channel service
    └── UserService.java          # User service
```

### Build and Test

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package
mvn package

# Run
java -jar target/irc-server-1.0.0.jar
```

## Usage Examples

### Connecting to Server

```irc
NICK testuser
USER testuser 0 * :Test User
JOIN #general
PRIVMSG #general :Hello, world!
```

### Admin Operations

```bash
# Get server status
curl http://localhost:8081/api/admin/status

# Kick user
curl -X POST http://localhost:8081/api/admin/users/testuser/kick \
  -H "Content-Type: application/json" \
  -d '{"reason": "Violation"}'

# Server-wide broadcast
curl -X POST http://localhost:8081/api/admin/broadcast \
  -H "Content-Type: application/json" \
  -d '{"message": "Server maintenance notice"}'
```

## Troubleshooting

### Common Issues

1. **Java Version Error**: Ensure using Java 21 or higher
2. **Port Occupied**: Check if ports 6667 and 8081 are in use
3. **Insufficient Memory**: Increase JVM memory setting `-Xmx1g`
4. **Connection Timeout**: Check firewall settings

### Log Levels

Adjust log levels in `application.yml`:

```yaml
logging:
  level:
    com.irc4spring: DEBUG  # Detailed logs
    org.springframework: INFO
```

## Contributing

Issues and Pull Requests are welcome!

## License

This project is licensed under the MIT License.

## Changelog

### v1.0.0
- Initial release
- Basic IRC protocol support
- User authentication and permission management
- Channel management functionality
- REST API management interface
- Java 21 virtual thread support 