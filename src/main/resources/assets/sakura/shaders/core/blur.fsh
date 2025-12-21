#version 150

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform vec2 uSize;
uniform vec2 uLocation;

uniform float radius;
uniform float Brightness;
uniform float Quality;
uniform vec4 color1;
in vec2 texCoord;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

vec4 blur() {
    #define TAU 6.28318530718

    vec2 Radius = Quality / InputResolution.xy;

    vec2 uv = gl_FragCoord.xy / InputResolution.xy;
    vec4 Color = texture(InputSampler, uv);

    float step =  TAU / 16.0;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            Color += texture(InputSampler, uv + vec2(cos(d), sin(d)) * Radius * i);
        }
    }

    Color /= 80;
    return mix(Color, color1, color1.a) * Brightness;
}

void main() {
    vec2 halfSize = uSize / 2.0;
    // 改进alpha计算以减少边缘闪烁
    float sdf = roundedBoxSDF(gl_FragCoord.xy - uLocation - halfSize, halfSize, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, 1.0, sdf);
    vec4 blurredResult = blur();
    // 确保最小alpha值以避免完全透明导致的闪烁
    float finalAlpha = max(smoothedAlpha * Brightness, 0.01);
    fragColor = vec4(blurredResult.rgb, finalAlpha);
}