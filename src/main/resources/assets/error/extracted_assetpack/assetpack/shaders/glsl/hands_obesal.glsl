#version 120

uniform sampler2D originalTexture;
uniform sampler2D blurredTexture;
uniform vec4  multiplier;
uniform vec3  accentColor;
uniform vec3  gradientStart;
uniform vec3  gradientEnd;
uniform vec2  resolution;
uniform vec2  viewOffset;
uniform float mixFactor;
uniform float time;
uniform float fillMode;
uniform float fillSpeed;
uniform float fillAmount;
uniform float dynamics;

float hash21(vec2 p) {
    p = fract(p * vec2(443.897, 397.297));
    p += dot(p, p + 23.317);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.54;
    for (int i = 0; i < 3; i++) {
        v += valueNoise(p) * a;
        p = mat2(1.48, -1.16, 1.16, 1.48) * p + vec2(6.7, 3.9);
        a *= 0.48;
    }
    return v;
}

vec3 sat(vec3 c, float k) {
    float g = dot(c, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(g), c, k), 0.0, 1.0);
}

vec3 contrast(vec3 c, float k) {
    return clamp((c - 0.5) * k + 0.5, 0.0, 1.0);
}

vec3 softLimit(vec3 c) {
    c = max(c, 0.0);
    return c / (1.0 + c * 0.38);
}

vec3 blurTap(vec2 uv, vec2 dir, float power) {
    vec2 px = 1.0 / max(resolution, vec2(1.0));
    vec2 o = dir * px * power;
    vec3 c = texture2D(blurredTexture, uv).rgb * 0.30;
    c += texture2D(blurredTexture, clamp(uv + o, 0.001, 0.999)).rgb * 0.18;
    c += texture2D(blurredTexture, clamp(uv - o, 0.001, 0.999)).rgb * 0.18;
    c += texture2D(blurredTexture, clamp(uv + o.yx * vec2(-1.0, 1.0), 0.001, 0.999)).rgb * 0.17;
    c += texture2D(blurredTexture, clamp(uv - o.yx * vec2(-1.0, 1.0), 0.001, 0.999)).rgb * 0.17;
    return sat(contrast(c, 0.82), 0.88);
}

float alphaAt(vec2 uv) {
    return texture2D(originalTexture, clamp(uv, 0.001, 0.999)).a;
}

float borderMask(vec2 uv, float w) {
    vec2 dx = dFdx(uv);
    vec2 dy = dFdy(uv);
    float a = alphaAt(uv);
    float n = 1.0;
    n = min(n, alphaAt(uv + dx * w));
    n = min(n, alphaAt(uv - dx * w));
    n = min(n, alphaAt(uv + dy * w));
    n = min(n, alphaAt(uv - dy * w));
    n = min(n, alphaAt(uv + (dx + dy) * w * 0.70));
    n = min(n, alphaAt(uv - (dx + dy) * w * 0.70));
    n = min(n, alphaAt(uv + (dx - dy) * w * 0.70));
    n = min(n, alphaAt(uv - (dx - dy) * w * 0.70));
    return smoothstep(0.015, 0.42, (a - n) * 4.0);
}

vec3 aurora(float p, float streak, vec3 tint, vec3 accent) {
    vec3 a = sat(mix(vec3(0.05, 0.90, 1.00), tint, 0.62), 1.35);
    vec3 b = sat(mix(vec3(0.72, 0.26, 1.00), accent, 0.68), 1.35);
    vec3 c = sat(mix(vec3(0.12, 1.00, 0.62), tint + accent, 0.36), 1.25);
    vec3 col = mix(mix(a, b, smoothstep(0.10, 0.78, p)), c, smoothstep(0.48, 1.0, p));
    col += mix(tint, accent, p) * 0.26;
    col += sat(mix(accent, vec3(0.30, 0.85, 1.0), 0.45), 1.2) * streak * 0.78;
    return sat(col, 1.42);
}

vec3 crystal(float p, float streak, vec3 tint, vec3 accent) {
    vec3 a = sat(mix(vec3(0.02, 0.46, 1.00), tint, 0.70), 1.45);
    vec3 b = sat(mix(vec3(0.00, 1.00, 0.96), accent, 0.70), 1.45);
    vec3 c = sat(mix(vec3(0.92, 0.22, 1.00), tint + accent, 0.30), 1.35);
    vec3 col = mix(mix(a, b, smoothstep(0.08, 0.70, p)), c, smoothstep(0.58, 1.0, p));
    col += sat(mix(tint, accent, 0.55), 1.4) * streak * 1.05;
    col += tint * 0.18 + accent * 0.18;
    return contrast(sat(col, 1.62), 1.10);
}

