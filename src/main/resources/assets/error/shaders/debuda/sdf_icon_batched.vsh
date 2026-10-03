#version 150

// Batched SDF-icon shader: per-vertex tint вместо TintColor uniform.
// Это позволяет рисовать много иконок (разных цветов) одним draw-call'ом
// — основа sprite-batch паттерна. См. {@link ru.isle.api.system.icons.IconAtlas}.

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 TexCoord;
out vec4 vColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    TexCoord = UV0;
    vColor = Color;
}
