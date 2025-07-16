package com.irc4spring.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * IRC消息模型
 * 格式: [:prefix] <command> [params] [:trailing]
 */
public class IrcMessage {
    private String prefix;
    private String command;
    private List<String> params;
    private String trailing;

    public IrcMessage() {
        this.params = new ArrayList<>();
    }

    public IrcMessage(String command) {
        this();
        this.command = command;
    }

    public IrcMessage(String command, String... params) {
        this(command);
        for (String param : params) {
            this.params.add(param);
        }
    }

    public IrcMessage(String prefix, String command, List<String> params, String trailing) {
        this.prefix = prefix;
        this.command = command;
        this.params = params != null ? params : new ArrayList<>();
        this.trailing = trailing;
    }

    /**
     * 解析IRC消息字符串
     */
    public static IrcMessage parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        IrcMessage message = new IrcMessage();
        String[] parts = line.split(" ");
        int index = 0;

        // 解析前缀
        if (parts[index].startsWith(":")) {
            message.prefix = parts[index].substring(1);
            index++;
        }

        // 解析命令
        if (index < parts.length) {
            message.command = parts[index].toUpperCase();
            index++;
        }

        // 解析参数
        while (index < parts.length) {
            if (parts[index].startsWith(":")) {
                // 处理trailing参数
                StringBuilder trailing = new StringBuilder();
                trailing.append(parts[index].substring(1));
                index++;
                while (index < parts.length) {
                    trailing.append(" ").append(parts[index]);
                    index++;
                }
                message.trailing = trailing.toString();
                break;
            } else {
                message.params.add(parts[index]);
                index++;
            }
        }

        return message;
    }

    /**
     * 转换为IRC协议字符串
     */
    public String toIrcString() {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            sb.append(":").append(prefix).append(" ");
        }

        sb.append(command);

        for (String param : params) {
            sb.append(" ").append(param);
        }

        if (trailing != null && !trailing.isEmpty()) {
            sb.append(" :").append(trailing);
        }

        return sb.toString();
    }

    // Getters and Setters
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public String getTrailing() {
        return trailing;
    }

    public void setTrailing(String trailing) {
        this.trailing = trailing;
    }

    public void addParam(String param) {
        this.params.add(param);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IrcMessage that = (IrcMessage) o;
        return Objects.equals(prefix, that.prefix) &&
                Objects.equals(command, that.command) &&
                Objects.equals(params, that.params) &&
                Objects.equals(trailing, that.trailing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, command, params, trailing);
    }

    @Override
    public String toString() {
        return "IrcMessage{" +
                "prefix='" + prefix + '\'' +
                ", command='" + command + '\'' +
                ", params=" + params +
                ", trailing='" + trailing + '\'' +
                '}';
    }
} 