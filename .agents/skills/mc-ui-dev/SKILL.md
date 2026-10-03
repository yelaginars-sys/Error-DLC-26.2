---
name: mc-ui-dev
description: >-
  Specialized skill for creating, editing, and troubleshooting custom 2D screens, HUD elements, module cards, and modal dialogs in the Error DLC Minecraft client. Use when designing new UI components or modifying existing UI screens like CustomTitleScreen or ClickGUI.
---

# Minecraft Client UI Development Skill (`mc-ui-dev`)

This skill provides step-by-step instructions and technical patterns for building pixel-perfect UI screens and widgets in the **Error DLC 26.2** client.

## Core Render Pipeline Setup

Every custom 2D screen extending `net.minecraft.client.gui.screens.Screen` must follow this rendering structure in `extractRenderState`:

```java
@Override
public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
    int screenWidth = this.width > 0 ? this.width : 854;
    int screenHeight = this.height > 0 ? this.height : 480;

    RenderExtend.enter2D(null, extractor, null);
    try {
        Render2DUtil.beginFrame();

        // 1. Background / Atmosphere
        // 2. Containers / Cards / Glass Panels
        // 3. Text & Icons
        // 4. Floating Modals / Tooltips

        Render2DUtil.flush();
    } finally {
        RenderExtend.exit2D();
    }
}
```

## Primitive UI Rendering API (`Render2D`)

- **Rounded Rectangle**: `Render2D.drawRoundedRect(x, y, w, h, radius, color)`
- **Rounded Outline**: `Render2D.drawRoundedOutline(x, y, w, h, radius, thickness, borderColor)`
- **Drop Shadows**: `Render2D.drawShadow(x, y, w, h, radius, blur, shadowColor)`
- **Gradients**: `Render2D.drawGradientRound(x, y, w, h, radius, cTopLeft, cTopRight, cBottomRight, cBottomLeft)`
- **Textured Box**: `Render2D.drawTexture(textureIdentifier, x, y, w, h, radius, color)`
- **Text & Centered Text**:
  - `Fonts.drawString(Fonts.SF_MEDIUM, text, x, y, size, color)`
  - `Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, centerX, y, size, color)`
- **Icons**: `Fonts.drawIcon(IconUse.GLYPH, x, y, size, color)`

## Smooth Hover Animations & Interpolation

Use `Mth.clamp` for smooth frame-independent transitions:

```java
boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
animProgress = Mth.clamp(animProgress + (hovered ? 0.12F : -0.12F), 0.0F, 1.0F);

int cardBorder = ColorUtil.lerp(
    ColorUtil.rgba(255, 255, 255, 30),
    ColorUtil.rgba(90, 150, 255, 140),
    animProgress
);
```

## Modals & Scrollable Containers

Use scissor testing for scrollable lists:

```java
Render2D.pushScissor(leftX, listY, leftW, listH);
// Draw scrollable items with offset itemY - scrollAmount
Render2D.popScissor();
```
