#version 330

layout(std140) uniform SadnesCustomSky {
    vec3 TintColor;
    // Minecraft's Std140Builder reserves a complete 16-byte slot for vec3.
    // Keeping this explicit prevents Time/Speed/Intensity from shifting by one float.
    float _TintPadding;
    float Time;
    float Speed;
    float Intensity;
    float Style;
    float MeteorEnabled;
    float MeteorFrequency;
    float MeteorStyle;
    float MeteorSpeed;
    vec3 MeteorColor;
    float _MeteorColorPadding;
    float MeteorBrightness;
    float MeteorSize;
    float MeteorTrail;
    float MeteorSpread;
    float MeteorGlow;
};

in vec3 skyDir;
out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash31(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.52;
    for (int i = 0; i < 4; i++) {
        value += valueNoise3(p) * amplitude;
        p = p * 2.03 + vec3(7.1, 3.8, 5.4);
        amplitude *= 0.5;
    }
    return value;
}

float starField(vec3 direction, float scale, float threshold) {
    vec3 coordinates = direction * scale;
    vec3 cell = floor(coordinates);
    vec3 local = fract(coordinates) - 0.5;
    float star = smoothstep(0.13, 0.0, length(local));
    float seed = hash31(cell);
    return star * smoothstep(threshold, 1.0, seed)
            * (0.68 + 0.32 * sin(Time * 2.4 + seed * 43.0));
}

float angleDifference(float first, float second) {
    return atan(sin(first - second), cos(first - second));
}

float meteorLayer(vec3 direction, float meteorTime, float layerOffset, int styleIndex) {
    float localTime = meteorTime + layerOffset;
    float cycle = floor(localTime);
    float phase = fract(localTime);
    float seedA = hash31(vec3(cycle, layerOffset * 37.0 + 2.4, 11.7));
    float seedB = hash31(vec3(cycle * 0.73 + 8.1, layerOffset * 19.0, 4.6));
    float seedC = hash31(vec3(cycle * 1.37 + 2.8, layerOffset * 53.0, 19.2));
    float startAzimuth = (seedA * 2.0 - 1.0) * 3.14159265;
    float spread = clamp(MeteorSpread, 0.0, 1.0);
    float startAltitude = mix(0.58, mix(0.22, 0.90, seedB), spread);
    float horizontalSpeed = mix(0.52, 0.96, seedB);
    float verticalSpeed = mix(0.17, 0.38, seedA);

    if (styleIndex == 1) {
        horizontalSpeed *= 1.18;
        verticalSpeed *= 1.35;
    } else if (styleIndex == 2) {
        horizontalSpeed *= 0.72;
        verticalSpeed *= 0.62;
    } else if (styleIndex == 3) {
        horizontalSpeed *= 0.82;
        verticalSpeed *= 1.52;
    } else if (styleIndex == 4) {
        horizontalSpeed *= 1.85;
        verticalSpeed *= 0.12;
        startAltitude = mix(0.18, 0.88, seedB);
    } else if (styleIndex == 5) {
        horizontalSpeed *= 0.46;
        verticalSpeed *= mix(-0.8, 1.8, seedC);
    }

    float motionSpeed = max(MeteorSpeed, 0.05);
    float headAzimuth = startAzimuth + (phase - 0.12) * horizontalSpeed * motionSpeed;
    float headAltitude = startAltitude - phase * verticalSpeed * motionSpeed;

    float azimuth = atan(direction.z, direction.x);
    vec2 relative = vec2(
        angleDifference(azimuth, headAzimuth),
        direction.y - headAltitude
    );
    vec2 trailDirection = normalize(vec2(-horizontalSpeed, verticalSpeed));
    float along = dot(relative, trailDirection);
    float across = abs(relative.x * trailDirection.y - relative.y * trailDirection.x);
    float styleSize = 1.0;
    float styleTrail = 1.0;
    if (styleIndex == 1) {
        styleSize = 0.68;
        styleTrail = 0.78;
    } else if (styleIndex == 2) {
        styleSize = 2.35;
        styleTrail = 0.72;
    } else if (styleIndex == 3) {
        styleSize = 1.28;
        styleTrail = 1.75;
    } else if (styleIndex == 4) {
        styleSize = 0.72;
        styleTrail = 2.45;
    } else if (styleIndex == 5) {
        styleSize = 0.52;
        styleTrail = 0.38;
    }
    float visualSize = max(MeteorSize * styleSize, 0.08);
    float visualTrail = max(MeteorTrail * styleTrail, 0.08);
    float trail = exp(-across * (320.0 / visualSize))
            * smoothstep(-0.008, 0.018, along)
            * (1.0 - smoothstep(0.06 * visualTrail, 0.34 * visualTrail, along));
    float head = exp(-length(relative) * (150.0 / visualSize));
    float halo = exp(-length(relative) * (34.0 / visualSize)) * MeteorGlow;
    float life = smoothstep(0.02, 0.13, phase) * (1.0 - smoothstep(0.72, 0.96, phase));
    if (styleIndex == 4) {
        life = smoothstep(0.01, 0.05, phase) * (1.0 - smoothstep(0.62, 0.86, phase));
    } else if (styleIndex == 5) {
        life *= 0.55 + 0.45 * sin(phase * 76.0 + seedC * 31.0);
    }
    return (trail + head * 1.8 + halo * 0.16) * life;
}

