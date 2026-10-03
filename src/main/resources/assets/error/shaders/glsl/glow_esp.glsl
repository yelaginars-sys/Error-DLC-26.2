#version 120

uniform sampler2D textureIn;
uniform vec2 texelSize, direction;
uniform vec4 color;
uniform bool avoidTexture;
uniform float exposure, radius, fillTint;
uniform float weights[8];

#define offset direction * texelSize
#define MAX_R 6

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec4 centerTex = texture2D(textureIn, uv);

    if (direction.y == 1.0 && avoidTexture) {
        if (centerTex.a != 0.0) discard;
    }

    if (radius <= 0.0) {
        vec3 finalRgb = centerTex.rgb + (color.rgb * fillTint);
        gl_FragColor = vec4(finalRgb, centerTex.a * color.a);
        return;
    }

    if (radius < 0.1) {
        float outline = 0.0;

        outline += texture2D(textureIn, uv + vec2(texelSize.x, 0.0)).a;
        outline += texture2D(textureIn, uv - vec2(texelSize.x, 0.0)).a;
        outline += texture2D(textureIn, uv + vec2(0.0, texelSize.y)).a;
        outline += texture2D(textureIn, uv - vec2(0.0, texelSize.y)).a;
        
        if (centerTex.a < 0.1 && outline > 0.1) {
            gl_FragColor = vec4(color.rgb, color.a);
        } else {
            vec3 finalRgb = centerTex.rgb + (color.rgb * fillTint);
            gl_FragColor = vec4(finalRgb, centerTex.a * color.a);
        }
        return;
    }

    float rCap = min(radius, float(MAX_R));
    float innerAlpha = centerTex.a * weights[0];
    vec3 colorSum = centerTex.rgb * centerTex.a * weights[0];

    for (float r = 1.0; r <= rCap; r += 1.0) {
        int ri = int(r);
        vec4 t1 = texture2D(textureIn, uv + offset * r);
        vec4 t2 = texture2D(textureIn, uv - offset * r);
        innerAlpha += (t1.a + t2.a) * weights[ri];
        colorSum += (t1.rgb * t1.a + t2.rgb * t2.a) * weights[ri];
    }

    vec3 outRgb = colorSum / max(innerAlpha, 0.001);
    gl_FragColor = vec4(outRgb, color.a * min(innerAlpha, 1.0) * exposure);
}