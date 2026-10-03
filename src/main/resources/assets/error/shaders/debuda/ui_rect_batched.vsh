#version 150

// Батч-версия скруглённого прямоугольника (hud_rect / mre:rectangle).
//
// Всё, что в одиночном шейдере было uniform'ом, здесь приходит per-vertex через
// штатные каналы BufferBuilder — поэтому десятки прямоугольников с РАЗНЫМИ
// размерами, радиусами и цветами уезжают одним draw-call'ом:
//   Position — экранная позиция вершины, УЖЕ прошедшая ModelView*caller-матрицу
//              на CPU (матрица не входит в ключ батча — см. UiBatch)
//   Color    — цвет угла (4 угла = градиент), уже домноженный на ColorModulator
//   UV0      — размер примитива (w, h) в px
//   UV1      — радиусы углов 1,2 * 64 (fixed-point, 1/64 px)
//   UV2      — радиусы углов 3,4 * 64
//
// FragCoord берётся из gl_VertexID % 4: индексный буфер QUADS у Minecraft отдаёт
// индексы 4k+{0,1,2,2,3,0}, поэтому остаток от деления на 4 — это номер угла,
// как и в одиночном шейдере.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;
out vec2 vSize;
out vec4 vRadius;

const vec2[4] RECT_VERTICES_COORDS = vec2[](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

void main() {
    FragCoord = RECT_VERTICES_COORDS[gl_VertexID % 4];
    FragColor = Color;
    vSize = UV0;
    vRadius = vec4(float(UV1.x), float(UV1.y), float(UV2.x), float(UV2.y)) / 64.0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
