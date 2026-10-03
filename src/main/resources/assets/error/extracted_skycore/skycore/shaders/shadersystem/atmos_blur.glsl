#version 150 core

in vec2 fragCoord;

uniform sampler2D inputTexture;
uniform vec2 texelSize;
uniform vec2 direction;

out vec4 fragColor;

void main() {
    vec2 uv = vec2(fragCoord.x, 1.0 - fragCoord.y);
    vec2 offset = texelSize * direction;

    vec3 sum = texture(inputTexture, uv).rgb * 0.227027;
    sum += texture(inputTexture, uv + offset * 1.3846153846).rgb * 0.3162162162;
    sum += texture(inputTexture, uv - offset * 1.3846153846).rgb * 0.3162162162;
    sum += texture(inputTexture, uv + offset * 3.2307692308).rgb * 0.0702702703;
    sum += texture(inputTexture, uv - offset * 3.2307692308).rgb * 0.0702702703;

    fragColor = vec4(sum, 1.0);
}
