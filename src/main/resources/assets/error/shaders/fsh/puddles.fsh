#version 330

in vec2 TexCoord;
out vec4 OutColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform EffectData {
    vec4 uColor;
    vec4 uColor2;
    vec4 lens;
    vec4 upView;
    vec4 march;
    vec4 misc;
    vec4 halfPixelData;
};

float linearDepth(float depth) {
    float near = lens.z;
    float far = lens.w;
    return (near * far) / (near + depth * (far - near));
}

vec3 rayThrough(vec2 texcoord) {
    vec2 ndc = texcoord * 2.0 - 1.0;
    return vec3(ndc.x * lens.x, ndc.y * lens.y, -1.0);
}

vec3 viewPos(vec2 texcoord, float depth) {
    return rayThrough(texcoord) * linearDepth(depth);
}

vec2 toUv(vec3 view) {
    vec2 ndc = vec2(view.x / lens.x, view.y / lens.y) / max(-view.z, 1e-4);
    return ndc * 0.5 + 0.5;
}

const float TILT_SLACK = 0.02;

const float BACK_FADE = 0.25;

bool offScreen(vec2 texcoord) {
    return texcoord.x < 0.0 || texcoord.x > 1.0 || texcoord.y < 0.0 || texcoord.y > 1.0;
}

void main() {
    vec2 uv = TexCoord;
    float depth = texture(Sampler1, uv).x;
    if (depth <= 0.0) {
        discard;
    }

    vec3 origin = viewPos(uv, depth);

    vec3 dx = dFdx(origin);
    vec3 dy = dFdy(origin);
    vec3 normal = cross(dx, dy);
    float nlen = length(normal);
    if (nlen < 1e-6) {
        discard;
    }
    normal /= nlen;
    if (dot(normal, origin) > 0.0) {
        normal = -normal;
    }

    float tiltFade = smoothstep(upView.w - TILT_SLACK, upView.w, dot(normal, normalize(upView.xyz)));
    if (tiltFade <= 0.0) {
        discard;
    }

    vec3 viewDir = normalize(origin);
    vec3 ray = reflect(viewDir, normal);
    float backFade = smoothstep(0.0, BACK_FADE, -ray.z);
    if (backFade <= 0.0) {
        discard;
    }

    float stepLen = march.x;
    int steps = int(march.y);
    float thickness = march.z;
    float maxTravel = misc.y;

    float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);

    vec2 hitUv = vec2(0.0);
    float progress = 0.0;
    bool hit = false;
    vec3 prev = origin;
    float travel = 0.0;

    for (int i = 1; i <= steps; i++) {
        travel = stepLen * (float(i) + jitter) * (1.0 + float(i) * 0.06);
        if (travel > maxTravel) {
            break;
        }
        vec3 point = origin + ray * travel;
        vec2 pointUv = toUv(point);
        if (offScreen(pointUv)) {
            break;
        }

        float sceneDepth = texture(Sampler1, pointUv).x;
        if (sceneDepth <= 0.0) {
            prev = point;
            continue;
        }

        float rayDist = -point.z;
        float sceneDist = linearDepth(sceneDepth);
        if (sceneDist < rayDist && rayDist - sceneDist < thickness) {
            vec3 lo = prev;
            vec3 hi = point;
            for (int r = 0; r < 5; r++) {
                vec3 mid = (lo + hi) * 0.5;
                vec2 midUv = toUv(mid);
                if (offScreen(midUv)) {
                    break;
                }
                float midDepth = texture(Sampler1, midUv).x;
                if (midDepth > 0.0 && linearDepth(midDepth) < -mid.z) {
                    hi = mid;
                } else {
                    lo = mid;
                }
            }
            hitUv = toUv(hi);
            progress = travel / maxTravel;
            hit = true;
            break;
        }
        prev = point;
    }

    if (!hit || offScreen(hitUv)) {
        discard;
    }

    if (distance(hitUv, uv) < 0.002) {
        discard;
    }

    vec3 color = texture(Sampler0, hitUv).rgb;

    vec2 edge = smoothstep(0.0, 0.15, hitUv) * smoothstep(0.0, 0.15, 1.0 - hitUv);
    float fade = edge.x * edge.y;
    fade *= 1.0 - progress * progress;
    fade *= tiltFade * backFade;
    if (misc.x > 0.5) {
        fade *= mix(0.15, 1.0, pow(1.0 - clamp(dot(-viewDir, normal), 0.0, 1.0), 4.0));
    }

    float alpha = clamp(march.w * fade, 0.0, 1.0);
    if (alpha <= 0.004) {
        discard;
    }
    OutColor = vec4(color, alpha);
}
