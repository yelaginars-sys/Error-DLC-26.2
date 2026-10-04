#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform SadnesWeatherPost {
    mat4 Projection;
    mat4 InverseProjection;
    mat4 ViewRotation;
    mat4 InverseViewRotation;
    vec3 CameraPosition;
    float _CameraPositionPadding;
    vec3 ReflectionCameraPosition;
    float _ReflectionCameraPositionPadding;
    vec3 SkyColor;
    float _SkyColorPadding;
    vec3 LightningColor;
    float _LightningColorPadding;
    vec3 RainColor;
    float _RainColorPadding;
    vec3 RainSplashColor;
    float _RainSplashColorPadding;
    vec2 Resolution;
    float ZoomScale;
    float WetEnabled;
    float AoEnabled;
    float FogEnabled;
    float FogAtmosphere;
    float EnhancedRainEnabled;
    float RainAmount;
    float Time;
    float LightningAmount;
    float LightningStrength;
    float LightningAzimuth;
    float LightningSeed;
    float LightningStyle;
    float LightningWidth;
    float LightningIlluminate;
    float LightningReflect;
    float RainStyle;
    float RainMistEnabled;
    float RainMistStrength;
    float RainSplashEnabled;
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

float valueNoise(vec2 value) {
    vec2 cell = floor(value);
    vec2 local = fract(value);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float wetPattern(vec2 worldXZ) {
    float large = valueNoise(worldXZ * 0.23);
    float medium = valueNoise(worldXZ * 0.72 + vec2(11.7, 4.3));
    float fine = valueNoise(worldXZ * 2.15 - vec2(2.1, 8.6));
    return clamp(large * 0.55 + medium * 0.31 + fine * 0.14, 0.0, 1.0);
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
    float ring = 1.0 - smoothstep(
        lineWidth,
        lineWidth * 2.45,
        abs(distanceToImpact - radius)
    );
    float life = (1.0 - smoothstep(0.38, 1.0, phase));
    float activation = step(
        hash21(cell + vec2(29.2, 41.7)),
        clamp(0.14 + density * 0.205, 0.12, 0.88)
    );

    float angle = atan(local.y, local.x);
    float crownRadius = mix(0.025, 0.18, smoothstep(0.0, 0.34, phase)) * size;
    float crownBand = 1.0 - smoothstep(
        lineWidth * 1.1,
        lineWidth * 3.0,
        abs(distanceToImpact - crownRadius)
    );
    float crown = crownBand
        * pow(0.5 + 0.5 * sin(angle * 7.0 + seed * 37.0), 12.0)
        * (1.0 - smoothstep(0.08, 0.52, phase));
    float impact = exp(-distanceToImpact * (92.0 / size))
        * (1.0 - smoothstep(0.015, 0.19, phase));

    int splashStyle = int(floor(RainSplashStyle + 0.5));
    if (splashStyle == 0) {
        crown *= 0.12;
        impact *= 0.30;
    } else if (splashStyle == 1) {
        ring *= 0.48;
        crown *= 1.65;
        impact *= 0.72;
    } else if (splashStyle == 3) {
        ring *= 0.42;
        crown *= 0.28;
        impact *= 1.42;
    } else if (splashStyle == 4) {
        float secondRing = 1.0 - smoothstep(
            lineWidth * 1.5,
            lineWidth * 4.2,
            abs(distanceToImpact - radius * 0.58)
        );
        ring = ring * 1.18 + secondRing * 0.72;
        crown *= 0.68;
        impact *= 1.65;
    }

    return vec3(ring * life, crown, impact) * activation;
}

vec3 rainSplashEffect(vec2 worldXZ) {
    vec3 splash = rainSplashLayer(worldXZ, 1.0, 0.0);
    splash += rainSplashLayer(worldXZ + vec2(7.3, -3.8), 1.37, 0.37) * 0.78;
    int splashStyle = int(floor(RainSplashStyle + 0.5));
    if (splashStyle == 3 || RainSplashDensity > 1.45) {
        splash += rainSplashLayer(worldXZ - vec2(4.1, 8.7), 1.82, 0.71) * 0.58;
    }
    return splash;
}

float rainAtmosphereMultiplier() {
    int rainStyleIndex = int(floor(RainStyle + 0.5));
    if (rainStyleIndex == 1) {
        return 1.34;
    }
    if (rainStyleIndex == 2) {
        return 1.18;
    }
    if (rainStyleIndex == 3) {
        return 0.62;
    }
    if (rainStyleIndex == 4) {
        return 0.88;
    }
    return 1.0;
}

float angularDifference(float first, float second) {
    return atan(sin(first - second), cos(first - second));
}

float boltLine(float azimuth, float target, float innerWidth, float outerWidth) {
    float distanceToLine = abs(angularDifference(azimuth, target));
    return 1.0 - smoothstep(innerWidth, outerWidth, distanceToLine);
}

float lightningBolt(vec3 worldDirection) {
    float altitude = clamp(worldDirection.y, -0.28, 0.96);
    float vertical = smoothstep(-0.24, -0.04, altitude)
            * (1.0 - smoothstep(0.82, 0.97, altitude));
    float normalizedHeight = clamp((altitude + 0.18) / 1.08, 0.0, 1.0);
    float coarse = valueNoise(vec2(normalizedHeight * 8.0, LightningSeed * 0.071)) - 0.5;
    float fine = valueNoise(vec2(normalizedHeight * 31.0, LightningSeed * 0.193 + 4.2)) - 0.5;
    int style = int(floor(LightningStyle + 0.5));
    float width = max(LightningWidth, 0.1);
    float pathAmplitude = style == 4 ? 0.19 : (style == 1 ? 0.045 : 0.105);
    float path = coarse * pathAmplitude + fine * (style == 4 ? 0.058 : 0.025);
    if (style == 4) {
        path += sin(normalizedHeight * 57.0 + LightningSeed) * 0.018;
    }
    float azimuth = atan(worldDirection.z, worldDirection.x);
    float innerWidth = 0.0012 * width;
    float outerWidth = 0.0075 * width;
    if (style == 1) {
        innerWidth *= 0.42;
        outerWidth *= 0.52;
    } else if (style == 5) {
        innerWidth *= 1.45;
        outerWidth *= 1.85;
    }
    float mainBolt = boltLine(
            azimuth,
            LightningAzimuth + path,
            innerWidth,
            outerWidth
    );

    if (style == 0 || style == 1) {
        return mainBolt * vertical;
    }

    if (style == 3) {
        float separation = 0.018 + width * 0.003;
        float secondPath = path * 0.76
                + (valueNoise(vec2(normalizedHeight * 19.0, LightningSeed * 0.31)) - 0.5) * 0.035;
        float firstBolt = boltLine(
                azimuth, LightningAzimuth - separation + path,
                innerWidth * 0.78, outerWidth * 0.86
        );
        float secondBolt = boltLine(
                azimuth, LightningAzimuth + separation + secondPath,
                innerWidth * 0.78, outerWidth * 0.86
        );
        return max(firstBolt, secondBolt) * vertical;
    }

    float branchGateA = smoothstep(0.30, 0.36, normalizedHeight)
            * (1.0 - smoothstep(0.60, 0.67, normalizedHeight));
    float branchGateB = smoothstep(0.52, 0.57, normalizedHeight)
            * (1.0 - smoothstep(0.78, 0.84, normalizedHeight));
    float branchPathA = path + (normalizedHeight - 0.34) * 0.19;
    float branchPathB = path - (normalizedHeight - 0.55) * 0.15;
    float branchA = boltLine(
            azimuth, LightningAzimuth + branchPathA,
            innerWidth * 0.65, outerWidth * 0.76
    ) * branchGateA;
    float branchB = boltLine(
            azimuth, LightningAzimuth + branchPathB,
            innerWidth * 0.58, outerWidth * 0.70
    ) * branchGateB;
    float result = max(mainBolt, max(branchA, branchB) * 0.82);

    if (style == 2 || style == 4) {
        float branchGateC = smoothstep(0.13, 0.20, normalizedHeight)
                * (1.0 - smoothstep(0.43, 0.50, normalizedHeight));
        float branchGateD = smoothstep(0.66, 0.71, normalizedHeight)
                * (1.0 - smoothstep(0.91, 0.95, normalizedHeight));
        float branchC = boltLine(
                azimuth,
                LightningAzimuth + path - (normalizedHeight - 0.18) * 0.24,
                innerWidth * 0.48,
                outerWidth * 0.64
        ) * branchGateC;
        float branchD = boltLine(
                azimuth,
                LightningAzimuth + path + (normalizedHeight - 0.69) * 0.27,
                innerWidth * 0.46,
                outerWidth * 0.62
        ) * branchGateD;
        result = max(result, max(branchC, branchD) * 0.72);
    }

    if (style == 5) {
        float halo = boltLine(
                azimuth,
                LightningAzimuth + path,
                outerWidth,
                outerWidth * 3.8
        );
        result = max(result, halo * 0.36);
    }
    return clamp(result * vertical, 0.0, 1.0);
}

float lightningPulse() {
    float flicker = 0.78 + 0.22 * sin(Time * 91.0 + LightningSeed * 7.13);
    return LightningAmount * LightningStrength * flicker;
}

vec3 reconstructViewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    return view.xyz / max(abs(view.w), 0.00001) * sign(view.w);
}

