#version 150

// Port 1:1 from skycore-release liquid_glass_rectangle.glsl

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 resolution;
uniform vec2 size;
uniform vec4 round;
uniform float alpha;
uniform vec4 color;

out vec4 OutColor;

float liquidGlassDistanceField(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    float maxRadius = min(b.x, b.y);
    r = min(r, vec4(maxRadius));

    vec2 q = abs(p) - b + r.x;
    vec2 qClamped = max(q, 0.0);
    float len = pow(pow(qClamped.x, smoothness) + pow(qClamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

void main() {
    vec2 rectHalf = size * 0.5;
    vec2 blurredPos = gl_FragCoord.xy / resolution;
    vec2 glassPos = (FragCoord * size) - rectHalf;
    vec2 boxHalf = rectHalf - 1.0;

    float glassSdf = liquidGlassDistanceField(-glassPos, boxHalf, round, 3.0);
    float glassMask = 1.0 - smoothstep(0.5, 1.0, glassSdf);

    float distToEdge = abs(liquidGlassDistanceField(glassPos, boxHalf, round, 3.0));
    float maxDist = max(1.0, min(boxHalf.x, boxHalf.y));
    float edgeGradient = 1.0 - clamp(distToEdge / maxDist, 0.0, 1.0);
    float fresnel = exp(50.0 * log(clamp(edgeGradient, 0.001, 1.0)));
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 direction = glassPos;
    direction /= max(length(direction), 0.001);

    float distortionStrength = 0.045;
    vec2 rawDistortedUv = blurredPos + direction * fresnel * distortionStrength;
    vec2 distortedUv = clamp(rawDistortedUv, vec2(0.001), vec2(0.999));
    vec2 edgeDistance = min(blurredPos, vec2(1.0) - blurredPos);
    float edgeFade = smoothstep(0.0, distortionStrength + 0.01, min(edgeDistance.x, edgeDistance.y));
    float outOfBounds = step(rawDistortedUv.x, 0.0) + step(1.0, rawDistortedUv.x)
            + step(rawDistortedUv.y, 0.0) + step(1.0, rawDistortedUv.y);
    edgeFade *= 1.0 - clamp(outOfBounds, 0.0, 1.0);

    vec3 baseSample = texture(Sampler0, blurredPos).rgb;
    vec3 glassSample = mix(baseSample, texture(Sampler0, distortedUv).rgb, edgeFade);
    vec3 glassColor = mix(glassSample, color.rgb, 0.08);
    // Soft edge highlight — avoid harsh white rim on dark frost panels
    glassColor += glassSample * fresnel * 0.08;

    OutColor = vec4(glassColor, glassMask * alpha) * FragColor;
}
