#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 color;
uniform vec3 color2;
uniform float exposure;
uniform int autoColor;
uniform float saturation;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    vec4 bloom = texture(Sampler0, uv);
    vec4 mask = texture(Sampler1, uv);
    float outer = bloom.a * (1.0 - mask.a);
    float intensity = clamp(outer * exposure, 0.0, 1.0);
    if (intensity <= 0.001) discard;

    vec3 col;
    if (autoColor == 1) {
        vec3 c = bloom.rgb / max(bloom.a, 0.001);
        float m = max(c.r, max(c.g, c.b));
        if (m > 0.001) c /= m;
        float l = dot(c, vec3(0.299, 0.587, 0.114));
        col = clamp(mix(vec3(l), c, saturation), 0.0, 1.0);
    } else {
        col = mix(color, color2, uv.y);
    }
    OutColor = vec4(col, intensity);
}
