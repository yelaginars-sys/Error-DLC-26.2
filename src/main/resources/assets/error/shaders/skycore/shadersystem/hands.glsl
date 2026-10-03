#version 120

uniform sampler2D originalTexture;
uniform sampler2D blurredTexture;
uniform vec4 multiplier;
uniform vec2 resolution;
uniform float mixFactor;

void main() {
    vec4 srcColor = texture2D(originalTexture, gl_TexCoord[0].xy);
    if (srcColor.a == 0.0) discard;
    vec2 uv = gl_FragCoord.xy / resolution;
    uv.y = 1.0 - uv.y;
    vec4 blurredColor = texture2D(blurredTexture, uv);
    vec3 tintedGlass = blurredColor.rgb * multiplier.rgb;
    vec3 finalRGB = mix(tintedGlass, srcColor.rgb, mixFactor);
    gl_FragColor = vec4(finalRGB, srcColor.a);
}
