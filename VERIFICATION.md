# UniFlow — verification

Verified on 2026-09-30. These checks were executed on the supplied source, not inferred from its structure.

## Environment

- Windows, JDK 21.0.11, Gradle wrapper 8.13, Android Gradle Plugin 8.13.2.
- Android SDK 36, Build Tools 35.0.0; minSdk 26, targetSdk 36.
- Pixel_10a emulator, Android 17 / API 37, software GPU.

## Actual commands and results

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest assembleRelease --continue
```

The final run used the already populated Gradle cache (`--offline`) and completed with **BUILD SUCCESSFUL**.

| Check | Result |
|---|---|
| Debug app, including resource merging and Room/KSP generation | PASS |
| Unit tests | 5 passed, 0 failed |
| Instrumented Android tests | 6 passed, 0 failed |
| Android Lint | 0 errors; 12 non-blocking version/target warnings |
| Release build, R8 optimization and vital release lint | PASS; unsigned APK generated |
| Localization resources | Complete matching English, Kazakh and Russian keys; checked while packaging |
| ZIP integrity and exclusions | Checked by packaging script |

## Tested behavior

- Weighted averages normalize different grade scales; GPA weights course credits and ignores ungraded courses.
- Grade scale boundaries, empty analytics, invalid scores, missing course links and invalid class times.
- Room insertion/update/deletion and cascading deletion of linked records.
- Database close/reopen preserves a course and linked note.
- Compose search returns a matching course.
- A task created through the UI is stored in Room and survives activity recreation.
- Navigation through calendar, grades, attendance, notes, teachers and settings.
- English/light, Russian/dark and Kazakh/dark rendering; screenshots taken from the emulator.
- A real deadline notification is posted; the same record/date does not notify again after a title edit.

## Relevant fixes discovered during verification

- Corrected a Compose Surface overload that prevented compilation.
- Upgraded AndroidX Test/Espresso to remove the obsolete reflective InputManager call on Android 17.
- Prevented navigation actions before the first data load and NavHost initialization.
- Made bottom tabs open their overview rather than restoring a nested screen that trapped the More tab.
- Bundled all three languages in app bundles; configured explicit data extraction exclusions.
- Updated status/navigation bar icon contrast when the in-app theme changes.
- Explicitly provided text colors for translucent surfaces; screenshot verification and a pixel contrast assertion guard dark-theme readability.

Lint's remaining warnings concern deliberately pinned dependency/tool versions and targeting API 36 instead of the newest API. There are no remaining localization, permissions, backup or resource errors. The build versions are retained as a tested combination.

## Limits of verification

The app was exercised on one emulator; physical devices, OEM battery policies and all older Android API levels were not tested. Release code was compiled and optimized, but the unsigned release APK was not installed. Reminder notification delivery was tested directly through the worker; long real-time scheduling under Doze was not waited out. WorkManager delivery is approximate, as explained in the README. No cloud/backend behavior is claimed.

## Русский

Debug и release собраны успешно. Пройдено 5 unit-тестов и 6 тестов на эмуляторе Android 17, ошибок Android Lint нет. Проверены Room CRUD, каскадное удаление, сохранение после повторного открытия, создание задания через интерфейс, поиск, навигация, три языка, светлая/тёмная темы и уведомление без повторной отправки. Физические устройства и точность длительных фоновых напоминаний не проверялись.

## Қазақша

Debug және release сәтті жиналды. 5 unit-тест және Android 17 эмуляторындағы 6 тест өтті; Android Lint қатесі жоқ. Room CRUD, каскадты жою, қайта ашқанда деректердің сақталуы, интерфейс арқылы тапсырма жасау, іздеу, навигация, үш тіл, ашық/қараңғы тақырып және қайталанбайтын хабарлама тексерілді. Нақты құрылғылар мен ұзақ фондық жоспарлаудың дәлдігі тексерілмеді.
