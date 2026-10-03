#version 150

// Шейдер модуля Trajectory: кубик точки падения снаряда, подсветка хитбокса
// цели и мягкая аура вокруг них. Рисуется аддитивно (GL_ONE, GL_ONE), поэтому
// альфа выхода не участвует — яркость несёт сам цвет.
//
// Цвет приходит вершинами (градиент верх/низ + множитель яркости в альфе),
// uMode переключает рисунок: 1 — кубик, 2 — хитбокс цели.

in vec2 vUv;
in vec4 vColor;

uniform float iTime;
uniform float uAlpha;
uniform float uFill;
uniform float uThickness;
uniform int uMode;

out vec4 OutColor;

void main() {
    vec3 base = vColor.rgb;
    float mul = vColor.a;

    // расстояние до ближайшего края грани в UV
    float edge = min(min(vUv.x, 1.0 - vUv.x), min(vUv.y, 1.0 - vUv.y));
    float rim = smoothstep(uThickness, 0.0, edge);

    vec3 col;

    if (uMode == 1) {
        // Кубик: чёткая рамка + светящееся ядро + дыхание.
        vec2 c = vUv * 2.0 - 1.0;
        float core = 0.05 / (dot(c, c) + 0.06);
        float halo = 0.13 / (edge + 0.13);
        float pulse = 0.78 + 0.22 * sin(iTime * 5.0);
        col = base * (rim * 2.2 + halo * 0.5 + core * 0.8) * pulse;
    } else {
        // Хитбокс: заливка гуще снизу, рамка и бегущая снизу вверх полоса.
        // Наружу ничего не светит — свечение живёт только внутри самой грани.
        float h = clamp(vUv.y, 0.0, 1.0);
        float scan = exp(-pow((h - fract(iTime * 0.35)) * 5.0, 2.0));
        float fill = uFill * (0.18 + 0.5 * (1.0 - h));
        col = base * (rim * 1.9 + fill + scan * 0.55);
    }

    OutColor = vec4(col * uAlpha * mul, 1.0);
}