float meteorField(vec3 direction, int styleIndex) {
    float meteorTime = Time * 0.19 * max(MeteorFrequency, 0.05);
    float field = meteorLayer(direction, meteorTime, 0.0, styleIndex)
            + meteorLayer(direction, meteorTime, 0.37, styleIndex)
            + meteorLayer(direction, meteorTime, 0.71, styleIndex);
    if (styleIndex == 1 || styleIndex == 4 || styleIndex == 5) {
        field += meteorLayer(direction, meteorTime * 1.07, 0.16, styleIndex)
                + meteorLayer(direction, meteorTime * 0.93, 0.53, styleIndex)
                + meteorLayer(direction, meteorTime * 1.13, 0.84, styleIndex);
    }
    if (styleIndex == 1) {
        field += meteorLayer(direction, meteorTime * 1.21, 0.27, styleIndex)
                + meteorLayer(direction, meteorTime * 0.81, 0.64, styleIndex);
    }
    return field;
}

vec3 nebulaSky(vec3 direction, float t) {
    vec3 p = direction * 3.35;
    float warp = fbm(p * 0.72 + vec3(1.7, 6.3, 3.1));
    float mist = fbm(p + direction * (warp - 0.5) * 2.3);
    vec3 livingWarp = vec3(
        sin(t * 0.83 + p.y * 2.1 + warp * 3.4),
        cos(t * 0.71 + p.z * 1.8 - mist * 2.7),
        sin(t * 0.64 + p.x * 2.3 + mist * 3.1)
    ) * 0.22;
    float detail = fbm(p * 1.65 + vec3(mist * 1.8) + livingWarp);
    float ridge = pow(clamp(1.0 - abs(detail * 2.0 - 1.0), 0.0, 1.0), 4.2);
    float ribbonA = pow(clamp(1.0 - abs(sin((direction.x * 3.1 + direction.y * 1.7 + mist * 2.8) * 2.1)), 0.0, 1.0), 5.0);
    float ribbonB = pow(clamp(1.0 - abs(sin((direction.z * 2.6 - direction.y * 2.0 - warp * 3.2) * 1.8)), 0.0, 1.0), 6.0);
    float pulseA = 0.58 + 0.42 * sin(t * 1.45 + direction.y * 8.0 + mist * 5.0);
    float pulseB = 0.62 + 0.38 * cos(t * 1.12 - direction.x * 7.0 + warp * 4.2);
    float energy = clamp(ridge * 0.68 + ribbonA * (0.28 + pulseA * 0.32) + ribbonB * (0.18 + pulseB * 0.25), 0.0, 1.0);
    float horizon = pow(1.0 - abs(direction.y), 2.6);
    vec3 base = mix(vec3(0.006, 0.009, 0.025), vec3(0.028, 0.018, 0.072), horizon);
    vec3 primary = mix(TintColor, vec3(0.46, 0.16, 0.95), 0.32);
    vec3 secondary = mix(vec3(0.08, 0.58, 1.0), TintColor.bgr, 0.38);
    vec3 glow = mix(primary, secondary, clamp(mist * 0.9 + ribbonB * 0.35, 0.0, 1.0));
    float haze = smoothstep(0.38, 0.82, mist) * (0.11 + pulseB * 0.09);
    return base + glow * (energy * 0.92 + haze)
            + mix(primary, vec3(0.82, 0.92, 1.0), 0.52) * pow(energy, 3.0) * 0.34;
}

