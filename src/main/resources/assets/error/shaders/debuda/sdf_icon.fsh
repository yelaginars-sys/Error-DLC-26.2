#version 150

// SDF-icon atlas sampler. Atlas — single channel SDF (распакован в RGB одинаково),
// 0.5 = граница, 0=снаружи, 1=внутри. Spread в атлас-пикселях задан как uniform.
//
// AA-кромка скейлится автоматически через fwidth(): сколько signed-distance
// меняется на 1 screen-fragment → используем как ширину edge'а. Даёт чёткую
// 1-screen-pixel кромку при ЛЮБОМ render-размере иконки (отсюда SDF-преимущество).
//
// 2×2 supersample: сэмплим SDF в 4 точках вокруг центра пикселя (±¼ derivative)
// и усредняем alpha. Снимает "толстый AA"-эффект на маленьких размерах (9-14px),
// где fwidth огромный и smoothstep даёт мыло. Атлас грузится с mipmap chain
// (glGenerateMipmap при init), поэтому texture() автоматически берёт правильный
// LOD — supersample поверх mip'а добавляет ещё ~2 effective bits точности кромки.

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec4  TintColor;     // финальный цвет, RGB tint × alpha
uniform float SdfSpread;     // SDF spread в атлас-пикселях (см. tools/build_sdf_atlas.py)

out vec4 OutColor;

// MTSDF-декод: distance = median(R,G,B). На single-channel атласе (R=G=B)
// median == R, поэтому правка обратно совместима со старым icon_atlas.png и
// автоматически даёт острые углы после перепекания атласа в multi-channel
// (см. tools/build_mtsdf_atlas.py). Mipmap'ленные сэмплы median'ит так же —
// на используемых mip 0-2 реконструкция кромки остаётся корректной.
float median(vec3 c) {
    return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
}

float sampleAlpha(vec2 uv, float aa) {
    float sdf = median(texture(Sampler0, uv).rgb);
    float signed_dist = (sdf - 0.5) * 2.0 * SdfSpread;
    return smoothstep(-aa, aa, signed_dist);
}

void main() {
    // fwidth берётся от центрального sample — одинаков для всех 4 supersample точек.
    float centerSdf = median(texture(Sampler0, TexCoord).rgb);
    float centerDist = (centerSdf - 0.5) * 2.0 * SdfSpread;
    // На слабой/старой видюхе dFdx/dFdy/fwidth могут быть мертвы или NaN. Сравнения
    // с NaN = false → derivOk=false → берём фиксированную AA и одиночный сэмпл.
    // Иконка остаётся ВИДНА (раньше NaN в UV-смещении → texture()=0 → иконка пропадала).
    float fw = fwidth(centerDist);
    bool derivOk = (fw > 1e-8) && (fw < 1e8);
    float aa = derivOk ? max(fw * 0.5, 0.001) : max(SdfSpread * 0.15, 0.001);

    // Смещения supersample берём только если производные валидны; иначе 0 (один сэмпл).
    vec2 dx = derivOk ? dFdx(TexCoord) * 0.25 : vec2(0.0);
    vec2 dy = derivOk ? dFdy(TexCoord) * 0.25 : vec2(0.0);

    float alpha = sampleAlpha(TexCoord + dx + dy, aa)
                + sampleAlpha(TexCoord + dx - dy, aa)
                + sampleAlpha(TexCoord - dx + dy, aa)
                + sampleAlpha(TexCoord - dx - dy, aa);
    alpha *= 0.25;

    if (alpha < 0.001) discard;
    OutColor = vec4(TintColor.rgb, alpha * TintColor.a);
}
