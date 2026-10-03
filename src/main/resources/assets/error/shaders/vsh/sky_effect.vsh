#version 330

layout(std140) uniform EffectData {
    vec4 color;
    vec4 color2;
    vec4 screen;
    vec4 params0;
    vec4 params1;
    vec4 tex;
    vec4 halfPixel;
};

in vec3 Position;
out vec4 vertexColor;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vertexColor = color;
}
