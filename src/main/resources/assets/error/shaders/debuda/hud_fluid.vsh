#version 150

// Порт wonderful:core/position_color в namespace isle-client для шейдерного фона
// HUD. rvertexcoord встроен, без include — hud_* шейдеры обязаны быть
// самодостаточными, иначе на части GPU программа не собирается.

in vec3 Position; // POSITION_COLOR
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;

const vec2[4] RECT_VERTICES_COORDS = vec2[](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

void main() {
    FragCoord = RECT_VERTICES_COORDS[gl_VertexID % 4];
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
