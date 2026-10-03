#version 150

// Фрагмент батч-пилюли: drop-glow + диагональный градиент заливки + knob —
// всё в одном проходе, 1:1 повторяет renderTogglePill(enabled) (gradient через
// sdf_rect.renderGradient + отдельный knob fill). Цвета — uniform на весь батч.

in vec2 vLocal;   // позиция фрагмента отн. центра пилюли, px (с glow-зоной)
in vec2 vHalf;    // полуразмер РЕАЛЬНОЙ пилюли, px
in float vRadius; // скругление пилюли, px
in float vGlow;   // дистанция затухания glow наружу, px

uniform float Smoothness; // ширина AA-кромки (~1px)
uniform vec4 FillColor;    // accent  (верхний-левый угол градиента)
uniform vec4 FillColor2;   // accent2 (нижний-правый угол)
uniform vec4 GlowColor;    // dragGlow (drop-glow вокруг пилюли)
uniform vec4 KnobColor;    // onAccent (бегунок справа)

out vec4 OutColor;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

vec4 over(vec4 under, vec4 above) {
    float a = above.a + under.a * (1.0 - above.a);
    if (a < 0.0001) return vec4(0.0);
    vec3 rgb = (above.rgb * above.a + under.rgb * under.a * (1.0 - above.a)) / a;
    return vec4(rgb, a);
}

void main() {
    float distMain = sdRoundedRect(vLocal, vHalf, vRadius);

    // ── Glow: drop-shadow без смещения, затухание на vGlow (box-shadow 0 0 Npx).
    float glowA = 1.0 - smoothstep(0.0, max(vGlow, 0.0001), distMain);
    vec4 cGlow = vec4(GlowColor.rgb, GlowColor.a * glowA);

    // ── Заливка: диагональный (135°) градиент accent → accent2.
    float aaOuter = 1.0 - smoothstep(-Smoothness, Smoothness, distMain);
    vec2 uvRect = clamp((vLocal + vHalf) / max(2.0 * vHalf, vec2(0.0001)), 0.0, 1.0);
    float diag = clamp((uvRect.x + uvRect.y) * 0.5, 0.0, 1.0);
    vec4 fillMixed = mix(FillColor, FillColor2, diag);
    vec4 cFill = vec4(fillMixed.rgb, fillMixed.a * aaOuter);

    // ── Knob: круг справа (enabled-состояние). knobR = halfH - 1, центр на
    // (halfW - knobR - 1, 0) = (halfW - halfH, 0).
    float knobR = vHalf.y - 1.0;
    vec2 knobC = vec2(vHalf.x - vHalf.y, 0.0);
    float dKnob = length(vLocal - knobC) - knobR;
    float knobA = 1.0 - smoothstep(-Smoothness, Smoothness, dKnob);
    vec4 cKnob = vec4(KnobColor.rgb, KnobColor.a * knobA);

    // Композиция снизу-вверх: glow → заливка → knob.
    vec4 color = vec4(0.0);
    color = over(color, cGlow);
    color = over(color, cFill);
    color = over(color, cKnob);

    if (color.a < 0.001) discard;
    OutColor = color;
}
