#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;


out vec2 TexCoord;
out vec4 FragColor;
out vec2 GlobalPos; // Передаем глобальные координаты

void main() {
    TexCoord = UV0;
    FragColor = Color;
    GlobalPos = Position.xy; // Передаем позицию вершины в мировых координатах

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
