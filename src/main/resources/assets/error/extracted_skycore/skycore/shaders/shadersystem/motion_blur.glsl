#version 150 core

uniform sampler2D uTex;
uniform sampler2D uPrev;
uniform float strength;

in vec2 fragCoord;

out vec4 fragColor;

void main() {
    vec4 current = texture(uTex, fragCoord);
    vec4 previous = texture(uPrev, fragCoord);
    float amount = clamp(strength, 0.0, 0.98);
    fragColor = mix(current, previous, amount);
}