vec3 auroraSky(vec3 direction, float t) {
    vec3 d = normalize(direction);
    float horizon = smoothstep(-0.35, 0.42, d.y);

    // Keep all sampling in continuous 3D direction space. Using atan() here
    // creates a hard -PI/PI seam which is amplified into triangular wedges by
    // the sky dome interpolation.
    float broad = fbm(d * vec3(3.4, 2.6, 3.4)
            + vec3(t * 0.055, -t * 0.026, t * 0.041));
    float detail = fbm(d * 6.2
            + vec3(broad * 1.7, t * 0.09, -broad * 1.4));

    float phaseA = dot(d.xz, normalize(vec2(0.83, 0.56))) * 9.2
            + broad * 4.2 + t * 0.62;
    float phaseB = dot(d.xz, normalize(vec2(-0.44, 0.90))) * 6.6
            - detail * 3.8 - t * 0.43;
    float curtainA = pow(clamp(0.5 + 0.5 * sin(phaseA), 0.0, 1.0), 4.2);
    float curtainB = pow(clamp(0.5 + 0.5 * sin(phaseB), 0.0, 1.0), 5.0);

    float lowerEdge = 0.02 + (broad - 0.5) * 0.14;
    float upperEdge = 0.76 + (detail - 0.5) * 0.16;
    float vertical = smoothstep(lowerEdge - 0.09, lowerEdge + 0.10, d.y)
            * (1.0 - smoothstep(upperEdge - 0.16, upperEdge + 0.11, d.y));
    float folds = curtainA * (0.35 + detail * 0.65)
            + curtainB * (0.26 + broad * 0.38);
    float veil = smoothstep(0.38, 0.76, broad) * (0.22 + detail * 0.36);
    float aurora = vertical * (folds * 0.68 + veil * 0.28);

    vec3 green = mix(vec3(0.04, 1.0, 0.54), TintColor, 0.28);
    vec3 violet = mix(vec3(0.46, 0.12, 1.0), TintColor.bgr, 0.24);
    vec3 color = mix(vec3(0.003, 0.009, 0.026), vec3(0.012, 0.045, 0.068), horizon * 0.5);
    float colorBlend = clamp(curtainB * 0.58 + detail * 0.35, 0.0, 1.0);
    color += mix(green, violet, colorBlend) * aurora;
    color += mix(green, vec3(0.35, 0.75, 1.0), 0.45) * vertical * veil * 0.16;
    color += vec3(0.68, 0.84, 1.0) * starField(d, 92.0, 0.992) * 1.35;
    return color;
}

vec3 spaceSky(vec3 direction, float t) {
    float cloud = fbm(direction * 4.8 + vec3(t * 0.025, -t * 0.018, t * 0.012));
    float band = pow(max(0.0, 1.0 - abs(direction.y * 2.2 + direction.x * 0.55 + (cloud - 0.5) * 0.8)), 4.0);
    vec3 nebula = mix(TintColor * vec3(0.34, 0.18, 0.58), vec3(0.08, 0.42, 0.78), cloud);
    vec3 color = vec3(0.0015, 0.0025, 0.011) + nebula * band * (0.28 + cloud * 0.62);
    float stars = starField(direction, 78.0, 0.986) + starField(direction, 151.0, 0.996) * 0.72;
    return color + mix(vec3(0.62, 0.76, 1.0), vec3(1.0, 0.82, 0.62), cloud) * stars * 2.1;
}

vec3 stormSky(vec3 direction, float t) {
    vec3 flow = vec3(t * 0.11, -t * 0.025, t * 0.075);
    float broad = fbm(direction * 2.7 + flow);
    float detail = fbm(direction * 7.1 - flow * 1.7 + broad * 1.8);
    float cloud = smoothstep(0.28, 0.82, broad * 0.64 + detail * 0.52);
    float underside = pow(1.0 - clamp(direction.y * 0.7 + 0.35, 0.0, 1.0), 2.2);
    vec3 steel = mix(vec3(0.018, 0.025, 0.038), vec3(0.15, 0.18, 0.23), cloud);
    steel += mix(TintColor, vec3(0.28, 0.38, 0.52), 0.72) * cloud * underside * 0.16;
    return steel;
}

