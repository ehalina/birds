# Быстрая сборка APK без Android Studio

## Вариант 1: Docker (рекомендуется, самый простой)

```bash
./build_docker.sh
```

Требуется только Docker Desktop. APK будет в `motion-recorder.apk`

**Преимущества:** Не требует установки Android SDK, Java, Gradle - всё в контейнере.

## Вариант 2: Через скрипт (требует установки SDK)

```bash
./setup_gradle.sh  # Настройка Gradle Wrapper
./build_apk.sh     # Сборка APK
```

Скрипт автоматически установит Android SDK и Gradle через Homebrew.

## Вариант 3: Ручная установка Android SDK

1. Установите JDK 8+:
```bash
brew install openjdk@17
```

2. Установите Android Command Line Tools:
```bash
brew install --cask android-commandlinetools
```

3. Настройте переменные окружения в `~/.zshrc`:
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

4. Установите компоненты SDK:
```bash
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "y" | sdkmanager --licenses
```

5. Установите Gradle:
```bash
brew install gradle
```

6. Настройте Gradle Wrapper:
```bash
./setup_gradle.sh
```

7. Соберите APK:
```bash
./gradlew assembleDebug
```

## Вариант 4: Онлайн сборка (GitHub Actions)

1. Создайте репозиторий на GitHub
2. Загрузите код
3. GitHub Actions соберет APK автоматически

## Вариант 5: Использовать готовый APK builder

Можно использовать онлайн сервисы типа:
- https://www.bitrise.io/
- https://appcircle.io/

Но проще всего - Docker вариант.

