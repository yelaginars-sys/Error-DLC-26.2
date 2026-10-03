#version 150 core
uniform sampler2D blurredTexture;
uniform vec2 resolution;
uniform vec2 location;
uniform vec2 size;
uniform vec4 round;
uniform vec4 modulate;
uniform float blurStrength;
in vec2 fragCoord;
out vec4 fragColor;
float signedDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}
vec3 radialBlurSample(vec2 uv) {
    vec3 center = texture(blurredTexture, uv).rgb;
    if (blurStrength < 0.5) {
        return center;
    }
    vec2 ts = vec2(textureSize(blurredTexture, 0));
    vec2 mult = blurStrength / ts;
    vec3 acc = center;
    const float PI = 6.28318530718;
    const float STEP = PI / 12.0;
    int n = 1;
    for (float d = 0.0; d < PI; d += STEP) {
        for (float i = 0.25; i <= 1.0; i += 0.25) {
            acc += texture(blurredTexture, uv + vec2(cos(d), sin(d)) * mult * i).rgb;
            n++;
        }
    }
    return acc / float(n);
}
void main() {
    vec2 blurUV = gl_FragCoord.xy / resolution;
    vec3 blurred = radialBlurSample(blurUV);
    vec3 rgb = blurred * modulate.rgb;
    vec2 rectHalf = size * 0.5;
    float rr = 1.0 - smoothstep(0.0, 1.0, signedDistanceField(rectHalf - (fragCoord * size), rectHalf - 1.0, round));
    fragColor = vec4(rgb, rr * modulate.a);
}