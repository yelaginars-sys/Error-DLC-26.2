#version 150

// Batched SDF fragment: tint берётся из per-vertex Color вместо TintColor uniform.
// Логика SDF + AA + 2×2 supersample та же что и в sdf_icon.fsh.

in vec2 TexCoord;
in vec4 vColor;

uniform sampler2D Sampler0;
uniform float SdfSpread;

out vec4 OutColor;

// MTSDF-декод median(R,G,B); на single-channel (R=G=B) == R → обратно совместимо.
float median(vec3 c) {
    return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
}

float sampleAlpha(vec2 uv, float aa) {
    float sdf = median(texture(Sampler0, uv).rgb);
    float signed_dist = (sdf - 0.5) * 2.0 * SdfSpread;
    return smoothstep(-aa, aa, signed_dist);
}

void main() {
    float centerSdf = median(texture(Sampler0, TexCoord).rgb);
    float centerDist = (centerSdf - 0.5) * 2.0 * SdfSpread;
    // NaN/dead-производные на слабой видюхе → derivOk=false → фикс. AA + один сэмпл (иконка видна).
    float fw = fwidth(centerDist);
    bool derivOk = (fw > 1e-8) && (fw < 1e8);
    float aa = derivOk ? max(fw * 0.5, 0.001) : max(SdfSpread * 0.15, 0.001);

    vec2 dx = derivOk ? dFdx(TexCoord) * 0.25 : vec2(0.0);
    vec2 dy = derivOk ? dFdy(TexCoord) * 0.25 : vec2(0.0);

    float alpha = sampleAlpha(TexCoord + dx + dy, aa)
                + sampleAlpha(TexCoord + dx - dy, aa)
                + sampleAlpha(TexCoord - dx + dy, aa)
                + sampleAlpha(TexCoord - dx - dy, aa);
    alpha *= 0.25;

    if (alpha < 0.001) discard;
    OutColor = vec4(vColor.rgb, alpha * vColor.a);
}
