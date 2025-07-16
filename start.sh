#!/bin/bash

# IRC4Spring 服务器启动脚本

# 设置Java环境
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}
export PATH=$JAVA_HOME/bin:$PATH

# 检查Java版本
java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
echo "使用Java版本: $java_version"

if [[ ! "$java_version" =~ ^21\. ]]; then
    echo "错误: 需要Java 21或更高版本"
    echo "请设置JAVA_HOME环境变量指向Java 21安装目录"
    exit 1
fi

# 设置JVM参数
JVM_OPTS="-Xmx1g -Xms512m"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS --enable-preview"

# 设置虚拟线程相关参数
JVM_OPTS="$JVM_OPTS -Djdk.virtualThreadScheduler.parallelism=100"
JVM_OPTS="$JVM_OPTS -Djdk.virtualThreadScheduler.maxPoolSize=256"

# 设置日志
JVM_OPTS="$JVM_OPTS -Dlogging.config=classpath:logback-spring.xml"

# 应用程序参数
APP_OPTS=""

# 检查配置文件
if [ ! -f "application.yml" ] && [ ! -f "src/main/resources/application.yml" ]; then
    echo "警告: 未找到application.yml配置文件"
fi

# 构建项目
echo "正在构建项目..."
if ! mvn clean package -DskipTests; then
    echo "错误: 项目构建失败"
    exit 1
fi

# 查找JAR文件
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -n1)

if [ -z "$JAR_FILE" ]; then
    echo "错误: 未找到JAR文件"
    exit 1
fi

echo "找到JAR文件: $JAR_FILE"

# 启动服务器
echo "正在启动IRC4Spring服务器..."
echo "JVM参数: $JVM_OPTS"
echo "应用参数: $APP_OPTS"
echo ""

# 创建日志目录
mkdir -p logs

# 启动应用
exec java $JVM_OPTS -jar "$JAR_FILE" $APP_OPTS 