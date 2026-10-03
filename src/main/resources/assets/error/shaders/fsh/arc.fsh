#version 330

layout(std140) uniform ArcData {
    vec4 color1;
    vec4 color2;
    float radius;
    float thickness;
    float start;
    float end;
    vec2 size;
    vec2 location;
};

out vec4 fragColor;

#define PI 3.141592653589793
#define RAD 0.0174533

void main() {
    float startAngle = start * RAD;
    float sweep = min(end * RAD, PI * 2);

    float smoothThresh = 6.0 * (1.0 / length(size));
    vec2 centerPos = ((gl_FragCoord.xy - location) / size.xy) * 2.0 - 1.0;

    float dist = length(centerPos);
    float bandAlpha = smoothstep(radius, radius + smoothThresh, dist) * smoothstep(radius + thickness, (radius + thickness) - smoothThresh, dist);
    float angle = (atan(centerPos.y, centerPos.x) + PI);
    float sweepAngle = mod(angle + PI * 0.5, PI * 2);
    float rel = mod(sweepAngle - startAngle + PI * 2, PI * 2);
    float distStart = rel > PI * 2 - smoothThresh ? rel - PI * 2 : rel;
    float coverage = min(distStart, sweep - rel);
    float angleAlpha = smoothstep(-smoothThresh, smoothThresh, coverage);
    if (sweep >= PI * 2 - smoothThresh) {
        angleAlpha = 1.0;
    }
    if (sweep <= 0.0005) {
        angleAlpha = 0.0;
    }

    float angle2 = (angle / PI * 180.);
    angle2 = angle2 - 360. * floor(angle2 / 360.);
    if (angle2 >= 180.) {
        angle2 = (360. - angle2) * 2.;
    } else {
        angle2 = angle2 * 2.;
    }
    fragColor = mix(color1, color2, angle2 / 360.) * bandAlpha * angleAlpha;
}