vec3 atmosphericFogColor(float atmosphere) {
    vec3 daylightMist = vec3(0.56, 0.62, 0.66);
    vec3 nightMist = vec3(0.095, 0.12, 0.16);
    vec3 neutralMist = mix(daylightMist, nightMist, atmosphere);
    vec3 subduedSky = SkyColor * mix(0.62, 0.16, atmosphere);
    return mix(subduedSky, neutralMist, mix(0.58, 0.94, atmosphere));
}

float screenEdgeFade(vec2 uv) {
    vec2 edge = min(uv, vec2(1.0) - uv);
    return smoothstep(0.0, 0.095, min(edge.x, edge.y));
}

vec3 stableEnvironment(vec3 reflectedWorldDirection, vec2 worldXZ) {
    float skyFacing = smoothstep(-0.16, 0.72, reflectedWorldDirection.y);
    float horizon = pow(1.0 - abs(reflectedWorldDirection.y), 3.0);
    float broadCloud = valueNoise(
            reflectedWorldDirection.xz * 3.2
            + worldXZ * 0.0035
            + vec2(5.7, 1.9)
    );
    vec3 horizonColor = mix(SkyColor * 0.62, SkyColor * 1.18 + vec3(0.055), horizon);
    vec3 upperColor = SkyColor * (0.82 + broadCloud * 0.24) + vec3(0.025, 0.035, 0.055);
    return mix(horizonColor, upperColor, skyFacing);
}

