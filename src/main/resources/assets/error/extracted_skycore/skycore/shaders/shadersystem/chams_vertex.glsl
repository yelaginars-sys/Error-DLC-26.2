#version 150 core

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;

out vec3 vWorldDir;

void main() {
    vWorldDir = normalize(Color.rgb * 2.0 - 1.0);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
