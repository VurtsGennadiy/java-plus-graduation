#!/bin/bash

# Отключаем конвертацию путей в Git Bash → иначе /bin/kafka... превратится в C:/...
export MSYS_NO_PATHCONV=1

CONTAINER_NAME="kafka"
TOPIC="stats.user-actions.v1"
BOOTSTRAP_SERVER="localhost:9092"

echo "🚀 Запускаем Kafka consumer для топика: $TOPIC"
echo "Контейнер: $CONTAINER_NAME"
echo "Брокер: $BOOTSTRAP_SERVER"
echo "--------------------------------------------------"

docker exec -it "$CONTAINER_NAME" \
  /bin/kafka-console-consumer \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic "$TOPIC" \
