#version 150

// Батч-версия текстурированного скруглённого прямоугольника (hud_texture /
// mre:texture). Группируется по id текстуры — это та самая «сортировка по
// текстуре», ради которой батч и затевается.
//   Position — экранная позиция, ModelView*caller уже применены на CPU
//   Color    — цвет/тинт угла, ColorModulator уже свёрнут
//   UV0      — координаты текстуры (занимают штатный UV-канал)
//   UV1      — размер примитива (w * 16, h * 16)
//   UV2      — (radius * 64, показатель сквиркла * 256; 0 = обычное скругление)
//
// Один радиус на все углы: под четыре разных каналов не остаётся, такие вызовы
// UiBatch уводит в direct-путь.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;
out vec2 vSize;
out float vRadius;
out float vSquircle;

const vec2[4] RECT_VERTICES_COORDS = vec2[](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

void main() {
    FragCoord = RECT_VERTICES_COORDS[gl_VertexID % 4];
    TexCoord = UV0;
    FragColor = Color;
    vSize = vec2(float(UV1.x), float(UV1.y)) / 16.0;
    vRadius = float(UV2.x) / 64.0;
    vSquircle = float(UV2.y) / 256.0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
