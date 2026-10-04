#version 330

// Пушинка-лоскут (Particles, режим fluff): рваный полигон-веер из ParticleEngine3D.
// UV0.x — «глубина тела»: 1 в центре лоскута, 0 на рваной кромке; затухание к краю
// и подсветку центра считает фрагментный шейдер.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;
out vec2 localCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    localCoord = UV0;
}
