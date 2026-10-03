#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform float radius;

const int R = 2;

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec4 center = texture2D(texture, uv);
    if (center.a > 0.0) discard;

    float sum = 0.0;
    vec3 sumRgb = vec3(0.0);

    for (int dx = -R; dx <= R; dx++) {
        for (int dy = -R; dy <= R; dy++) {
            vec2 off = uv + vec2(float(dx), float(dy)) * texelSize;
            vec4 s = texture2D(texture, off);
            s.rgb *= s.a;
            float d = length(vec2(float(dx), float(dy)));
            float r = max(radius, 0.5);
            float w = max(0.0, 1.0 - d / r);
            sum += s.a * w;
            sumRgb += s.rgb * w;
        }
    }

    if (sum < 0.001) discard;
    vec3 rgb = sumRgb / sum * color;
    float a = min(1.0, sum * 0.35);
    gl_FragColor = vec4(rgb, a);
}
