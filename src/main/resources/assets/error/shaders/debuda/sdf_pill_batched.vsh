#version 150

// Батч включённых toggle-пилюль ClickGui. Все пилюли в кадре одного цвета
// (accent/accent2/glow/knob из MenuColors) → цвета приходят uniform'ами, а
// per-vertex идёт только геометрия. Так десятки «вкл»-пилюль (раньше каждая —
// direct renderGradient с флашем батча) летят ОДНИМ draw-call'ом.
//
//   Position — экранная позиция вершины (pre-transformed на CPU), quad расширен
//              на GlowWidth с каждой стороны под свечение
//   UV0      — localPos: позиция вершины отн. центра пилюли в px (с glow-зоной)
//   UV1      — halfSize РЕАЛЬНОЙ пилюли (halfW, halfH) в px, ivec2
//   UV2.x    — radius * 256 (скругление), UV2.y — glowWidth * 256

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vLocal;
out vec2 vHalf;
out float vRadius;
out float vGlow;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vLocal  = UV0;
    vHalf   = vec2(UV1);
    vRadius = float(UV2.x) / 256.0;
    vGlow   = float(UV2.y) / 256.0;
}
