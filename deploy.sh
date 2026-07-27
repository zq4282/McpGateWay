#!/usr/bin/env bash

# =======================================================
# Dynamic MCP Gateway 一键 Docker 构建与部署脚本
# =======================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=======================================================${NC}"
echo -e "${BLUE}        🚀 Dynamic MCP Gateway 一键部署工具             ${NC}"
echo -e "${BLUE}=======================================================${NC}"

# 1. 检查 Docker 环境
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ 错误: 未检测到 Docker 环境，请先安装 Docker！${NC}"
    exit 1
fi

# 2. 检查 Docker Compose
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
else
    echo -e "${RED}❌ 错误: 未检测到 docker compose 命令！${NC}"
    exit 1
fi

# 3. 确保本地 sqlite 数据库挂载文件存在（防目录误建）
if [ ! -f "mcp-gateway.db" ]; then
    echo -e "${YELLOW}⚠️ 本地 mcp-gateway.db 不存在，创建空白数据库空文件...${NC}"
    touch mcp-gateway.db
fi

# 4. 构建并启动容器
echo -e "${BLUE}📦 正在构建并启动 Docker 镜像容器...${NC}"
$DOCKER_COMPOSE_CMD down --remove-orphans || true
$DOCKER_COMPOSE_CMD up --build -d

echo -e "${BLUE}⏳ 正在等待容器服务初始化并进行健康检查...${NC}"
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f http://localhost:8080/admin/api-keys > /dev/null 2>&1; then
        echo -e "${GREEN}=======================================================${NC}"
        echo -e "${GREEN}🎉 部署成功！Dynamic MCP Gateway 已成功在线运行！     ${NC}"
        echo -e "${GREEN}=======================================================${NC}"
        echo -e "${GREEN}🌐 嵌入式 Web 控制台 : http://localhost:8080/${NC}"
        echo -e "${GREEN}🔗 MCP Streamable 端点 : http://localhost:8080/mcp${NC}"
        echo -e "${GREEN}=======================================================${NC}"
        exit 0
    fi
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT+1))
    echo -n "."
done

echo -e "\n${RED}❌ 服务健康检查超时，请执行 '$DOCKER_COMPOSE_CMD logs -f' 查看错误日志！${NC}"
exit 1
