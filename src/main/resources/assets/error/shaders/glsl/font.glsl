#version 120

uniform sampler2D font;
uniform vec4 inColor;
uniform vec2 TextureSize;
uniform float Range;
uniform float EdgeStrength;
uniform float width;
uniform float maxWidth;

void main() {
    vec2 pos = gl_TexCoord[0].xy;
    vec4 tex = texture2D(font, pos);
    float dx = dFdx(pos.x) * TextureSize.x;
    float dy = dFdy(pos.y) * TextureSize.y;
    float toPixels = Range * inversesqrt(dx * dx + dy * dy);
    float pseudoDist = tex.a - 0.5;
    float a = smoothstep(-EdgeStrength, EdgeStrength, pseudoDist * toPixels);
    vec4 vertColor = gl_Color;
    if (width > 0.0 && maxWidth > 0.0) {
        float f = clamp(smoothstep(0.5, 1.0, 1.0 - (gl_FragCoord.x - maxWidth) / width), 0.0, 1.0);
        a = a * f;
    }
    gl_FragColor = vec4(vertColor.rgb * inColor.rgb, vertColor.a * inColor.a * a);
}
