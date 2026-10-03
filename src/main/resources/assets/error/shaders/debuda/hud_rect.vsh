#version 150

// Порт otap:core/rectangle в namespace isle-client. Per-corner radius (vec4) +
// per-vertex цвет (4-цветный градиент по интерполяции Color). Логика 1:1 с
// otap (rvertexcoord встроен, чтобы не тащить include) → пиксели идентичны.

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
