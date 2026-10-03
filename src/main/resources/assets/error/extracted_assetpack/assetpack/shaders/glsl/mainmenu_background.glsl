#version 120

#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform vec3 themeColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec2 p = (gl_FragCoord.xy - 0.5 * resolution.xy) / max(resolution.x, resolution.y);
    
    float t = time * 0.1;
    
    vec3 color = vec3(0.02, 0.02, 0.035);
    
    for(float i = 1.0; i <= 3.0; i++) {
        float speed = t * (0.8 + i * 0.2);
        vec2 pos = vec2(
            sin(speed + i * 1.5) * 0.5,
            cos(speed * 0.7 + i * 2.0) * 0.4
        );
        
        float dist = length(p - pos);
        float radius = 0.6 + sin(t * 0.1 + i) * 0.2;
        float strength = smoothstep(radius, 0.0, dist);
        
        color += themeColor * strength * (0.12 / i);
    }
    
    float flow = 0.0;
    for(float i = 1.0; i <= 4.0; i++) {
        float layerT = t * (1.0 + i * 0.1);
        float line = 0.0018 / abs(p.y + sin(p.x * (1.1 + i * 0.12) + layerT + i) * 0.15 + (i - 2.5) * 0.12);
        flow += line;
    }
    color += themeColor * clamp(flow, 0.0, 1.0) * 0.22;
    
    float bottomGlow = pow(1.0 - uv.y, 3.0) * 0.15;
    color += themeColor * bottomGlow;
    
    float dist = length(p);
    float vig = smoothstep(1.3, 0.3, dist);
    color *= (0.7 + 0.3 * vig);
    
    float g = (hash(gl_FragCoord.xy) - 0.5) * 0.015;
    color += vec3(g);
    
    gl_FragColor = vec4(color, 1.0);
}
