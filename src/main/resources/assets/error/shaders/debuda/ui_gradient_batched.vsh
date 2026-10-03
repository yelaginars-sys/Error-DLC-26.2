#version 150

// Батч-версия градиентной заливки (hud_gradient). Раскладка вершины та же, что у
// ui_rect_batched: форма (размер + радиусы) идёт per-vertex, а сам градиент
// (тип, точки, стопы) остаётся uniform'ом и служит КЛЮЧОМ группы батча — в
// реальном HUD тинт Debuda у всех плашек одинаковый, поэтому все они схлопываются
// в один draw, а «прозрачность плашки» переезжает в вершинный цвет.
//   Position — экранная позиция, ModelView*caller уже применены на CPU
//   Color    — множитель цвета (тинт/альфа плашки), ColorModulator уже свёрнут
//   UV0      — размер примитива (w, h) в px
//   UV1      — радиусы углов 1,2 * 64
//   UV2      — радиусы углов 3,4 * 64

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
