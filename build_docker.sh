#!/bin/bash 

# Сборка APK через Docker (не требует установки Android SDK) - нет

set -e

echo "=== Сборка APK через Docker ==="

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Установите Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Создание Dockerfile для сборки
cat > Dockerfile.build << 'EOF'
FROM gradle:8.2-jdk17

WORKDIR /app

# Копирование файлов проекта
COPY . .

# Настройка Android SDK (установка через sdkmanager в контейнере)
RUN apt-get update && apt-get install -y wget unzip && \
    mkdir -p /opt/android-sdk/cmdline-tools && \
    cd /opt/android-sdk/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip -q commandlinetools-linux-9477386_latest.zip && \
    mv cmdline-tools latest && \
    rm commandlinetools-linux-9477386_latest.zip && \
    yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses && \
    /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

# Сборка APK
RUN chmod +x gradlew && ./gradlew assembleDebug

# Копирование APK в корень
RUN cp app/build/outputs/apk/debug/app-debug.apk /app/motion-recorder.apk
EOF

echo "🐳 Сборка Docker образа..."
docker build -f Dockerfile.build -t motion-recorder-build .

echo "📦 Извлечение APK..."
docker create --name temp-container motion-recorder-build
docker cp temp-container:/app/motion-recorder.apk ./motion-recorder.apk
docker rm temp-container

rm Dockerfile.build

if [ -f "motion-recorder.apk" ]; then
    echo "✅ APK собран: motion-recorder.apk"
else
    echo "❌ Ошибка сборки"
    exit 1
fi

