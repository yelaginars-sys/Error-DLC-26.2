#version 150

// self-contained (без #moj_import): rvertexcoord встроен
const vec2[4] RECT_VERTICES_COORDS = vec2[](
    vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(1.0, 0.0)
);

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    FragCoord = RECT_VERTICES_COORDS[gl_VertexID % 4]; // 0..1
    FragColor = Color;
}