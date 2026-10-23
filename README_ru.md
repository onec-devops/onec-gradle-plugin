# Onec Gradle Plugin

[English](README.md) | Русский

Gradle-плагин для разработки на 1С:Предприятии. Собирает конфигурации, расширения, внешние
обработки и отчёты из XML-выгрузок и проектов 1С:EDT.

## Требования

- Gradle 9.0 или новее.
- Java 17 или новее.
- Платформа 1С:Предприятие с `ibcmd`.
- 1С:EDT, только для сборки проектов 1С:EDT.

## Начало работы

Шаги ниже создают проект, который собирает конфигурацию.

1. [Установите Gradle](https://gradle.org/install/) 9.0 или новее.

2. Создайте пустой каталог для проекта, например `my-project`, и выполните в нём:

   ```
   gradle init --type basic --dsl kotlin --use-defaults
   ```

   Команда создаёт проект Gradle
   ([документация по `gradle init`](https://docs.gradle.org/current/userguide/build_init_plugin.html)).
3. Выгрузите конфигурацию в XML-файлы в каталог `src` так, чтобы `Configuration.xml` оказался
   прямо в нём. Если вы работаете в 1С:EDT, скопируйте в `src` проект целиком, см.
   [Проекты 1С:EDT](#проекты-1сedt).

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
       id("io.github.onec-devops.onec") version "0.2.0"
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
    edtVersion = "2026.1"        // только для проектов 1С:EDT: полная версия или её начало
    srcDir = file("src")         // необязательно: по умолчанию src
}
```

### Результат сборки

Проект собирает один артефакт из исходников в `srcDir`: XML-выгрузки или проекта 1С:EDT. Что
именно, плагин определяет по файлам в этом каталоге:

| Что собирается | Результат | XML-выгрузка | Проект 1С:EDT |
|---|---|---|---|
| Конфигурация | `.cf` | `Configuration.xml` конфигурации | Проект конфигурации |
| Расширение | `.cfe` | `Configuration.xml` расширения | Проект расширения |
| Внешняя обработка | `.epf` | `<Имя>.xml` и каталог `<Имя>` | Проект внешних отчетов и обработок с одной обработкой |
| Внешний отчёт | `.erf` | `<Имя>.xml` и каталог `<Имя>` | Проект внешних отчетов и обработок с одним отчётом |

> [!TIP]
> В XML-выгрузке внешнюю обработку рекомендуется называть `DataProcessor`, а внешний отчёт —
> `Report`. Тогда в `srcDir` любого такого проекта будет один из трёх файлов: `DataProcessor.xml`,
> `Report.xml` или `Configuration.xml`.

Артефакт называется по имени проекта: `build/onec/<имя проекта>.cf`. Если нужно другое имя, его
можно задать в `build.gradle.kts`:

```kotlin
base {
    archivesName = "accounting"
}
```

### Проекты 1С:EDT

В качестве `srcDir` используется сам каталог проекта 1С:EDT, поэтому `.project` и `DT-INF/`
находятся прямо в `src`:

```
my-project/
├─ settings.gradle.kts
├─ build.gradle.kts
└─ src/
   ├─ .project
   ├─ DT-INF/
   └─ src/
```

Для сборки проекта 1С:EDT нужна установленная 1С:EDT той версии, что указана в `edtVersion`.
Сборка запускает 1С:EDT, и даже на маленьком проекте это занимает около минуты.

### Поиск платформы и 1С:EDT

`platformVersion` — полная версия платформы или её начало. Под `8.3.25` подходит любая установка
8.3.25.x, и плагин берёт самую новую из них. С `edtVersion` так же: под `2026.1` подходит любая
2026.1.x.

Плагин сам находит их в стандартных каталогах:

| | Windows | Linux |
|---|---|---|
| Платформа | `%ProgramFiles%\1cv8`, `%ProgramFiles(x86)%\1cv8` | `/opt/1cv8/*` |
| 1С:EDT | `%LOCALAPPDATA%\1C\1cedtstart\installations` (1С:EDT Start), `%ProgramFiles%\1C\1CE\components` (установщик 1С:Предприятия) | `/opt/1C/1CE/components` |

Если они установлены в другом месте, путь указывается в Gradle-свойствах, например в
`~/.gradle/gradle.properties`, или через переменную окружения:

| Свойство | По умолчанию | Описание | Пример |
|---|---|---|---|
| `onec.installations.paths` | | Пути к установкам платформы или к каталогам с установками разных версий, через запятую. | `D:/1cv8,E:/1C/8.3.25.1633` |
| `onec.installations.fromEnv` | `ONEC_HOME` | Имена переменных окружения с такими путями, через запятую. | `ONEC_HOME,V8_HOME` |
| `onec.edt.installations.paths` | | То же для 1С:EDT. | `D:/1C/EDT` |
| `onec.edt.installations.fromEnv` | `EDT_HOME` | То же для 1С:EDT. | `EDT_HOME,EDT_2026` |

`./gradlew onecInstallations` показывает найденные установки и отмечает используемые:

```
1C:Enterprise platform, requested: 8.3.25

* 8.3.25.1633   C:\Program Files\1cv8\8.3.25.1633   standard location

1C:EDT, requested: 2026.1

* 2026.1.3   C:\Users\me\AppData\Local\1C\1cedtstart\installations\1C_EDT 2026.1\1cedt   standard location
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
    id("io.github.onec-devops.onec") version "0.2.0" apply false
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
| `onecInstallations` | Показывает найденные установки платформы и 1С:EDT. |

`assemble`, а значит и `build`, зависят от `onecBuild`. Она пропускается, если исходники не
менялись, а с включённым build cache результат берётся из кеша. Плагин работает
с configuration cache.

## Участие в разработке

Помочь проекту можно pull request'ом: исправлением ошибки, новой функциональностью или улучшением
документации. Крупное изменение лучше сначала обсудить в issue. Как собрать плагин и запустить тесты, рассказано
в [CONTRIBUTING.md](CONTRIBUTING.md).

## Лицензия

Плагин распространяется по лицензии [Apache License 2.0](LICENSE).
