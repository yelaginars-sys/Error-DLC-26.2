#version 120

uniform sampler2D Sampler0;
uniform vec2 uOffset;
uniform vec2 uHalfPixel;
uniform vec2 uSize;
uniform vec3 color;

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    vec2 halfPixel = uHalfPixel * uOffset;

    vec4 sum = texture2D(Sampler0, uv + vec2(-halfPixel.x * 2.0, 0.0));
    sum += texture2D(Sampler0, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture2D(Sampler0, uv + vec2(0.0, halfPixel.y * 2.0));
    sum += texture2D(Sampler0, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture2D(Sampler0, uv + vec2(halfPixel.x * 2.0, 0.0));
    sum += texture2D(Sampler0, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
    sum += texture2D(Sampler0, uv + vec2(0.0, -halfPixel.y * 2.0));
    sum += texture2D(Sampler0, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;

    vec4 result = sum / 12.0;
    gl_FragColor = vec4(result.rgb * color, result.a);
}