vec2 projectViewPosition(vec3 viewPosition, out float clipW) {
    vec4 clip = Projection * vec4(viewPosition, 1.0);
    clipW = clip.w;
    return clip.xy / max(clip.w, 0.0001) * 0.5 + 0.5;
}

float calculateAmbientOcclusion(vec2 uv, vec3 centerPosition, vec3 viewNormal, float distanceFade) {
    vec2 texel = 1.0 / max(Resolution, vec2(1.0));
    float pixelRadius = mix(3.2, 1.35, clamp(length(centerPosition) / 30.0, 0.0, 1.0));
    vec2 offsets[4];
    offsets[0] = vec2(1.0, 1.0);
    offsets[1] = vec2(-1.0, 1.0);
    offsets[2] = vec2(1.0, -1.0);
    offsets[3] = vec2(-1.0, -1.0);

    float occlusion = 0.0;
    for (int index = 0; index < 4; index++) {
        vec2 sampleUv = clamp(uv + offsets[index] * texel * pixelRadius, vec2(0.002), vec2(0.998));
        float sampleDepth = texture(Sampler1, sampleUv).r;
        if (sampleDepth >= 0.99998) {
            continue;
        }
        vec3 samplePosition = reconstructViewPosition(sampleUv, sampleDepth);
        vec3 difference = samplePosition - centerPosition;
        float sampleDistance = length(difference);
        float rangeWeight = 1.0 - smoothstep(0.08, 2.15, sampleDistance);
        float hemisphere = max(dot(viewNormal, normalize(difference)) - 0.10, 0.0);
        occlusion += hemisphere * rangeWeight;
    }

    return 1.0 - clamp(occlusion * 0.46 * distanceFade, 0.0, 0.34);
}

