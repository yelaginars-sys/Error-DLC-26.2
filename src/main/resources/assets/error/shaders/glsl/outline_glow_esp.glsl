#version 120

uniform vec2 texelSize, direction;
uniform sampler2D texture;
uniform float radius;
uniform vec3 color;

#define offset direction * texelSize
#define MAX_R 6

void main() {
    vec4 centerTex = texture2D(texture, gl_TexCoord[0].xy);
    float centerAlpha = centerTex.a;
    float rCap = min(radius, float(MAX_R));
    float innerAlpha = centerAlpha;
    vec3 colorSum = centerTex.rgb * centerAlpha;
    for (float r = 1.0; r <= rCap; r += 1.0) {
        vec4 t1 = texture2D(texture, gl_TexCoord[0].xy + offset * r);
        vec4 t2 = texture2D(texture, gl_TexCoord[0].xy - offset * r);
        innerAlpha += t1.a + t2.a;
        colorSum += t1.rgb * t1.a + t2.rgb * t2.a;
    }
    vec3 outColor = colorSum / max(innerAlpha, 0.001);
    gl_FragColor = vec4(outColor, innerAlpha) * step(0.0, -centerAlpha);
}