# Cortex Project Structure

```
.
├── app
│   ├── build.gradle.kts
│   ├── src
│   │   └── main
│   │       ├── AndroidManifest.xml
│   │       ├── cpp
│   │       │   ├── CMakeLists.txt
│   │       │   └── terminal-jni.cpp
│   │       └── java
│   │           └── io
│   │               └── cortex
│   │                   └── terminal
│   │                       ├── CortexApplication.kt
│   │                       ├── data
│   │                       │   ├── api
│   │                       │   │   └── AIService.kt
│   │                       │   ├── model
│   │                       │   │   └── AIModels.kt
│   │                       │   └── repository
│   │                       │       └── AIRepository.kt
│   │                       ├── di
│   │                       │   └── AppModule.kt
│   │                       ├── domain
│   │                       │   └── usecase
│   │                       │       └── GetCommandSuggestionUseCase.kt
│   │                       ├── engine
│   │                       │   └── TerminalSession.kt
│   │                       ├── presentation
│   │                       │   ├── MainActivity.kt
│   │                       │   ├── ai
│   │                       │   │   └── AIViewModel.kt
│   │                       │   └── terminal
│   │                       │       ├── TerminalScreen.kt
│   │                       │       └── TerminalViewModel.kt
│   │                       └── ui
│   │                           └── theme
│   │                               ├── Color.kt
│   │                               ├── Theme.kt
│   │                               └── Type.kt
├── build.gradle.kts
└── settings.gradle.kts
```
