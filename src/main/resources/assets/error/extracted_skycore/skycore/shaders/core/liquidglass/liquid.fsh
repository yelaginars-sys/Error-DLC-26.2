#version 150

// Liquid glass — corner SDF/AA identical to blurred_round_rect (Blur2).

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float CornerSmoothness;
uniform float GlobalAlpha;

uniform float FresnelPower;
uniform vec3 FresnelColor;
uniform float FresnelAlpha;
uniform float BaseAlpha;
uniform int FresnelInvert;
uniform float FresnelMix;
uniform float DistortStrength;
uniform float BlurStrength;
uniform vec2 resolution;
uniform vec3 OverlayColor;
uniform float OverlayStrength;

out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec3 sampleGlass(vec2 uv) {
    vec3 center = texture(Sampler0, uv).rgb;
    if (BlurStrength < 0.35) {
        return center;
    }
    vec2 texel = BlurStrength / max(resolution, vec2(1.0));
    vec3 acc = center;
    int n = 1;
    const float TAU = 6.28318530718;
    for (float a = 0.0; a < TAU; a += TAU / 10.0) {
        vec2 dir = vec2(cos(a), sin(a));
        for (float r = 0.25; r <= 1.0; r += 0.25) {
            acc += texture(Sampler0, clamp(uv + dir * texel * r, vec2(0.001), vec2(0.999))).rgb;
            n++;
        }
    }
    return acc / float(n);
}

void main() {
    vec2 center = Size * 0.5;
    vec2 pos = center - (FragCoord * Size);
    vec2 box_half_size = max(center - vec2(1.0), vec2(1.0));

    float sdf = roundedBoxSDF(pos, box_half_size, Radius);
    float alpha = 1.0 - smoothstep(0.0, 1.0, sdf);

    float distToEdge = abs(sdf);
    float max_dist_norm = max(1.0, min(box_half_size.x, box_half_size.y));
    float edge_gradient = 1.0 - clamp(distToEdge / max_dist_norm, 0.0, 1.0);

    float fresnel;
    float base = FresnelInvert != 0 ? edge_gradient : (1.0 - edge_gradient);
    if (FresnelPower > 20.0) {
        fresnel = exp(FresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, FresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 screenUv = gl_FragCoord.xy / resolution;

    float absInside = smoothstep(1.0, 3.5, -sdf);
    float relStart = max(0.4, max_dist_norm * 0.05);
    float relFull = max(relStart + 0.5, max_dist_norm * 0.55);
    float relInside = smoothstep(relStart, relFull, -sdf);
    float smallness = clamp(90.0 / max(max_dist_norm, 1.0), 0.0, 1.0);
    float inside = mix(absInside, max(absInside, relInside), smallness);

    // Thick-lens feel: radial pinch + mild swirl near the rim.
    float sizeBoost = mix(1.15, clamp(160.0 / max(min(Size.x, Size.y), 1.0), 1.0, 3.6), smallness);
    vec2 radial = length(pos) > 0.001 ? normalize(-pos) : vec2(0.0);
    vec2 tangent = vec2(-radial.y, radial.x);
    float rim = fresnel * fresnel;
    float body = 0.22 * inside;
    float warpAmt = (rim * 1.15 + body) * DistortStrength * inside * sizeBoost;
    // Soft barrel/pinch: stronger toward center of the lens, not only the rim.
    float pinch = inside * inside * DistortStrength * 0.55 * sizeBoost;
    vec2 warped = screenUv
            + radial * (warpAmt + pinch)
            + tangent * rim * DistortStrength * 0.18 * sizeBoost;
    vec2 distortedUv = clamp(warped, vec2(0.001), vec2(0.999));

    vec3 texColor = sampleGlass(distortedUv);
    // Subtle chromatic fringe on the glass rim.
    float chroma = abs(DistortStrength) * rim * 0.35 * sizeBoost;
    if (chroma > 0.0004) {
        vec3 split;
        split.r = sampleGlass(clamp(distortedUv + radial * chroma, vec2(0.001), vec2(0.999))).r;
        split.g = texColor.g;
        split.b = sampleGlass(clamp(distortedUv - radial * chroma, vec2(0.001), vec2(0.999))).b;
        texColor = mix(texColor, split, clamp(rim * 0.85, 0.0, 1.0));
    }

    vec3 finalColor = mix(texColor, FresnelColor, fresnel * FresnelMix);
    finalColor = mix(finalColor, OverlayColor, clamp(OverlayStrength, 0.0, 0.8));
    // Soft specular lip along the edge.
    finalColor += FresnelColor * rim * 0.12;

    float finalAlpha = mix(BaseAlpha, FresnelAlpha, fresnel) * alpha;
    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha * GlobalAlpha) * FragColor;
}
