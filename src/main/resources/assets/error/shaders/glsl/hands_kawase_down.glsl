#version 120

uniform sampler2D Sampler0;
uniform vec2 uOffset;
uniform vec2 uHalfPixel;
uniform vec2 uSize;

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    vec2 halfPixel = uHalfPixel * uOffset;

    vec4 sum = texture2D(Sampler0, uv) * 4.0;
    sum += texture2D(Sampler0, uv - halfPixel);
    sum += texture2D(Sampler0, uv + halfPixel);
    sum += texture2D(Sampler0, uv + vec2(halfPixel.x, -halfPixel.y));
    sum += texture2D(Sampler0, uv - vec2(halfPixel.x, -halfPixel.y));

    gl_FragColor = sum / 8.0;
}
