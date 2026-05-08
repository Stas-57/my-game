# Snake Game — Android

## Требования
- Android Studio Hedgehog или новее
- Android SDK API 29+ (Android 10)
- Java 11

## Структура проекта
```
SnakeGame/
├── app/
│   ├── src/main/
│   │   ├── java/com/snake/game/
│   │   │   ├── MainActivity.java      ← Точка входа, обработка свайпов
│   │   │   └── SnakeGameView.java     ← Вся игровая логика и отрисовка
│   │   ├── res/values/
│   │   │   └── styles.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Сборка в Android Studio
1. Открой Android Studio → File → Open → выбери папку SnakeGame
2. Дождись синхронизации Gradle
3. Нажми Run ▶ или Build → Generate Signed APK

## Функции игры
- 🐍 Плавная змейка с анимированными глазами
- 🍎 Обычная еда (+10 × уровень очков)
- ⭐ Бонусная еда (+50 × уровень, появляется на 10 сек)
- 📈 10 уровней сложности (скорость растёт)
- 🏆 Рекорд сохраняется в сессии
- 👆 Управление свайпами, пауза кнопкой
- 🎨 Неоновый дизайн, тёмная тема

## Управление
- Свайп вверх/вниз/влево/вправо — поворот
- Тап по экрану — старт / рестарт
- Кнопка паузы (⏸/▶) — пауза/продолжить
