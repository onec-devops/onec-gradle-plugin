# Onec Gradle Plugin

[English](README.md) | **Русский**

[![Сборка](https://github.com/onec-devops/onec-gradle-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/onec-devops/onec-gradle-plugin/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.onec-devops.onec?logo=gradle&logoColor=4DC9C0&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.onec-devops.onec)
[![Документация](https://img.shields.io/badge/Документация-grey?logo=docusaurus)](https://onec-devops.github.io/onec-gradle-plugin/ru/)

Gradle-плагин для проектов на платформе 1С:Предприятие. Он автоматизирует процессы разработки и
позволяет применять в проектах лучшие инженерные практики.

## Возможности

- **Любой формат и тип проекта**. Плагин собирает конфигурации, расширения, внешние обработки и
  отчёты из XML-выгрузки и из проекта 1С:EDT и сам определяет, что перед ним.
- **Многопроектная сборка**. Конфигурация, её расширения, обработки и отчёты могут храниться в
  одном репозитории и собираться как одно решение.
- **Кастомизация и расширяемость**. Можно изменить настройки плагина и добавить свои задачи,
  например проверку кода с BSL Language Server.
- **Интеграция в экосистему Gradle**. Проект на 1С совместим с плагинами сообщества Gradle, а также
  с IDE и CI, которые поддерживают Gradle.

## Начало работы

1. Установите Java 17 или новее и последнюю версию Gradle по
   [инструкции Gradle](https://gradle.org/install/).
2. Если у проекта ещё нет сборки Gradle, создайте её в каталоге проекта. Команда создаёт скрипты
   сборки и Gradle Wrapper:

   ```bash
   gradle init --type basic --dsl kotlin --use-defaults
   ```

3. Поместите XML-выгрузку или проект 1С:EDT в каталог `src`:

   ```text
   my-project/
   ├─ gradlew
   ├─ settings.gradle.kts
   ├─ build.gradle.kts
   └─ src/
      ├─ Configuration.xml   # XML-выгрузка
      └─ ...
   ```

   Для проекта 1С:EDT в `src` окажутся `.project`, `DT-INF/` и `src/` проекта.

4. Примените плагин в `build.gradle.kts`:

   ```kotlin
   plugins {
       id("io.github.onec-devops.onec") version "0.2.0"
   }

   onec {
       platformVersion = "8.3.25"  // версия установленной платформы или её начало
       edtVersion = "2026.2"       // только для проекта 1С:EDT
       srcDir = file("src")        // каталог исходного кода, по умолчанию src
   }
   ```

5. Соберите проект командой `./gradlew build`. Результат появится в `build/onec/`.

Подробнее о плагине - в
[документации](https://onec-devops.github.io/onec-gradle-plugin/ru/).

## Участие в разработке

Помочь проекту можно pull request'ом: исправлением ошибки, новой функциональностью или улучшением
документации. Крупное изменение лучше сначала обсудить в issue. Как собрать плагин и запустить
тесты, рассказано в [CONTRIBUTING.md](CONTRIBUTING.md).

## Лицензия

Плагин распространяется по лицензии [Apache License 2.0](LICENSE).
