package com.irc4spring.constant;

/**
 * IRC命令常量
 */
public final class IrcCommand {
    
    // 用户命令
    public static final String NICK = "NICK";
    public static final String USER = "USER";
    public static final String PASS = "PASS";
    public static final String QUIT = "QUIT";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    
    // 频道命令
    public static final String JOIN = "JOIN";
    public static final String PART = "PART";
    public static final String PRIVMSG = "PRIVMSG";
    public static final String NOTICE = "NOTICE";
    public static final String TOPIC = "TOPIC";
    public static final String NAMES = "NAMES";
    public static final String LIST = "LIST";
    public static final String WHO = "WHO";
    public static final String WHOIS = "WHOIS";
    public static final String MODE = "MODE";
    public static final String KICK = "KICK";
    public static final String INVITE = "INVITE";
    
    // 管理员命令
    public static final String OPER = "OPER";
    public static final String KILL = "KILL";
    public static final String REHASH = "REHASH";
    public static final String RESTART = "RESTART";
    public static final String SQUIT = "SQUIT";
    public static final String CONNECT = "CONNECT";
    public static final String WALLOPS = "WALLOPS";
    
    // 服务器响应代码
    public static final String RPL_WELCOME = "001";
    public static final String RPL_YOURHOST = "002";
    public static final String RPL_CREATED = "003";
    public static final String RPL_MYINFO = "004";
    public static final String RPL_BOUNCE = "005";
    public static final String RPL_MOTDSTART = "375";
    public static final String RPL_MOTD = "372";
    public static final String RPL_ENDOFMOTD = "376";
    public static final String RPL_NAMREPLY = "353";
    public static final String RPL_ENDOFNAMES = "366";
    public static final String RPL_TOPIC = "332";
    public static final String RPL_NOTOPIC = "331";
    public static final String RPL_WHOISUSER = "311";
    public static final String RPL_WHOISSERVER = "312";
    public static final String RPL_WHOISOPERATOR = "313";
    public static final String RPL_ENDOFWHOIS = "318";
    public static final String RPL_CHANNELMODEIS = "324";
    public static final String RPL_YOUREOPER = "381";
    
    // 错误代码
    public static final String ERR_NOSUCHNICK = "401";
    public static final String ERR_NOSUCHSERVER = "402";
    public static final String ERR_NOSUCHCHANNEL = "403";
    public static final String ERR_CANNOTSENDTOCHAN = "404";
    public static final String ERR_TOOMANYCHANNELS = "405";
    public static final String ERR_UNKNOWNCOMMAND = "421";
    public static final String ERR_NONICKNAMEGIVEN = "431";
    public static final String ERR_ERRONEUSNICKNAME = "432";
    public static final String ERR_NICKNAMEINUSE = "433";
    public static final String ERR_USERNOTINCHANNEL = "441";
    public static final String ERR_NOTONCHANNEL = "442";
    public static final String ERR_USERONCHANNEL = "443";
    public static final String ERR_NEEDMOREPARAMS = "461";
    public static final String ERR_ALREADYREGISTRED = "462";
    public static final String ERR_PASSWDMISMATCH = "464";
    public static final String ERR_CHANNELISFULL = "471";
    public static final String ERR_UNKNOWNMODE = "472";
    public static final String ERR_INVITEONLYCHAN = "473";
    public static final String ERR_BANNEDFROMCHAN = "474";
    public static final String ERR_BADCHANNELKEY = "475";
    public static final String ERR_NOPRIVILEGES = "481";
    public static final String ERR_CHANOPRIVSNEEDED = "482";
    public static final String ERR_CANTKILLSERVER = "483";
    public static final String ERR_NOOPERHOST = "491";
    
    private IrcCommand() {
        // 私有构造函数，防止实例化
    }
} 