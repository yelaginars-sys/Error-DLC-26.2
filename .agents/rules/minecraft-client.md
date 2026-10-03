# Minecraft Client (Error DLC 26.2) Guidelines & Rules

## Project Environment & Build Commands
- **JDK Location**: `C:\Users\yelag\.jdks\ms-25.0.4.1`
- **Gradle Compilation Command**:
  ```powershell
  $env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; .\gradlew.bat compileJava --console=plain
  ```
- **Run Client Command**:
  ```powershell
  $env:JAVA_HOME = "C:\Users\yelag\.jdks\ms-25.0.4.1"; .\gradlew.bat runClient
  ```

## UI & Rendering Conventions (`Render2D` & `Fonts`)
1. **Rendering Scope**: Always wrap custom 2D rendering blocks inside `RenderExtend.enter2D(null, extractor, null)` and `RenderExtend.exit2D()`.
2. **Frame Batching**: Call `Render2DUtil.beginFrame()` before drawing 2D primitives and `Render2DUtil.flush()` before exiting the 2D scope.
3. **Colors**: Always use `ColorUtil.rgba(...)`, `ColorUtil.applyAlpha(...)`, or `ColorUtil.withAlpha(...)`. Use `Theme.getAccentColor()` for active accenting.
4. **Icons**: Use `Fonts.drawIcon(IconUse.GLYPH, x, y, size, color)` using the `IconUse` enum.
5. **Fonts**: Use `Fonts.SF_MEDIUM` for all UI text drawing.
6. **Scissor Clipping**: Use `Render2D.pushScissor(x, y, w, h)` and `Render2D.popScissor()` for scrollable lists.

## Mixins & Vanilla Safety
1. **CubeMap / Panorama Safety**: Vanilla `CubeMap.render` should be guarded against uninitialized texture states during startup recovery reloads.
2. **TitleScreen Mixin**: Screen overrides should cleanly redirect `TitleScreen.init` without interfering with low-level pipeline initialization.
3. **Deprecations & Contracts**: Retain existing signatures and backward-compatibility methods (e.g. `CustomTitleScreen.loadWallpaper()`).
