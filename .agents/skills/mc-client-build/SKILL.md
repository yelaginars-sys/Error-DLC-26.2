---
name: mc-client-build
description: >-
  Specialized skill for compiling, running, diagnosing build errors, and verifying changes in the Error DLC 26.2 Minecraft client repository. Use when building the project, running gradle tasks, or debugging compilation failures.
---

# Minecraft Client Build & Verification Skill (`mc-client-build`)

This skill contains procedures for building, running, and diagnosing issues in the **Error DLC 26.2** Fabric client.

## Standard Build Commands

### 1. Compile Java Source Code
```powershell
$env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; .\gradlew.bat compileJava --console=plain
```

### 2. Launch Client
```powershell
$env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; .\gradlew.bat runClient
```

### 3. Stop Hanging Gradle Daemons
If Gradle locks or hangs on cache files:
```powershell
$env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; .\gradlew.bat --stop
```

## Troubleshooting Build Errors

1. **Missing Method / Symbol Errors**:
   - Inspect decompiled Fabric/Minecraft JAR using `javap`:
     ```powershell
     $env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; & "C:\Users\yelag\.jdks\ms-25.0.4.1\bin\javap.exe" -cp "C:\Users\yelag\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar" -p "target.package.ClassName"
     ```

2. **Crash Log Inspection**:
   - Latest crash reports are generated in `run/crash-reports/crash-YYYY-MM-DD_HH.MM.SS-client.txt`.
   - Inspect `Description:` and stacktrace to identify failing Mixins or render passes.
