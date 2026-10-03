#version 130

uniform float time;
uniform vec2 speed;
uniform vec3 themeColor;
uniform float alpha;
uniform int colorMode;
uniform vec3 cameraOffset;
uniform float breakProgress;
uniform float blockMinY;
uniform float blockMaxY;

uniform vec2 resolution;
uniform vec2 oneTexel;
uniform vec4 color;
uniform int quality;

varying vec3 vPos;

mat2 rotate2D(float r)
{
    return mat2(cos(r), sin(r), -sin(r), cos(r));
}

vec3 getEffectColor(vec2 frag)
{
    vec2 uv = 0.33 * (frag - 0.5 * resolution) / resolution.y;

    float t = time * speed.x;

    vec2 n = vec2(0.0);
    vec2 q = vec2(0.0);
    vec2 p = uv * 2.5;

    float d = dot(p, p);
    float S = 16.0;
    float a = 0.0;

    mat2 m = rotate2D(15.0 + sin(d * 0.1 + t * 0.1) * 2.0);

    for (float j = 0.0; j < 6.0; j++)
    {
        p *= m * 1.05;
        n *= m;

        q = p * S + t * 2.5 + sin(t + j) * 0.0018 + 3.0 * j - 1.25 * n;

        a += dot(cos(q) / S, vec2(0.15));

        n -= sin(q);
        S *= 1.5;
    }

    vec3 effect = vec3(2.5, 1.9, 3.5) * (a + 0.182) + 9.0 * a + a;

    return effect * color.rgb;
}

void main()
{
    vec3 worldPos = vPos + cameraOffset;

    float heightFrac = 1.0;

    if (breakProgress > 0.001)
    {
        float heightSpan = blockMaxY - blockMinY + 0.0001;
        heightFrac = (worldPos.y - blockMinY) / heightSpan;
    }

    vec3 col = getEffectColor(gl_FragCoord.xy);

    col = clamp(col, 0.0, 1.0);

    float finalAlpha = alpha;

    if (breakProgress > 0.001 && heightFrac < breakProgress)
    {
        col = mix(col, col * vec3(1.0, 0.15, 0.15), 0.7);
        finalAlpha = clamp(finalAlpha + 0.2, 0.0, 1.0);
    }

    gl_FragColor = vec4(col, finalAlpha);
}