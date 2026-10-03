#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D blurredTexture;
uniform vec2 resolution;
uniform vec2 start;
uniform vec2 size;
uniform vec4 round;
uniform float alpha;
uniform vec4 color;     // color.a = mix strength
uniform float roundType; // 0 = normal, 1 = ios/superellipse
uniform float blurStrength;

float signedDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float superellipseDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    float maxRadius = min(b.x, b.y);
    r *= 1.85;
    r = min(r, vec4(maxRadius));
    vec2 q = abs(p) - b + r.x;
    vec2 outside = max(q, 0.0);
    float superLength = pow(pow(outside.x, 3.0) + pow(outside.y, 3.0), 1.0 / 3.0);
    return min(max(q.x, q.y), 0.0) + superLength - r.x;
}

float interfaceDistanceField(vec2 p, vec2 b, vec4 r) {
    return mix(signedDistanceField(p, b, r), superellipseDistanceField(p, b, r), step(0.5, roundType));
}

vec3 sampleBlurred(vec2 uv) {
    vec3 center = texture(blurredTexture, uv).rgb;
    if (blurStrength < 0.5) {
        return center;
    }
    vec2 texel = blurStrength / max(resolution, vec2(1.0));
    vec3 acc = center;
    int n = 1;
    const float PI = 6.28318530718;
    for (float a = 0.0; a < PI; a += PI / 12.0) {
        vec2 dir = vec2(cos(a), sin(a));
        for (float r = 0.2; r <= 1.0; r += 0.2) {
            acc += texture(blurredTexture, uv + dir * texel * r).rgb;
            n++;
        }
    }
    return acc / float(n);
}

void main() {
    vec2 rectHalf = size * 0.5;
    vec2 blurredPos = gl_FragCoord.xy / resolution;
    vec2 localPos = rectHalf - (texCoord * size);
    float sdf = interfaceDistanceField(localPos, rectHalf - 1.0, round);
    float rr = 1.0 - smoothstep(0.0, 1.0, sdf);

    vec3 sampleColor = sampleBlurred(blurredPos);
    vec3 blurredColor = mix(sampleColor, color.rgb, color.a);
    fragColor = vec4(blurredColor, rr * alpha);
}
