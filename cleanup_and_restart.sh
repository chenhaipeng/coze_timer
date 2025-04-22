#!/bin/bash

echo "停止并删除所有容器..."
docker compose down

echo "清理 MySQL 数据..."
# 我们不会删除整个 MySQL 数据目录，而是删除之前的任务数据
if [ -d "./docker/mysql/data" ]; then
  # 备份原有数据以防万一
  echo "备份原有 MySQL 数据..."
  mv ./docker/mysql/data ./docker/mysql/data_backup_$(date +%Y%m%d%H%M%S)
fi

echo "使用时区修复后的配置启动所有容器..."
docker compose up -d

echo "等待容器完全启动..."
sleep 10

echo "检查容器内部时间..."
docker exec coze_timer_app date

echo "清理完成，系统已重启，现在您可以使用正确的时区设置测试定时任务。" 