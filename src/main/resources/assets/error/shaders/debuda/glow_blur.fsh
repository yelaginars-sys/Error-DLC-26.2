#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Direction; // (1,0) для горизонтального прохода, (0,1) для вертикального
uniform float Radius;   // в пикселях

out vec4 OutColor;

// 9-tap separable gaussian (sigma ~ Radius/3). Веса нормированы.
void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 offset = Direction * texelSize * Radius;

    // Exact 9-weight Gaussian folded into five hardware-linear samples. Fractional
    // taps remove the spaced bands that were visible on high-contrast item pixels.
    vec4 sum = texture(Sampler0, TexCoord) * 0.227027;
    sum += texture(Sampler0, TexCoord + offset * 1.384615) * 0.316216;
    sum += texture(Sampler0, TexCoord - offset * 1.384615) * 0.316216;
    sum += texture(Sampler0, TexCoord + offset * 3.230769) * 0.070270;
    sum += texture(Sampler0, TexCoord - offset * 3.230769) * 0.070270;

    OutColor = sum;
}