vec3 smoke(float p, float streak, vec3 tint, vec3 accent) {
    vec3 a = mix(vec3(0.04, 0.06, 0.10), tint * 0.42, 0.45);
    vec3 b = sat(mix(vec3(0.18, 0.58, 0.76), accent, 0.58), 1.10);
    vec3 c = sat(mix(vec3(0.82, 0.34, 0.96), tint + accent, 0.24), 1.15);
    vec3 col = mix(mix(a, b, smoothstep(0.10, 0.78, p)), c, smoothstep(0.62, 1.0, p) * 0.72);
    col += tint * 0.16 + accent * 0.20 + mix(tint, accent, 0.75) * streak * 0.48;
    return contrast(sat(col, 1.28), 1.08);
}

vec3 nebula(float p, float streak, vec3 tint, vec3 accent) {
    vec3 deep = mix(vec3(0.025, 0.018, 0.090), tint * 0.34, 0.34);
    vec3 violet = sat(mix(vec3(0.58, 0.18, 1.00), tint, 0.42), 1.55);
    vec3 cyan = sat(mix(vec3(0.10, 0.88, 1.00), accent, 0.55), 1.45);
    vec3 rose = sat(mix(vec3(1.00, 0.20, 0.74), tint + accent, 0.22), 1.38);
    vec3 col = mix(deep, violet, smoothstep(0.06, 0.62, p));
    col = mix(col, cyan, smoothstep(0.34, 0.86, p) * 0.72);
    col = mix(col, rose, smoothstep(0.68, 1.0, p) * 0.52);
    col += mix(cyan, rose, p) * streak * 1.15;
    col += mix(tint, accent, 0.62) * 0.24;
    return contrast(sat(col, 1.72), 1.16);
}

