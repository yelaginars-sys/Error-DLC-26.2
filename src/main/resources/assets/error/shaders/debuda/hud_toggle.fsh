#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;       // размер тумблера
uniform float Progress;  // 0 — выкл, 1 — вкл
uniform vec4 PillOff;    // цвет капсулы выкл
uniform vec4 PillOn;     // цвет капсулы вкл
uniform vec4 Knob;       // цвет ползунка
uniform float Inset;     // отступ ползунка от края в пикселях
uniform vec4 ColorModulator; // глобальный множитель (RenderSystem.setShaderColor) — фейд ClickGui

out vec4 OutColor;

// SDF отрезка со скруглением r — идеальная капсула при любом размере
float sdSegment(vec2 p, vec2 a, vec2 b, float r) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float t = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * t) - r;
}

void main() {
    vec2 px = FragCoord * Size;
    float r = Size.y * 0.5;

    // фон-капсула: осевой отрезок от (r, r) до (w-r, r), радиус r
    // АА через fwidth — ровно ~1px независимо от размера/масштаба (без размытия)
    float dPill = sdSegment(px, vec2(r, r), vec2(Size.x - r, r), r);
    float aaP = max(fwidth(dPill), 0.0001);
    float pillA = 1.0 - smoothstep(-aaP, aaP, dPill);
    vec4 pill = mix(PillOff, PillOn, Progress);
    vec4 col = vec4(pill.rgb, pill.a * pillA);

    // ползунок-круг, скользит слева направо по Progress
    float knobR = r - Inset;
    float cx = mix(Inset + knobR, Size.x - Inset - knobR, Progress);
    float dKnob = length(px - vec2(cx, r)) - knobR;
    float aaK = max(fwidth(dKnob), 0.0001);
    float ka = (1.0 - smoothstep(-aaK, aaK, dKnob)) * Knob.a;

    // альфа-композиция ползунка поверх капсулы
    col.rgb = mix(col.rgb, Knob.rgb, ka);
    col.a = col.a + ka * (1.0 - col.a);

    OutColor = col * FragColor * ColorModulator;
    if (OutColor.a <= 0.0) {
        discard;
    }
}
