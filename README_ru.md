# Onec Gradle Plugin

[English](README.md) | Русский

Gradle-плагин для разработки на 1С:Предприятии. Собирает конфигурации, расширения, внешние
обработки и отчёты.

## Требования

- Gradle 9.0 или новее.
- Java 17 или новее.
- Платформа 1С:Предприятие с `ibcmd`.

## Начало работы

Шаги ниже создают проект, который собирает конфигурацию.

1. [Установите Gradle](https://gradle.org/install/) 9.0 или новее.

2. Создайте пустой каталог для проекта, например `my-project`, и выполните в нём:

   ```
   gradle init --type basic --dsl kotlin --use-defaults
   ```

   Команда создаёт проект Gradle
   ([документация по `gradle init`](https://docs.gradle.org/current/userguide/build_init_plugin.html)).
3. Выгрузите конфигурацию в XML-файлы в каталог `src`.
   `Configuration.xml` должен оказаться прямо в `src`:

   ```
   my-project/
   ├─ settings.gradle.kts
   ├─ build.gradle.kts
   └─ src/
      ├─ Configuration.xml
      └─ ...
   ```

4. Замените содержимое `build.gradle.kts` на:

   ```kotlin
   plugins {
       id("io.github.onec-devops.onec") version "0.1.0"
   }

   onec {
       platformVersion = "8.3.25"   // версия установленной платформы или её начало
   }
   ```

5. Соберите проект:

   ```
   ./gradlew build
   ```

   В Windows выполните `gradlew build` в командной строке или `.\gradlew build` в PowerShell.
   Получится `build/onec/my-project.cf`.

## Настройка

Плагин настраивается в блоке `onec` в `build.gradle.kts`:

```kotlin
onec {
    platformVersion = "8.3.25"   // обязательно: полная версия или её начало
    srcDir = file("src")         // необязательно: по умолчанию src
}
```

### Результат сборки

Проект собирает один артефакт из выгрузки в `srcDir`. Что именно, плагин определяет по файлам в
этом каталоге:

| В `srcDir` лежит | Результат |
|---|---|
| `Configuration.xml` конфигурации | `.cf` |
| `Configuration.xml` расширения | `.cfe` |
| `<Имя>.xml` внешней обработки и каталог `<Имя>` | `.epf` |
| `<Имя>.xml` внешнего отчёта и каталог `<Имя>` | `.erf` |

> [!TIP]
> Для единообразия внешнюю обработку рекомендуется называть `DataProcessor`, а внешний отчёт —
> `Report`. Тогда в `srcDir` всегда лежит файл с одним и тем же именем: `DataProcessor.xml`,
> `Report.xml` или `Configuration.xml`.

Артефакт называется по имени проекта: `build/onec/<имя проекта>.cf`. Если нужно другое имя, его
можно задать в `build.gradle.kts`:

```kotlin
base {
    archivesName = "accounting"
}
```

### Поиск платформы

`platformVersion` — полная версия платформы или её начало. Под `8.3.25` подходит любая установка
8.3.25.x, и плагин берёт самую новую из них.

Плагин сам находит платформу в стандартных каталогах: `%ProgramFiles%\1cv8` и
`%ProgramFiles(x86)%\1cv8` в Windows, `/opt/1cv8/*` в Linux. Если она установлена в другом месте,
путь к ней указывается в Gradle-свойствах, например в `~/.gradle/gradle.properties`, или через
переменную окружения:

| Свойство | По умолчанию | Описание | Пример |
|---|---|---|---|
| `onec.installations.paths` | | Пути к установкам или к каталогам с установками разных версий, через запятую. | `D:/1cv8,E:/1C/8.3.25.1633` |
| `onec.installations.fromEnv` | `ONEC_HOME` | Имена переменных окружения с такими путями, через запятую. | `ONEC_HOME,V8_HOME` |

`./gradlew onecInstallations` показывает найденные установки и отмечает используемую:

```
Requested: 8.3.25

* 8.3.25.1633   C:\Program Files\1cv8\8.3.25.1633   standard location
```

## Многопроектная сборка

Проект собирает один артефакт, поэтому конфигурация, расширение к ней и внешняя обработка — это три
подпроекта одной сборки. Каталог `my-project` создаётся той же командой
`gradle init --type basic --dsl kotlin --use-defaults`. Он объединяет подпроекты и сам ничего не
собирает, а подпроекты — обычные каталоги в нём:

```
my-project/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ configuration/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ Configuration.xml
│     └─ ...
├─ extension/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ Configuration.xml
│     └─ ...
└─ data-processor/
   ├─ build.gradle.kts
   └─ src/
      ├─ DataProcessor.xml
      └─ DataProcessor/
```

`settings.gradle.kts` подключает подпроекты:

```kotlin
rootProject.name = "my-project"

include("configuration", "extension", "data-processor")
```

`build.gradle.kts` в `my-project` объявляет плагин с версией, но не применяет его (`apply false`):

```kotlin
plugins {
    id("io.github.onec-devops.onec") version "0.1.0" apply false
}
```

Каждый подпроект применяет плагин в своём `build.gradle.kts`, уже без версии:

```kotlin
plugins {
    id("io.github.onec-devops.onec")
}

onec {
    platformVersion = "8.3.25"
}
```

> [!NOTE]
> Когда подпроектов много, общие настройки, например `platformVersion`, обычно выносят в
> плагин-соглашение (convention plugin), см.
> [рекомендации Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html).

`./gradlew build` собирает все три:

```
configuration/build/onec/configuration.cf
extension/build/onec/extension.cfe
data-processor/build/onec/data-processor.epf
```

## Задачи

| Задача | Описание |
|---|---|
| `onecBuild` | Собирает артефакт проекта в `build/onec`. |
| `onecCreateInfobase` | Создаёт в `build/onec/infobase` пустую информационную базу, которая нужна `ibcmd`. |
| `onecInstallations` | Показывает найденные установки платформы. |

`assemble`, а значит и `build`, зависят от `onecBuild`. Она пропускается, если исходники не
менялись, а с включённым build cache результат берётся из кеша. Плагин работает
с configuration cache.

## Участие в разработке

Помочь проекту можно pull request'ом: исправлением ошибки, новой функциональностью или улучшением
документации. Крупное изменение лучше сначала обсудить в issue. Как собрать плагин и запустить тесты, рассказано
в [CONTRIBUTING.md](CONTRIBUTING.md).

## Лицензия

Плагин распространяется по лицензии [Apache License 2.0](LICENSE).
