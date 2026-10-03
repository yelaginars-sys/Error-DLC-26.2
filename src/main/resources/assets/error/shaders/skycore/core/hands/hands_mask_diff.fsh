#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    vec3 beforeColor = texture(Sampler0, uv).rgb;
    vec3 afterColor = texture(Sampler1, uv).rgb;
    float depthBefore = texture(Sampler2, uv).r;
    float depthAfter = texture(Sampler3, uv).r;
    float depthMask = step(0.0001, depthBefore - depthAfter);
    float colorMask = smoothstep(0.025, 0.12, length(afterColor - beforeColor));
    float result = max(depthMask, colorMask);
    OutColor = vec4(result, result, result, result);
}
