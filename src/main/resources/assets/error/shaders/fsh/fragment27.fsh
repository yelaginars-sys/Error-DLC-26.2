#version 330

layout(std140) uniform ZenithData {
    vec2 Size;
    vec4 Radius;
    float Smoothness;
    float CornerSmoothness;
    float GlobalAlpha;
    float FresnelPower;
    vec3 FresnelColor;
    float FresnelAlpha;
    float BaseAlpha;
    int FresnelInvert;
    float FresnelMix;
    float DistortStrength;
    float Time;
    vec3 GlareColor;
    float GlareAlpha;
    float GlareSpeed;
};

#moj_import <zenith:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;


out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0/smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

void main() {
    vec2 center = Size * 0.5;
    vec2 box_half_size = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    float distance = roundedBoxSDF(-pos, box_half_size, Radius, CornerSmoothness);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    float distToEdge = abs(roundedBoxSDF(pos, box_half_size, Radius, CornerSmoothness));

    float max_dist_norm = min(box_half_size.x, box_half_size.y);
    float edge_gradient = 1.0 - clamp(distToEdge / max_dist_norm, 0.0, 1.0);

    float fresnel;
    float base = FresnelInvert != 0 ? edge_gradient : (1.0 - edge_gradient);

    if (FresnelPower > 20.0) {
        fresnel = exp(FresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, FresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    // Refraction distortion
    vec2 dir = normalize(pos + vec2(0.001));
    vec2 baseDistort = dir * fresnel * DistortStrength;

    // Multi-sample refraction with chromatic aberration
    float aberration = DistortStrength * 2.5;
    vec3 refracted = vec3(0.0);
    float totalWeight = 0.0;

    for (int i = 0; i < 5; i++) {
        float s = float(i) * 0.25;
        float g = s * 2.0 - 1.0;
        float weight = exp(-g * g * 2.0);
        vec2 offset = baseDistort * (0.3 + s * 1.4);

        refracted.r += texture(Sampler0, TexCoord + offset * (1.0 + aberration)).r * weight;
        refracted.g += texture(Sampler0, TexCoord + offset).g * weight;
        refracted.b += texture(Sampler0, TexCoord + offset * (1.0 - aberration)).b * weight;
        totalWeight += weight;
    }
    refracted /= totalWeight;
    vec4 texColor = vec4(refracted, 1.0) * FragColor;

    // Diagonal glare — alternating corners
    vec2 normPos = (FragCoord - 0.5) * 2.0;

    // Cycle through 4 diagonal directions (corner to corner)
    float cycle = mod(Time * GlareSpeed, 4.0);
    float phase = floor(cycle);
    float t = fract(cycle);

    // Smooth sweep: ease-in-out
    float eased = t * t * (3.0 - 2.0 * t);

    // 4 corner directions
    vec2 d0 = normalize(vec2( 1.0,  0.7));  // bottom-right
    vec2 d1 = normalize(vec2(-1.0,  0.7));  // bottom-left
    vec2 d2 = normalize(vec2(-1.0, -0.7));  // top-left
    vec2 d3 = normalize(vec2( 1.0, -0.7));  // top-right

    vec2 diagDir;
    if (phase < 1.0)      diagDir = d0;
    else if (phase < 2.0) diagDir = d1;
    else if (phase < 3.0) diagDir = d2;
    else                   diagDir = d3;

    vec2 perpDir = vec2(-diagDir.y, diagDir.x);

    float perp = dot(normPos, perpDir);
    float along = dot(normPos, diagDir);

    // Sweep from one edge to the other
    float sweep = mix(-1.6, 1.6, eased);
    float bandDist = perp - sweep;

    float sharpBand = exp(-bandDist * bandDist * 35.0);
    float endFade = smoothstep(1.3, 0.4, abs(along));

    // Fade in at start, fade out at end of each sweep
    float sweepFade = smoothstep(0.0, 0.15, t) * smoothstep(1.0, 0.85, t);

    float glare = sharpBand * endFade * sweepFade * GlareAlpha;
    glare *= (1.0 - edge_gradient * 0.4);

    // Edge caustic — light concentration at glass edges
    float caustic = pow(edge_gradient, 4.0) * 0.1;

    // Combine
    vec3 finalColor = mix(texColor.rgb, FresnelColor, fresnel * FresnelMix);
    finalColor += glare * GlareColor + caustic * FresnelColor;
    finalColor = clamp(finalColor, 0.0, 1.0);

    float finalAlpha = (mix(BaseAlpha, FresnelAlpha, fresnel) + glare * 0.35) * alpha;

    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha * GlobalAlpha);
}

