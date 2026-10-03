#version 150

// SDF rounded rectangle с текстурой: сэмплируем sub-region (UvRect) из Sampler0
// и маскируем по rounded rect SDF. Используется для аватарок (скин-голова в TargetHud)
// чтобы текстура была вписана в те же скруглённые грани что и панели интерфейса.

in vec2 TexCoord;  // 0..1 по quad'у

uniform sampler2D Sampler0;
uniform vec2  RectSize;     // визуальный размер квадрата в пикселях
uniform vec4  UvRect;       // (u0, v0, u1, v1) — область Sampler0 которую растягиваем на quad
uniform float Radius;       // скругление углов в пикселях
uniform vec4  TintColor;    // умножается на цвет текстуры (для тонировки)
uniform float Smoothness;   // ширина AA-кромки (~1px)

out vec4 OutColor;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 p = TexCoord * RectSize - RectSize * 0.5;
    float dist = sdRoundedRect(p, RectSize * 0.5, Radius);
    float aaOuter = 1.0 - smoothstep(-Smoothness, +Smoothness, dist);
    if (aaOuter <= 0.0) discard;

    vec2 uv = mix(UvRect.xy, UvRect.zw, TexCoord);
    vec4 tex = texture(Sampler0, uv);

    OutColor = vec4(tex.rgb * TintColor.rgb, tex.a * TintColor.a * aaOuter);
}
