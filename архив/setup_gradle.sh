#!/bin/bash

# Настройка Gradle Wrapper

set -e

echo "=== Настройка Gradle Wrapper ==="

WRAPPER_DIR="gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "📥 Скачивание gradle-wrapper.jar..."
    mkdir -p "$WRAPPER_DIR"
    
    # Скачивание через curl
    curl -L -o "$WRAPPER_JAR" \
        "https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar" \
        2>/dev/null || \
    curl -L -o "$WRAPPER_JAR" \
        "https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar" \
        2>/dev/null || {
        echo "❌ Не удалось скачать gradle-wrapper.jar"
        echo "Скачайте вручную:"
        echo "https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar"
        echo "И поместите в: $WRAPPER_JAR"
        exit 1
    }
    
    echo "✅ Gradle Wrapper настроен"
else
    echo "✅ Gradle Wrapper уже настроен"
fi

chmod +x gradlew
echo "✅ Готово к сборке: ./gradlew assembleDebug"

