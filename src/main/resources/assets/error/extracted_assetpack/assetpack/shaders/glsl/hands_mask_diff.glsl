#version 120

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    vec4 before = texture2D(Sampler0, uv);
    vec4 after = texture2D(Sampler1, uv);

    vec3 colorDiff = abs(after.rgb - before.rgb);
    float maxColorDiff = max(max(colorDiff.r, colorDiff.g), colorDiff.b);
    float isHand = maxColorDiff > 0.01 ? 1.0 : 0.0;

    float topDistance = 1.0 - uv.y;
    isHand *= smoothstep(0.012, 0.075, topDistance);

    gl_FragColor = vec4(isHand, isHand, isHand, isHand);
}
