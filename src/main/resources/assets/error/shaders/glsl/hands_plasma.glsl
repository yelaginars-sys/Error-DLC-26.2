#version 130

uniform sampler2D textureIn;
uniform float time;
uniform vec2 resolution;
uniform vec4 color;
uniform float alpha;

mat2 rotate2D(float r) {
    return mat2(cos(r), sin(r), -sin(r), cos(r));
}

vec3 getEffectColor(vec2 frag) {
    vec2 uv = 0.33 * (frag - 0.5 * resolution) / resolution.y;
    float t = time;
    vec2 n = vec2(0.0);
    vec2 q = vec2(0.0);
    vec2 p = uv * 2.5;
    float d = dot(p, p);
    float S = 16.0;
    float a = 0.0;
    mat2 m = rotate2D(15.0 + sin(d * 0.1 + t * 0.1) * 2.0);
    for (float j = 0.0; j < 6.0; j++) {
        p *= m * 1.05;
        n *= m;
        q = p * S + t * 2.5 + sin(t + j) * 0.0018 + 3.0 * j - 1.25 * n;
        a += dot(cos(q) / S, vec2(0.15));
        n -= sin(q);
        S *= 1.5;
    }
    vec3 effect = vec3(2.5, 1.9, 3.5) * (a + 0.182) + 9.0 * a + a;
    return effect;
}

void main() {
    vec4 srcColor = texture2D(textureIn, gl_TexCoord[0].xy);
    if (srcColor.a < 0.01) {
        discard;
    }
    
    vec3 effect = getEffectColor(gl_FragCoord.xy);
    vec3 plasmaColor = effect * color.rgb;
    
    // Смешиваем оригинальную текстуру с эффектом плазмы
    // Чем выше alpha, тем больше плазмы
    vec3 finalRgb = mix(srcColor.rgb, plasmaColor, 0.45);
    
    // Добавляем немного свечения плазмы поверх
    finalRgb += plasmaColor * 0.25;
    
    gl_FragColor = vec4(finalRgb, alpha * srcColor.a);
}
