#version 120

uniform vec2 u_resolution;
uniform float u_time;
uniform vec4 u_colors[2];
uniform float u_intensity;
uniform float u_rays;
uniform float u_reach;

// Функция для создания микро-шума (дизеринг), чтобы убрать полосы на градиенте
float random(vec2 coords) {
    return fract(sin(dot(coords, vec2(12.9898, 78.233))) * 43758.5453);
}

float rayStrength(vec2 raySource, vec2 rayRefDirection, vec2 coord, float seedA, float seedB, float speed) {
    vec2 sourceToCoord = coord - raySource;
    float dist = length(sourceToCoord) / u_resolution.y;
    vec2 dir = normalize(sourceToCoord);
    
    // Используем плавный dot product
    float cosAngle = dot(dir, rayRefDirection);

    // Смягчаем синусоиду: возводим в степень, чтобы убрать "серые" грязные зоны
    float wave = (0.5 + 0.5 * sin(cosAngle * seedA + u_time * speed));
    wave = pow(wave, 2.0); // Делает лучи четче у центра, но мягче к краям

    // Экспоненциальное затухание (самое мягкое из возможных)
    float falloff = exp(-dist * (5.0 / u_reach));
    
    return wave * falloff;
}

void main() {
    vec2 coord = gl_FragCoord.xy;
    float speed = u_rays * 3.0; 

    // Центр лучей чуть ниже экрана
    vec2 rayPos = vec2(u_resolution.x * 0.5, u_resolution.y * -0.4);

    float r1 = rayStrength(rayPos, normalize(vec2(0.3, 1.0)), coord, 20.0, 10.0, 1.5 * speed);
    float r2 = rayStrength(rayPos, normalize(vec2(-0.3, 1.0)), coord, 15.0, 8.0, 1.0 * speed);

    // Смешиваем лучи и применяем общую интенсивность
    float combined = (r1 + r2) * u_intensity;

    // Добавляем дизеринг (микро-шум), чтобы скрыть ступенчатость переходов
    combined += (random(coord) - 0.5) * 0.005;

    vec3 finalRGB = mix(u_colors[0].rgb, u_colors[1].rgb, r2 / (r1 + r2 + 0.001));
    
    gl_FragColor = vec4(finalRGB, combined);
}