vec3 traceScreenReflection(
        vec3 worldOrigin,
        vec3 worldDirection,
        float maximumDistance,
        out float confidence
) {
    confidence = 0.0;
    vec3 hitColor = vec3(0.0);
    float previousRayDistance = 0.12;
    float previousDepthDelta = -100000.0;

    for (int index = 0; index < 8; index++) {
        float progress = (float(index) + 1.0) / 8.0;
        float rayDistance = 0.24 + maximumDistance * pow(progress, 1.72);
        vec3 worldRayPosition = worldOrigin + worldDirection * rayDistance;
        vec3 rayPosition = (ViewRotation * vec4(worldRayPosition - CameraPosition, 0.0)).xyz;

        float clipW;
        vec2 rayUv = projectViewPosition(rayPosition, clipW);
        if (clipW <= 0.02
                || rayUv.x <= 0.002 || rayUv.y <= 0.002
                || rayUv.x >= 0.998 || rayUv.y >= 0.998) {
            break;
        }

        float sampledDepth = texture(Sampler1, rayUv).r;
        if (sampledDepth >= 0.99998) {
            previousRayDistance = rayDistance;
            previousDepthDelta = -100000.0;
            continue;
        }

        vec3 sampledPosition = reconstructViewPosition(rayUv, sampledDepth);
        float depthDelta = (-rayPosition.z) - (-sampledPosition.z);
        float thickness = 0.16 + rayDistance * 0.072;
        bool crossedSurface = depthDelta > 0.0
                && (previousDepthDelta <= 0.0 || depthDelta < thickness);
        if (crossedSurface) {
            float lowDistance = previousRayDistance;
            float highDistance = rayDistance;
            vec2 hitUv = rayUv;

            for (int refinement = 0; refinement < 2; refinement++) {
                float middleDistance = (lowDistance + highDistance) * 0.5;
                vec3 middleWorldPosition = worldOrigin + worldDirection * middleDistance;
                vec3 middleViewPosition = (
                        ViewRotation * vec4(middleWorldPosition - CameraPosition, 0.0)
                ).xyz;
                float middleClipW;
                vec2 middleUv = projectViewPosition(middleViewPosition, middleClipW);
                if (middleClipW <= 0.02
                        || middleUv.x <= 0.002 || middleUv.y <= 0.002
                        || middleUv.x >= 0.998 || middleUv.y >= 0.998) {
                    break;
                }

                float middleDepth = texture(Sampler1, middleUv).r;
                if (middleDepth >= 0.99998) {
                    lowDistance = middleDistance;
                    continue;
                }
                vec3 middleSampledPosition = reconstructViewPosition(middleUv, middleDepth);
                float middleDelta = (-middleViewPosition.z) - (-middleSampledPosition.z);
                if (middleDelta > 0.0) {
                    highDistance = middleDistance;
                    hitUv = middleUv;
                } else {
                    lowDistance = middleDistance;
                }
            }

            hitColor = texture(Sampler0, hitUv).rgb;
            float distanceConfidence = 1.0 - smoothstep(
                    maximumDistance * 0.52, maximumDistance, highDistance
            );
            confidence = screenEdgeFade(hitUv) * (0.68 + distanceConfidence * 0.32);
            break;
        }
        previousRayDistance = rayDistance;
        previousDepthDelta = depthDelta;
    }
    return hitColor;
}

