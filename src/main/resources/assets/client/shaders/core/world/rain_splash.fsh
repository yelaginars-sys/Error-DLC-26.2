#version 330

uniform sampler2D Sampler0;

layout(std140) uniform SadnesRainSplash {
    mat4 InverseProjection;
    mat4 InverseViewRotation;
    vec3 CameraPosition;
    float _CameraPositionPadding;
    vec3 RainSplashColor;
    float _RainSplashColorPadding;
    float RainAmount;
    float Time;
    float RainSplashStyle;
    float RainSplashDensity;
    float RainSplashSize;
    float RainSplashStrength;
    float RainSplashSpeed;
    float RainSplashGlow;
    float RainWindX;
    float RainWindZ;
    float _pad0;
    float _pad1;
};

in vec2 texCoord0;
out vec4 fragColor;

float hash21(vec2 value) {
    vec3 p = fract(vec3(value.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 reconstructViewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    return view.xyz / max(abs(view.w), 0.00001) * sign(view.w);
}

vec3 rainSplashLayer(vec2 worldXZ, float layerScale, float timeOffset) {
    float density = max(RainSplashDensity, 0.05);
    float densityScale = mix(0.72, 2.35, clamp(density / 3.0, 0.0, 1.0));
    vec2 scaled = worldXZ * densityScale * layerScale;
    vec2 cell = floor(scaled);
    float seed = hash21(cell + vec2(19.4 + timeOffset * 11.0, 7.1));
    vec2 impactOffset = vec2(
        hash21(cell + vec2(3.7, 13.1) + timeOffset),
        hash21(cell + vec2(17.9, 5.3) - timeOffset)
    ) - 0.5;
    impactOffset *= 0.64;

    float phase = fract(Time * 0.66 * max(RainSplashSpeed, 0.05) + seed + timeOffset);
    vec2 local = fract(scaled) - 0.5 - impactOffset;
    local += vec2(RainWindX, RainWindZ) * phase * 0.026;
    float size = max(RainSplashSize, 0.08);
    float distanceToImpact = length(local);
    float radius = phase * 0.46 * size;
    float lineWidth = mix(0.018, 0.047, clamp(size / 2.5, 0.0, 1.0));
    float ring = 1.0 - smoothstep(lineWidth, lineWidth * 2.45, abs(distanceToImpact - radius));
    float life = 1.0 - smoothstep(0.38, 1.0, phase);
    float activation = step(
        hash21(cell + vec2(29.2, 41.7)),
        clamp(0.14 + density * 0.205, 0.12, 0.88)
    );

    float angle = atan(local.y, local.x);
    float crownRadius = mix(0.025, 0.18, smoothstep(0.0, 0.34, phase)) * size;
    float crownBand = 1.0 - smoothstep(
        lineWidth * 1.1, lineWidth * 3.0, abs(distanceToImpact - crownRadius)
    );
    float crown = crownBand
        * pow(0.5 + 0.5 * sin(angle * 7.0 + seed * 37.0), 12.0)
        * (1.0 - smoothstep(0.08, 0.52, phase));
    float impact = exp(-distanceToImpact * (92.0 / size))
        * (1.0 - smoothstep(0.015, 0.19, phase));

    int style = int(floor(RainSplashStyle + 0.5));
    if (style == 0) {
        crown *= 0.12;
        impact *= 0.30;
    } else if (style == 1) {
        ring *= 0.48;
        crown *= 1.65;
        impact *= 0.72;
    } else if (style == 3) {
        ring *= 0.42;
        crown *= 0.28;
        impact *= 1.42;
    } else if (style == 4) {
        float secondRing = 1.0 - smoothstep(
            lineWidth * 1.5, lineWidth * 4.2, abs(distanceToImpact - radius * 0.58)
        );
        ring = ring * 1.18 + secondRing * 0.72;
        crown *= 0.68;
        impact *= 1.65;
    }
    return vec3(ring * life, crown, impact) * activation;
}

void main() {
    float depth = texture(Sampler0, texCoord0).r;
    if (depth >= 0.99998) {
        discard;
    }

    vec3 viewPosition = reconstructViewPosition(texCoord0, depth);
    float distanceToCamera = length(viewPosition);
    if (distanceToCamera > 38.0) {
        discard;
    }
    vec3 tangentX = dFdx(viewPosition);
    vec3 tangentY = dFdy(viewPosition);
    vec3 viewNormal = normalize(cross(tangentX, tangentY));
    if (dot(viewNormal, normalize(-viewPosition)) < 0.0) {
        viewNormal = -viewNormal;
    }
    vec3 worldNormal = normalize((InverseViewRotation * vec4(viewNormal, 0.0)).xyz);
    float upward = smoothstep(0.35, 0.88, worldNormal.y);
    if (upward <= 0.001) {
        discard;
    }

    vec3 worldPosition = (InverseViewRotation * vec4(viewPosition, 0.0)).xyz + CameraPosition;
    vec3 splash = rainSplashLayer(worldPosition.xz, 1.0, 0.0);
    splash += rainSplashLayer(worldPosition.xz + vec2(7.3, -3.8), 1.37, 0.37) * 0.78;
    if (int(floor(RainSplashStyle + 0.5)) == 3 || RainSplashDensity > 1.45) {
        splash += rainSplashLayer(worldPosition.xz - vec2(4.1, 8.7), 1.82, 0.71) * 0.58;
    }

    float detailFade = 1.0 - smoothstep(10.0, 38.0, distanceToCamera);
    float energy = (splash.x * 0.32 + splash.y * 0.58 + splash.z)
        * RainAmount * RainSplashStrength * upward * detailFade;
    float glow = dot(splash, vec3(1.0)) * RainSplashGlow * 0.08;
    float alpha = clamp(energy * 0.52 + glow, 0.0, 0.62);
    if (alpha <= 0.002) {
        discard;
    }
    vec3 color = mix(RainSplashColor * 0.72, RainSplashColor * 1.35, clamp(energy, 0.0, 1.0));
    fragColor = vec4(color, alpha);
}