vec3 sunsetSky(vec3 direction, float t) {
    float height = clamp(direction.y * 0.5 + 0.5, 0.0, 1.0);
    float horizon = exp(-abs(direction.y) * 5.8);
    vec3 low = mix(vec3(1.0, 0.11, 0.035), TintColor, 0.22);
    vec3 high = mix(vec3(0.025, 0.055, 0.23), TintColor * 0.42, 0.34);
    vec3 color = mix(low, high, smoothstep(0.28, 0.78, height));
    float wisps = fbm(direction * 6.0 + vec3(t * 0.08, 0.0, -t * 0.12));
    color += vec3(1.0, 0.42, 0.14) * horizon * (0.42 + wisps * 0.34);
    color += mix(TintColor, vec3(1.0, 0.24, 0.38), 0.58)
            * pow(smoothstep(0.52, 0.86, wisps), 2.0) * horizon * 0.55;
    return color;
}

vec3 abyssSky(vec3 direction, float t) {
    float angle = atan(direction.z, direction.x);
    float radius = acos(clamp(direction.y, -1.0, 1.0));
    float spiral = sin(angle * 5.0 + radius * 12.0 - t * 0.68 + fbm(direction * 4.0) * 4.0);
    float vein = pow(0.5 + 0.5 * spiral, 10.0);
    float voidGlow = exp(-abs(direction.y + 0.18) * 4.2);
    vec3 color = vec3(0.0008, 0.0003, 0.004);
    color += mix(vec3(0.12, 0.015, 0.32), TintColor * 0.48, 0.44) * vein * (0.18 + voidGlow);
    color += vec3(0.18, 0.02, 0.38) * voidGlow * 0.08;
    return color;
}

vec3 eventHorizonSky(vec3 direction, float t) {
    vec3 center = normalize(vec3(0.52, 0.20, 0.83));
    float angle = acos(clamp(dot(direction, center), -1.0, 1.0));
    float ring = exp(-pow(abs(angle - 0.205) * 34.0, 2.0));
    float lens = exp(-pow(abs(angle - 0.29) * 13.0, 2.0));
    float turbulence = fbm(direction * 8.0 + vec3(t * 0.14, -t * 0.08, t * 0.11));
    float rotation = 0.62 + 0.38 * sin(atan(direction.z - center.z, direction.x - center.x) * 9.0 - t * 2.1);
    vec3 stars = vec3(0.34, 0.48, 0.82) * starField(direction, 132.0, 0.994) * 1.8;
    vec3 accretion = mix(vec3(1.0, 0.18, 0.025), TintColor + vec3(0.28, 0.08, 0.02), 0.34);
    vec3 color = vec3(0.0005, 0.0008, 0.004) + stars;
    color += accretion * ring * (1.2 + turbulence * 1.7) * rotation;
    color += mix(TintColor, vec3(0.18, 0.42, 1.0), 0.52) * lens * 0.22;
    color *= smoothstep(0.105, 0.155, angle);
    color += vec3(0.82, 0.90, 1.0) * exp(-angle * 26.0) * 0.08;
    return color;
}

vec3 bloodEclipseSky(vec3 direction, float t) {
    vec3 moonDirection = normalize(vec3(-0.46, 0.34, 0.82));
    float moonAngle = acos(clamp(dot(direction, moonDirection), -1.0, 1.0));
    float moon = 1.0 - smoothstep(0.142, 0.158, moonAngle);
    float rim = exp(-pow(abs(moonAngle - 0.153) * 72.0, 2.0));
    float clouds = fbm(direction * 5.3 + vec3(t * 0.05, 0.0, -t * 0.09));
    float smokyBand = smoothstep(0.42, 0.78, clouds) * exp(-abs(direction.y) * 2.4);
    vec3 crimson = mix(vec3(0.72, 0.015, 0.008), TintColor * vec3(1.0, 0.18, 0.12), 0.24);
    vec3 color = mix(vec3(0.004, 0.001, 0.006), vec3(0.032, 0.003, 0.009), smokyBand);
    color += crimson * moon * (0.34 + clouds * 0.36);
    color += vec3(1.0, 0.12, 0.025) * rim * (0.72 + 0.28 * sin(t * 1.4));
    color += crimson * smokyBand * 0.16;
    color += vec3(0.82, 0.58, 0.52) * starField(direction, 105.0, 0.995) * 0.72;
    return color;
}

