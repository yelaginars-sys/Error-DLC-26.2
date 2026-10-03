#version 120

uniform float time;
uniform vec2 resolution;
uniform vec3 color;
uniform float intensity;
uniform float speed;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n = i.x + i.y * 57.0;
    return mix(mix(hash(n + 0.0), hash(n + 1.0), f.x),
               mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec2 p = uv;

    p.y = 1.0 - p.y;

    float rayIntensity = 0.0;

    float waveOffset = sin(time * speed * 0.8) * 0.1;
    float verticalFlow = fract(p.y + time * speed * 0.4) * 2.0 - 1.0;
    
    for (int i = 0; i < 10; i++) {
        float baseRayX = float(i) / 9.0;

        float rayX = baseRayX + sin(time * speed * 0.6 + float(i) * 0.5) * 0.05;
        float dist = abs(p.x - rayX);

        float ray = smoothstep(0.2, 0.0, dist);
        ray *= smoothstep(0.0, 0.5, p.y);
        ray *= smoothstep(1.0, 0.7, p.y);

        float wave = sin((p.y + time * speed * 0.5) * 8.0 + float(i) * 2.0) * 0.5 + 0.5;
        ray *= wave * 0.7 + 0.3;
        
        rayIntensity += ray * 0.25;
    }

    vec2 noiseCoord = vec2(p.x * 2.5 + time * speed * 0.3, p.y * 4.0 - time * speed * 0.6);
    float noiseValue = fbm(noiseCoord);
    rayIntensity += noiseValue * 0.12 * smoothstep(0.0, 0.5, p.y) * smoothstep(1.0, 0.6, p.y);

    float verticalGlow = sin(p.x * 18.0 + time * speed * 0.8) * 0.5 + 0.5;
    verticalGlow *= sin((p.y - time * speed * 0.3) * 6.0) * 0.3 + 0.7;
    verticalGlow *= smoothstep(0.0, 0.4, p.y);
    verticalGlow *= smoothstep(1.0, 0.6, p.y);
    rayIntensity += verticalGlow * 0.15;

    float gradient = smoothstep(0.0, 0.5, p.y) * smoothstep(1.0, 0.4, p.y);
    float waveGradient = sin((p.y - time * speed * 0.2) * 4.0) * 0.2 + 0.8;
    gradient *= waveGradient;
    rayIntensity += gradient * 0.2;

    for (int j = 0; j < 3; j++) {
        float spotX = 0.2 + float(j) * 0.3 + sin(time * speed * 0.4 + float(j)) * 0.15;
        float spotY = fract(time * speed * 0.25 + float(j) * 0.33) * 1.2 - 0.1;
        float spotDist = distance(p, vec2(spotX, spotY));
        float spot = smoothstep(0.3, 0.0, spotDist);
        spot *= smoothstep(0.0, 0.3, p.y);
        rayIntensity += spot * 0.1;
    }

    float edgeSmoothX = smoothstep(1.0, 0.9, abs(p.x - 0.5) * 2.0);
    float edgeSmoothY = smoothstep(1.0, 0.95, p.y);
    float edgeSmooth = min(edgeSmoothX, edgeSmoothY);
    rayIntensity *= edgeSmooth;

    vec3 finalColor = color * rayIntensity * intensity;

    float edgeGlow = smoothstep(0.98, 0.92, abs(p.x - 0.5) * 2.0);
    edgeGlow *= smoothstep(0.0, 0.3, p.y);
    edgeGlow *= sin(time * speed * 0.5) * 0.3 + 0.7;
    finalColor += color * edgeGlow * 0.08 * intensity;

    float alpha = rayIntensity * intensity * 0.4;
    gl_FragColor = vec4(finalColor, alpha);
}