void main() {
    vec2 texUV = gl_TexCoord[0].xy;
    vec4 src = texture2D(originalTexture, texUV);
    if (src.a < 0.01) discard;

    vec2 scUV = vec2(gl_FragCoord.x / resolution.x, 1.0 - gl_FragCoord.y / resolution.y);
    scUV = clamp(scUV + viewOffset, 0.001, 0.999);

    float amount = clamp(fillAmount, 0.0, 1.5);
    float q = clamp(amount / 1.5, 0.0, 1.0);
    float speedUser = clamp(fillSpeed, 0.0, 2.0);
    float dyn = clamp(dynamics, 0.0, 2.0);
    float mixAmt = clamp(mixFactor, 0.0, 1.0);
    vec3 tint = mix(clamp(multiplier.rgb, 0.0, 1.0), clamp(gradientStart, 0.0, 1.0), 0.72);
    vec3 accent = mix(clamp(accentColor, 0.0, 1.0), clamp(gradientEnd, 0.0, 1.0), 0.72);
    float inten = clamp(multiplier.a, 0.1, 1.25);
    tint = sat(tint, 1.12);
    accent = sat(accent, 1.12);

    float aspect = resolution.x / max(resolution.y, 1.0);
    vec2 p = scUV * vec2(aspect, 1.0);
    float modeSpeed = 1.0;
    if (fillMode < 0.5) {
        modeSpeed = 1.38;
    } else if (fillMode < 1.5) {
        modeSpeed = 1.72;
    } else if (fillMode < 2.5) {
        modeSpeed = 1.55;
    } else {
        modeSpeed = 1.88;
    }
    float speed = (0.06 + speedUser * (0.95 + dyn * 0.20) + q * 0.38) * modeSpeed;

    vec2 flowUV = p * (1.15 + q * 1.25);
    flowUV += vec2(time * 0.24 * speed, -time * 0.17 * speed);
    flowUV += vec2(sin(scUV.y * (7.0 + dyn * 1.8) + time * 0.58 * speed), cos(scUV.x * (6.0 + dyn * 1.4) - time * 0.44 * speed)) * (0.18 + dyn * 0.06);

    if (fillMode >= 0.5 && fillMode < 1.5) {
        flowUV = mat2(0.84, -0.54, 0.54, 0.84) * flowUV;
        flowUV += vec2(sin(time * 1.35 * speed), cos(time * 1.10 * speed)) * 0.18;
    } else if (fillMode >= 1.5 && fillMode < 2.5) {
        flowUV += vec2(fbm(p * 1.4 + time * 0.35), fbm(p * 1.2 - time * 0.31)) * 0.35;
    } else if (fillMode >= 2.5) {
        flowUV = mat2(0.62, -0.78, 0.78, 0.62) * flowUV;
        flowUV += vec2(fbm(p * 1.9 + vec2(time * 0.30 * speed, -time * 0.22 * speed)),
                       fbm(p * 1.7 + vec2(-time * 0.24 * speed, time * 0.34 * speed))) * 0.55;
    }

    float n1 = fbm(flowUV);
    float n2 = fbm(p * (2.55 + q * 1.15) + vec2(-time * 0.28 * speed, time * 0.36 * speed) + vec2(8.2, 1.7));
    float flow = smoothstep(0.08, 0.92, n1 * 0.56 + n2 * 0.44);
    float micro = fbm(p * (3.7 + dyn * 1.4) + vec2(time * 0.52 * speed, -time * 0.37 * speed));
    float shell = borderMask(texUV, 0.85 + dyn * 0.55);

    float detailLine = 2.2;
    if (fillMode >= 0.5 && fillMode < 1.5) {
        detailLine = 3.8;
    } else if (fillMode >= 1.5 && fillMode < 2.5) {
        detailLine = 1.45;
    } else if (fillMode >= 2.5) {
        detailLine = 2.75;
    }
    float streakLine = sin((p.x * detailLine - p.y * (detailLine + 1.15)) + flow * (5.8 + q * 1.8) + time * speed * (1.08 + dyn * 0.24));
    float streak = pow(max(0.0, streakLine * 0.5 + 0.5), 7.0);
    if (fillMode >= 0.5 && fillMode < 1.5) {
        float shard = abs(sin(p.x * (9.0 + dyn * 1.5) + p.y * (5.5 + dyn * 0.8) + time * speed * 1.55 + flow * (3.0 + dyn)));
        streak = max(streak, pow(shard, 10.0) * 0.75);
    } else if (fillMode >= 1.5 && fillMode < 2.5) {
        streak *= 0.55 + fbm(p * (3.0 + dyn * 0.8) + vec2(time * 0.72 * speed, -time * 0.44 * speed)) * 0.65;
    } else if (fillMode >= 2.5) {
        float star = fbm(p * (7.5 + dyn * 2.0) + vec2(time * 0.16 * speed, -time * 0.12 * speed));
        float vein = abs(sin((p.x * 5.2 + p.y * 7.4) + flow * 7.2 - time * speed * 1.25));
        streak = max(streak * 0.85, pow(max(0.0, star), 18.0) * 1.7);
        streak = max(streak, pow(vein, 13.0) * 0.82);
    }

    vec2 refDir = normalize(vec2(
        sin(flow * 6.283 + time * 0.68 * speed + scUV.y * 3.0),
        cos(flow * 5.200 - time * 0.58 * speed + scUV.x * 2.5)
    ));
    vec2 refrUV = clamp(scUV + refDir * (0.004 + q * 0.014) * (0.65 + flow), 0.001, 0.999);
    vec3 bg = blurTap(refrUV, refDir, 4.0 + q * 10.0);

    vec3 fill;
    if (fillMode < 0.5) {
        fill = aurora(flow, streak, tint, accent);
    } else if (fillMode < 1.5) {
        fill = crystal(flow, streak, tint, accent);
    } else if (fillMode < 2.5) {
        fill = smoke(flow, streak, tint, accent);
    } else {
        fill = nebula(flow, streak, tint, accent);
    }
    fill = mix(fill, mix(tint, accent, flow), 0.12 + micro * 0.18);
    fill += mix(tint, accent, shell) * (0.08 + streak * 0.22 + dyn * 0.06);

    float whiteCut = max(max(bg.r, bg.g), bg.b);
    bg = mix(bg, bg * vec3(0.62, 0.78, 0.92), smoothstep(0.62, 1.0, whiteCut));

    float vertical = smoothstep(0.0, 1.0, scUV.y);
    float glassDensity = 0.12 + amount * 0.72 + q * 0.12;
    if (fillMode >= 0.5 && fillMode < 1.5) {
        glassDensity += 0.08;
    } else if (fillMode >= 1.5 && fillMode < 2.5) {
        glassDensity += 0.04;
    } else if (fillMode >= 2.5) {
        glassDensity += 0.14;
    }
    vec3 body = bg * (0.22 + mixAmt * 0.18);
    body += fill * glassDensity;
    body += mix(tint, accent, clamp(flow + micro * 0.25, 0.0, 1.0)) * (0.10 + dyn * 0.05);
    body += vec3(0.10, 0.32, 0.46) * (1.0 - vertical) * 0.22;
    body += sat(mix(tint, accent, 0.58), 1.35) * streak * (0.05 + amount * 0.24 + q * 0.08 + dyn * 0.04);
    body += mix(tint, accent, shell) * shell * (0.08 + dyn * 0.06);
    body = contrast(body, 1.13 + q * 0.18);

    vec3 result = body;
    result = sat(result, 1.10 + q * 0.18);
    result = softLimit(result * inten);

    float solid = smoothstep(0.01, 0.40, src.a);
    float bodyA = solid * (0.30 + mixAmt * 0.15 + amount * 0.18 + shell * 0.12);
    gl_FragColor = vec4(result, clamp(bodyA, 0.0, 0.88));
}
