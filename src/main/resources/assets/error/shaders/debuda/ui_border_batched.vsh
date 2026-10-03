#version 150

// Батч-версия скруглённой обводки (hud_border / mre:border).
//   Position — экранная позиция, ModelView*caller уже применены на CPU
//   Color    — цвет угла, ColorModulator уже свёрнут
//   UV0      — размер примитива (w, h) в px
//   UV1      — (radius * 64, thickness * 64)
//   UV2      — (внутренняя мягкость * 256, внешняя мягкость * 256)
//
// Батчится только ОДИНАКОВЫЙ радиус на все четыре угла — под четыре разных
// каналов не остаётся, такие вызовы UiBatch уводит в direct-путь.

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
out float vRadius;
out float vThickness;
out vec2 vSmoothness;

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
    vRadius = float(UV1.x) / 64.0;
    vThickness = float(UV1.y) / 64.0;
    vSmoothness = vec2(float(UV2.x), float(UV2.y)) / 256.0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
