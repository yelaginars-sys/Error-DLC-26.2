#version 120

uniform sampler2D image;
uniform float offset;
uniform vec2 resolution;

void main() {
    vec2 uv = gl_TexCoord[0].xy * 2.0;
    vec2 halfpixel = resolution * 2.0 * offset;
    vec3 sum = texture2D(image, uv).rgb * 4.0;
    sum += texture2D(image, uv - halfpixel).rgb;
    sum += texture2D(image, uv + halfpixel).rgb;
    sum += texture2D(image, uv + vec2(halfpixel.x, -halfpixel.y)).rgb;
    sum += texture2D(image, uv - vec2(halfpixel.x, -halfpixel.y)).rgb;
    gl_FragColor = vec4(sum / 8.0, 1.0);
}
