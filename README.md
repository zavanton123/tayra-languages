# Tayra Languages

Learn languages by reading. A Kotlin Multiplatform port of Lute (Learning Using Texts)
for Android, iOS, desktop (JVM) and web (Wasm), with a shared Compose Multiplatform UI.

## Modules

- `shared` – app composition: navigation graph, DI wiring, iOS framework
- `androidApp`, `desktopApp`, `webApp`, `iosApp` – thin platform launchers

## Building

```
./gradlew :desktopApp:run                      # desktop
./gradlew :androidApp:installDebug             # android
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # web
open iosApp/iosApp.xcodeproj                   # ios
```
