#version 150 core

uniform sampler2D uTex;
uniform float saturation;

in vec2 fragCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(uTex, fragCoord);
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(luma), color.rgb, saturation);
    fragColor = color;
}
