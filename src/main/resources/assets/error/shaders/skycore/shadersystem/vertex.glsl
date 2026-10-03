#version 150 core

uniform mat4 Transform;

in vec3 Position;
in vec2 UV0;

out vec2 fragCoord;

void main() {
    fragCoord = UV0;
    gl_Position = Transform * vec4(Position, 1.0);
}
