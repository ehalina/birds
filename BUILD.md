# Инструкция по сборке APK

## Требования
- Android SDK
- Gradle 8.2+
- JDK 8+

## Сборка через Android Studio
1. Откройте проект в Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)
3. APK будет в `app/build/outputs/apk/debug/app-debug.apk`

## Сборка через командную строку
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Установка на устройство
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```


