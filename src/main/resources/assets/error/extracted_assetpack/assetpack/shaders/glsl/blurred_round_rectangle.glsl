#version 120

uniform sampler2D blurredTexture;
uniform vec2 resolution, start, size;
uniform vec4 round;
uniform float alpha;
uniform vec4 color;

float signedDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;

    vec2 q = abs(p) - b + r.x;

    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 rectHalf = size * 0.5;

    vec2 pos = gl_FragCoord.xy;
    vec2 blurredPos = pos / resolution;
    vec3 blurredColor = mix(texture2D(blurredTexture, blurredPos).rgb, color.rgb, color.a);
    pos.y = resolution.y - pos.y;
    float rr = 1.0 - smoothstep(0.0, 1.0, signedDistanceField(rectHalf - (gl_TexCoord[0].st * size), rectHalf - 1.0, round));
    gl_FragColor = vec4(blurredColor, rr * alpha);
}