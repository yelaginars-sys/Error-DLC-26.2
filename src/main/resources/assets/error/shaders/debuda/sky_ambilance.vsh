#version 150

// Общий вершинник всех небесных шейдеров Ambilance.
// Квад подаётся сразу в clip space (-1..1), поэтому ModelViewMat/ProjMat не нужны:
// небо всегда занимает весь экран независимо от матриц мира.

in vec3 Position;
in vec2 UV0;

out vec2 TexCoord;

void main() {
    gl_Position = vec4(Position, 1.0);
    TexCoord = UV0;
}