void main() {
    vec2 uv = texCoord0;
    vec4 scene = texture(Sampler0, uv);
    float depth = texture(Sampler1, uv).r;
    if (depth >= 0.99998) {
        vec3 skyResult = scene.rgb;
        vec3 skyViewDirection = normalize(reconstructViewPosition(uv, 0.9999));
        vec3 skyWorldDirection = normalize(
                (InverseViewRotation * vec4(skyViewDirection, 0.0)).xyz
        );
        if (FogEnabled > 0.5) {
            float horizonBand = 1.0 - smoothstep(
                    0.018,
                    mix(0.19, 0.31, FogAtmosphere),
                    abs(skyWorldDirection.y)
            );
            float horizonFog = horizonBand
                    * mix(0.23, 0.72, FogAtmosphere)
                    * mix(1.0, 1.12, RainAmount);
            skyResult = mix(
                    skyResult,
                    atmosphericFogColor(FogAtmosphere),
                clamp(horizonFog, 0.0, 0.78)
            );
        }
        if (EnhancedRainEnabled > 0.5 && RainMistEnabled > 0.5 && RainAmount > 0.01) {
            float rainHorizon = 1.0 - smoothstep(0.035, 0.52, abs(skyWorldDirection.y));
            float movingVeil = valueNoise(
                skyWorldDirection.xz * 5.4
                + vec2(RainWindX, RainWindZ) * Time * 0.035
            );
            float rainMist = rainHorizon
                * RainAmount
                * RainMistStrength
                * rainAtmosphereMultiplier()
                * (0.055 + movingVeil * 0.075);
            vec3 rainMistColor = mix(
                atmosphericFogColor(FogAtmosphere),
                RainColor * 0.58 + vec3(0.08, 0.10, 0.12),
                0.56
            );
            skyResult = mix(skyResult, rainMistColor, clamp(rainMist, 0.0, 0.36));
        }
        if (LightningAmount > 0.001) {
            float pulse = lightningPulse();
            float bolt = lightningBolt(skyWorldDirection);
            float azimuth = atan(skyWorldDirection.z, skyWorldDirection.x);
            float localGlow = exp(-abs(angularDifference(azimuth, LightningAzimuth)) * 3.6);
            if (LightningIlluminate > 0.5) {
                skyResult += LightningColor * pulse * (0.18 + localGlow * 0.34);
            }
            skyResult += mix(LightningColor, vec3(0.96, 0.98, 1.0), bolt * 0.62)
                    * bolt * pulse * 4.8;
        }
        fragColor = vec4(skyResult, scene.a);
        return;
    }

    vec3 viewPosition = reconstructViewPosition(uv, depth);
    float realDistance = length(viewPosition);
    float perceivedDistance = realDistance / max(ZoomScale, 1.0);
    vec3 tangentX = dFdx(viewPosition);
    vec3 tangentY = dFdy(viewPosition);
    vec3 viewNormal = normalize(cross(tangentX, tangentY));
    vec3 toCamera = normalize(-viewPosition);
    if (dot(viewNormal, toCamera) < 0.0) {
        viewNormal = -viewNormal;
    }

    vec3 worldNormal = normalize((InverseViewRotation * vec4(viewNormal, 0.0)).xyz);
    vec3 worldPosition = (InverseViewRotation * vec4(viewPosition, 0.0)).xyz + CameraPosition;
    vec3 result = scene.rgb;

    if (AoEnabled > 0.5 && perceivedDistance < 31.0) {
        float aoDistanceFade = 1.0 - smoothstep(13.0, 30.0, perceivedDistance);
        result *= calculateAmbientOcclusion(uv, viewPosition, viewNormal, aoDistanceFade);
    }

    float upward = smoothstep(0.18, 0.88, worldNormal.y);
    float sideSheen = smoothstep(0.15, 0.75, 1.0 - abs(worldNormal.y)) * 0.16;
    float wetDistanceFade = 1.0 - smoothstep(22.0, 64.0, perceivedDistance);
    float detailFade = 1.0 - smoothstep(10.0, 38.0, perceivedDistance);
    vec3 splashEffect = vec3(0.0);
    float ripple = 0.0;
    if (RainAmount > 0.01
            && RainSplashEnabled > 0.5
            && upward > 0.35
            && detailFade > 0.001) {
        splashEffect = rainSplashEffect(worldPosition.xz)
            * RainAmount
            * detailFade
            * RainSplashStrength;
        ripple = splashEffect.x + splashEffect.y * 0.42;
    }

    float renderedWetness = 0.0;
    if (WetEnabled > 0.5 && wetDistanceFade > 0.001) {
        float pattern = wetPattern(worldPosition.xz);
        float puddle = smoothstep(0.38, 0.72, pattern);
        float film = 0.24 + puddle * 0.76;
        film = mix(0.30, film, detailFade);

        if (EnhancedRainEnabled > 0.5 && RainAmount > 0.01 && upward > 0.35) {
            film = clamp(film + RainAmount * 0.10 + ripple * 0.10, 0.0, 1.15);
        }

        float relativeEdge = (length(tangentX) + length(tangentY)) / max(realDistance, 0.5);
        float edgeSafety = 1.0 - smoothstep(0.075, 0.32, relativeEdge);
        float wetness = clamp(
                (upward * film + sideSheen) * wetDistanceFade * edgeSafety,
                0.0,
                1.0
        );
        renderedWetness = wetness;

        if (wetness > 0.001) {
            vec3 reflectionWorldIncident = normalize(worldPosition - ReflectionCameraPosition);
            vec3 reflectedWorldDirection = normalize(reflect(reflectionWorldIncident, worldNormal));
            vec3 environment = stableEnvironment(reflectedWorldDirection, worldPosition.xz);

            float traceConfidence = 0.0;
            vec3 tracedReflection = vec3(0.0);
            if (upward > 0.24 && detailFade > 0.04) {
                float traceDistance = min(
                        42.0,
                        mix(9.0, 29.0, detailFade) * sqrt(max(ZoomScale, 1.0))
                );
                tracedReflection = traceScreenReflection(
                        worldPosition + worldNormal * 0.055,
                        reflectedWorldDirection,
                        traceDistance,
                        traceConfidence
                );
            }

            float screenAmount = traceConfidence * detailFade * upward * 0.92;
            vec3 reflection = mix(environment, tracedReflection, screenAmount);
            float facing = clamp(
                    dot(worldNormal, normalize(ReflectionCameraPosition - worldPosition)),
                    0.0,
                    1.0
            );
            float fresnel = 0.08 + 0.92 * pow(1.0 - facing, 3.0);
            float skyGloss = upward * (0.16 + fresnel * 0.30);
            float reflectionStrength = wetness * (0.22 + fresnel * 0.36);

            vec3 wetBase = result * (1.0 - wetness * 0.14);
            vec3 coolReflection = mix(
                    reflection,
                    reflection * vec3(0.82, 0.92, 1.08),
                    0.34
            );
            result = mix(wetBase, coolReflection, reflectionStrength);
            result += mix(SkyColor, vec3(0.78, 0.86, 0.96), 0.42)
                    * skyGloss * wetness * 0.22;
            result += mix(SkyColor, vec3(0.88, 0.93, 1.0), 0.5)
                    * ripple * wetness * 0.055;
            if (LightningAmount > 0.001 && LightningReflect > 0.5) {
                float reflectedAzimuth = atan(reflectedWorldDirection.z, reflectedWorldDirection.x);
                float reflectedBolt = exp(
                        -abs(angularDifference(reflectedAzimuth, LightningAzimuth)) * 8.5
                );
                vec3 reflectedLightning = LightningColor
                        * lightningPulse()
                        * wetness
                        * upward
                        * (0.16 + reflectedBolt * 0.72);
                result += reflectedLightning;
            }
        }
    }

    // Splashes remain usable as their own module even when reflective puddles
    // are disabled. They color the impacted surface without sampling or
    // warping the scene framebuffer.
    if (RainSplashEnabled > 0.5 && dot(splashEffect, vec3(1.0)) > 0.001) {
        float crispSplash = splashEffect.x * 0.040
            + splashEffect.y * 0.074
            + splashEffect.z * 0.145;
        float splashHalo = dot(splashEffect, vec3(1.0))
            * RainSplashGlow
            * 0.030;
        float energyBoost = int(floor(RainSplashStyle + 0.5)) == 4 ? 1.42 : 1.0;
        float surfaceVisibility = mix(0.68, 1.0, renderedWetness);
        result += RainSplashColor
            * (crispSplash + splashHalo)
            * upward
            * surfaceVisibility
            * energyBoost;
    }

    if (FogEnabled > 0.5) {
        float fogDistance = smoothstep(
                mix(24.0, 18.0, FogAtmosphere),
                mix(92.0, 76.0, FogAtmosphere),
                perceivedDistance
        );
        float groundLevel = CameraPosition.y - 1.55;
        float heightAboveGround = max(worldPosition.y - groundLevel, 0.0);
        float heightDensity = exp(-heightAboveGround * 0.34);
        float fogAmount = fogDistance * (
                mix(0.055, 0.105, FogAtmosphere)
                + heightDensity * mix(0.17, 0.24, FogAtmosphere)
        );
        fogAmount *= mix(1.0, 1.28, RainAmount);
        vec3 fogColor = atmosphericFogColor(FogAtmosphere);
        fogColor = mix(fogColor, vec3(0.45, 0.50, 0.54), RainAmount * 0.18);
        result = mix(
                result,
                fogColor,
                clamp(fogAmount, 0.0, mix(0.24, 0.43, FogAtmosphere))
        );
    }

    if (EnhancedRainEnabled > 0.5 && RainMistEnabled > 0.5 && RainAmount > 0.01) {
        float rainDistance = smoothstep(12.0, 86.0, perceivedDistance);
        float rainNoise = valueNoise(
            worldPosition.xz * 0.035
            + vec2(RainWindX, RainWindZ) * Time * 0.028
        );
        float lowVeil = exp(-max(worldPosition.y - (CameraPosition.y - 1.8), 0.0) * 0.055);
        float rainMist = rainDistance
            * RainAmount
            * RainMistStrength
            * rainAtmosphereMultiplier()
            * (0.038 + rainNoise * 0.052 + lowVeil * 0.025);
        vec3 rainMistColor = mix(
            atmosphericFogColor(FogAtmosphere),
            RainColor * 0.52 + vec3(0.10, 0.115, 0.13),
            0.48
        );
        result = mix(result, rainMistColor, clamp(rainMist, 0.0, 0.28));
    }

    if (LightningAmount > 0.001 && LightningIlluminate > 0.5) {
        float surfaceFacing = 0.34 + upward * 0.66;
        float distanceFalloff = mix(1.0, 0.68, smoothstep(18.0, 90.0, perceivedDistance));
        result += LightningColor
                * lightningPulse()
                * surfaceFacing
                * distanceFalloff
                * 0.46;
    }

    fragColor = vec4(result, scene.a);
}
