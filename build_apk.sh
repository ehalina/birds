#!/bin/bash

# Скрипт для сборки APK без Android Studio

set -e

echo "=== Сборка APK Motion Recorder ==="

# Проверка Java
if ! command -v java &> /dev/null; then
    echo "❌ Java не установлена. Установите JDK 8+"
    exit 1
fi

# Установка Android SDK через Homebrew (macOS)
if [ -z "$ANDROID_HOME" ]; then
    echo "📦 Установка Android SDK..."
    
    if command -v brew &> /dev/null; then
        echo "Установка через Homebrew..."
        brew install --cask android-commandlinetools
        
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
        
        # Создание директорий
        mkdir -p "$ANDROID_HOME/cmdline-tools"
        mkdir -p "$ANDROID_HOME/licenses"
        
        echo "y" | sdkmanager --licenses || true
        
        # Установка необходимых компонентов
        sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
    else
        echo "❌ Homebrew не установлен. Установите Android SDK вручную:"
        echo "1. Скачайте: https://developer.android.com/studio#command-tools"
        echo "2. Распакуйте в ~/Library/Android/sdk"
        echo "3. Установите ANDROID_HOME в ~/.zshrc"
        exit 1
    fi
fi

# Установка Gradle
if ! command -v gradle &> /dev/null; then
    echo "📦 Установка Gradle..."
    if command -v brew &> /dev/null; then
        brew install gradle
    else
        echo "Установите Gradle: https://gradle.org/install/"
        exit 1
    fi
fi

# Сборка APK
echo "🔨 Сборка APK..."
cd "$(dirname "$0")"
./gradlew assembleDebug

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ APK собран: app/build/outputs/apk/debug/app-debug.apk"
    echo "📱 Установка на устройство: adb install app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Ошибка сборки"
    exit 1
fi

