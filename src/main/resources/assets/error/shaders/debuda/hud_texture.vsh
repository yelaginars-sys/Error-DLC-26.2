#version 150

// Самодостаточный (без #moj_import): rvertexcoord встроен, как в hud_rect. Include
// (#moj_import <isle-client:common.glsl>) на части GPU/сборок не резолвится → шейдер
// не линкуется → BuiltTexture NPE → плашка «убитая» (только ректы, не двигается).

in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;

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

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