vec3 cloudOceanSky(vec3 direction, float t) {
    float horizon = exp(-abs(direction.y + 0.02) * 4.3);
    float broad = fbm(direction * 3.0 + vec3(t * 0.045, 0.0, -t * 0.072));
    float detail = fbm(direction * 8.2 + vec3(-t * 0.08, broad * 1.8, t * 0.035));
    float cloud = smoothstep(0.38, 0.81, broad * 0.68 + detail * 0.45);
    float sea = 1.0 - smoothstep(-0.38, 0.22, direction.y);
    vec3 zenith = mix(vec3(0.018, 0.14, 0.34), TintColor * 0.42 + vec3(0.02, 0.10, 0.18), 0.30);
    vec3 color = zenith * (0.45 + max(direction.y, 0.0) * 0.65);
    color = mix(color, vec3(0.16, 0.32, 0.48), horizon * 0.72);
    color += mix(vec3(0.26, 0.38, 0.52), vec3(0.86, 0.91, 0.98), detail) * cloud * (0.26 + sea * 0.82);
    color += mix(TintColor, vec3(1.0, 0.58, 0.28), 0.72) * horizon * 0.18;
    return color;
}

vec3 crystalRiftSky(vec3 direction, float t) {
    float warp = fbm(direction * 4.6 + vec3(t * 0.08, -t * 0.05, t * 0.03));
    float cutA = abs(sin((direction.x * 4.7 + direction.y * 2.3 + warp * 1.8) * 3.2));
    float cutB = abs(sin((direction.z * 5.1 - direction.y * 3.1 - warp * 2.2) * 2.8));
    float rift = pow(1.0 - min(cutA, cutB), 18.0);
    float facets = pow(abs(sin(direction.x * 19.0 + direction.z * 17.0 + warp * 5.0)), 12.0);
    vec3 cold = mix(vec3(0.08, 0.72, 1.0), TintColor, 0.52);
    vec3 color = vec3(0.002, 0.006, 0.022) + cold * rift * (1.0 + facets * 1.4);
    color += mix(cold, vec3(0.92, 0.98, 1.0), 0.68) * pow(rift, 3.0) * 1.5;
    color += vec3(0.48, 0.68, 1.0) * starField(direction, 118.0, 0.993) * 1.1;
    return color;
}

vec3 synthwaveSky(vec3 direction, float t) {
    float azimuth = atan(direction.z, direction.x);
    float height = clamp(direction.y * 0.5 + 0.5, 0.0, 1.0);
    float horizon = exp(-abs(direction.y + 0.06) * 7.2);
    vec3 color = mix(vec3(0.32, 0.012, 0.28), vec3(0.004, 0.018, 0.105), smoothstep(0.30, 0.82, height));
    float stripes = pow(0.5 + 0.5 * sin(direction.y * 92.0 + t * 0.7), 18.0) * horizon;
    float longitudeGrid = pow(1.0 - abs(sin(azimuth * 12.0)), 28.0);
    float latitudeGrid = pow(1.0 - abs(sin((direction.y + 0.34) * 34.0)), 26.0);
    float grid = (longitudeGrid + latitudeGrid) * (1.0 - smoothstep(-0.55, 0.22, direction.y));
    float sunDistance = length(vec2(angleDifference(azimuth, -0.65), direction.y - 0.06));
    float sun = 1.0 - smoothstep(0.22, 0.235, sunDistance);
    color += mix(vec3(1.0, 0.08, 0.66), TintColor, 0.28) * (horizon * 0.58 + stripes * 0.72);
    color += vec3(1.0, 0.26, 0.56) * sun * (0.72 + stripes * 0.55);
    color += mix(vec3(0.04, 0.72, 1.0), TintColor, 0.38) * grid * 0.52;
    return color;
}

vec3 solarCrownSky(vec3 direction, float t) {
    vec3 sunDirection = normalize(vec3(0.26, 0.46, 0.85));
    float sunAngle = acos(clamp(dot(direction, sunDirection), -1.0, 1.0));
    float disk = 1.0 - smoothstep(0.115, 0.135, sunAngle);
    float coronaNoise = fbm(direction * 13.0 + vec3(t * 0.12, -t * 0.10, t * 0.07));
    float corona = exp(-sunAngle * mix(6.2, 10.0, coronaNoise));
    float rays = pow(0.5 + 0.5 * sin(atan(direction.y - sunDirection.y, direction.x - sunDirection.x) * 19.0 + coronaNoise * 5.0 - t), 8.0);
    vec3 gold = mix(vec3(1.0, 0.32, 0.025), TintColor + vec3(0.36, 0.10, 0.0), 0.24);
    vec3 color = vec3(0.003, 0.008, 0.024) + vec3(0.18, 0.32, 0.58) * max(direction.y, 0.0) * 0.20;
    color += gold * corona * (0.42 + rays * 0.72);
    color += vec3(1.0, 0.92, 0.62) * disk * 2.1;
    return color;
}

