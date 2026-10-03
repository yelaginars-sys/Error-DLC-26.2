#version 150

in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

uniform sampler2D Sampler0;
uniform float Range;
uniform float Thickness;
uniform float Smoothness;
uniform bool Outline;
uniform float OutlineThickness;
uniform vec4 OutlineColor;
uniform vec4 ColorModulator;

uniform bool EnableFadeout;
uniform float FadeoutStart;
uniform float FadeoutEnd;
uniform float MaxWidth;
uniform float TextPosX;

out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

// Alpha одного MSDF-сэмпла в точке uv. pixels = screenPxRange (дистанция в
// экранных пикселях на единицу MSDF-distance).
float msdfAlpha(vec2 uv, float pixels) {
    float d = median(texture(Sampler0, uv).rgb) - 0.5 + Thickness;
    return smoothstep(-Smoothness, Smoothness, d * pixels);
}

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0)); // явный vec2 — строгие драйверы (слабые GPU) не делают ivec→vec неявно

    // screenPxRange. На нормальной видюхе — через производные (как раньше, попиксельно
    // корректно даже при батче разных размеров). Но если dFdx/dFdy на слабой/старой
    // видюхе мертвы или возвращают NaN — hlen2 не пройдёт проверку (сравнения с NaN =
    // false) и берём фиксированную разумную ширину AA. Текст остаётся ВИДЕН.
    // max(.., 1.0) — эталонный нижний кламп screenPxRange (Chlumsky).
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * texSize;
    float hlen2 = h.x * h.x + h.y * h.y;
    float pixels = (hlen2 > 1e-8) ? max(Range * inversesqrt(hlen2), 1.0) : 4.0;

    // 2×2 supersample: смещения в UV считаем ИЗ pixels/Range/texSize, а НЕ из
    // dFdx(TexCoord). Раньше при NaN-производных смещение = NaN → texture(Sampler0, NaN)
    // = 0 → median<0.5 → текст пропадал целиком. Текст не повёрнут: оси UV = осям экрана.
    float texPerPx = Range / max(pixels, 0.0001);
    vec2 dx = vec2(0.25 * texPerPx / texSize.x, 0.0);
    vec2 dy = vec2(0.0, 0.25 * texPerPx / texSize.y);
    float alpha = msdfAlpha(TexCoord + dx + dy, pixels)
                + msdfAlpha(TexCoord + dx - dy, pixels)
                + msdfAlpha(TexCoord - dx + dy, pixels)
                + msdfAlpha(TexCoord - dx - dy, pixels);
    alpha *= 0.25;

    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    // Центральный сэмпл нужен ТОЛЬКО обводке. Раньше он брался безусловно, до
    // 2×2 supersample'а — то есть каждый пиксель каждой строки клиента платил за
    // пятую выборку текстуры впустую (обводка на HUD выключена). Outline —
    // uniform, ветка не расходится по варпу, картинка не меняется.
    if (Outline) {
        float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + Thickness;
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness, Smoothness, (dist + OutlineThickness) * pixels);
    }

    // Apply horizontal fadeout
    if (EnableFadeout) {
        float fadeAlpha = 1.0;
        // Вычисляем позицию относительно начала текста
        float relativeX = GlobalPos.x - TextPosX;
        // Нормализуем относительно ширины текста
        float normalizedX = relativeX / MaxWidth;
        if (normalizedX > FadeoutStart) {
            fadeAlpha = 1.0 - smoothstep(FadeoutStart, FadeoutEnd, normalizedX);
        }
        color.a *= fadeAlpha;
    }

    OutColor = color * ColorModulator;
}