vec3 emeraldDreamSky(vec3 direction, float t) {
    float flow = fbm(direction * 3.8 + vec3(t * 0.07, t * 0.025, -t * 0.09));
    float vines = pow(0.5 + 0.5 * sin(direction.x * 13.0 - direction.z * 9.0 + flow * 7.0 + t * 0.35), 12.0);
    float mist = smoothstep(0.34, 0.76, flow) * exp(-abs(direction.y) * 1.8);
    vec3 emerald = mix(vec3(0.015, 0.88, 0.38), TintColor, 0.32);
    vec3 color = mix(vec3(0.001, 0.012, 0.009), vec3(0.012, 0.062, 0.038), max(direction.y, 0.0));
    color += emerald * vines * (0.18 + mist * 0.88);
    color += mix(emerald, vec3(0.64, 1.0, 0.74), 0.64) * mist * 0.25;
    color += vec3(0.72, 1.0, 0.78) * starField(direction, 86.0, 0.991) * 1.35;
    return color;
}

vec3 cosmicStormSky(vec3 direction, float t) {
    vec3 flow = vec3(t * 0.12, -t * 0.07, t * 0.10);
    float broad = fbm(direction * 3.4 + flow);
    float detail = fbm(direction * 8.8 - flow * 1.7 + broad * 2.2);
    float vortex = sin(atan(direction.z, direction.x) * 7.0 + acos(clamp(direction.y, -1.0, 1.0)) * 11.0 - t * 1.25 + broad * 5.0);
    float energy = pow(0.5 + 0.5 * vortex, 9.0) * smoothstep(0.32, 0.78, detail);
    float cloud = smoothstep(0.25, 0.82, broad * 0.62 + detail * 0.48);
    vec3 plasma = mix(TintColor, vec3(0.10, 0.52, 1.0), detail);
    vec3 color = vec3(0.002, 0.004, 0.016) + plasma * cloud * 0.24;
    color += mix(plasma, vec3(0.92, 0.96, 1.0), 0.66) * energy * 1.38;
    color += vec3(0.54, 0.70, 1.0) * starField(direction, 142.0, 0.996) * 0.9;
    return color;
}

void main() {
    vec3 direction = normalize(skyDir);
    float t = Time * Speed;
    int styleIndex = int(floor(Style + 0.5));
    vec3 color;
    if (styleIndex == 1) {
        color = auroraSky(direction, t);
    } else if (styleIndex == 2) {
        color = spaceSky(direction, t);
    } else if (styleIndex == 3) {
        color = stormSky(direction, t);
    } else if (styleIndex == 4) {
        color = sunsetSky(direction, t);
    } else if (styleIndex == 5) {
        color = abyssSky(direction, t);
    } else if (styleIndex == 6) {
        color = eventHorizonSky(direction, t);
    } else if (styleIndex == 7) {
        color = bloodEclipseSky(direction, t);
    } else if (styleIndex == 8) {
        color = cloudOceanSky(direction, t);
    } else if (styleIndex == 9) {
        color = crystalRiftSky(direction, t);
    } else if (styleIndex == 10) {
        color = synthwaveSky(direction, t);
    } else if (styleIndex == 11) {
        color = solarCrownSky(direction, t);
    } else if (styleIndex == 12) {
        color = emeraldDreamSky(direction, t);
    } else if (styleIndex == 13) {
        color = cosmicStormSky(direction, t);
    } else {
        color = nebulaSky(direction, t);
    }
    if (MeteorEnabled > 0.5) {
        int meteorStyleIndex = int(floor(MeteorStyle + 0.5));
        float meteor = meteorField(direction, meteorStyleIndex);
        vec3 meteorTint = MeteorColor;
        if (meteorStyleIndex == 2) {
            meteorTint = mix(MeteorColor, vec3(1.0, 0.22, 0.025), 0.64);
        } else if (meteorStyleIndex == 3) {
            meteorTint = mix(MeteorColor, vec3(0.32, 0.82, 1.0), 0.58);
        } else if (meteorStyleIndex == 4) {
            meteorTint = mix(MeteorColor, vec3(0.72, 0.34, 1.0), 0.42);
        } else if (meteorStyleIndex == 5) {
            meteorTint = mix(MeteorColor, vec3(1.0, 0.66, 0.18), 0.48);
        }
        color += meteorTint * meteor * 3.4 * max(MeteorBrightness, 0.01);
    }
    color *= max(Intensity, 0.01);
    color *= 0.94 + 0.06 * smoothstep(0.0, 0.85, direction.y);

    fragColor = vec4(color, 1.0);
}
