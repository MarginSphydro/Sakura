#version 150

uniform sampler2D InputSampler;
uniform sampler2D MaskSampler;
uniform vec2 InputResolution;
uniform float Brightness;
uniform float Quality;
uniform vec4 color1;

out vec4 fragColor;

void main() {
    #define TAU 6.28318530718

    vec2 uv = gl_FragCoord.xy / InputResolution.xy;

    vec4 withEntity = texture(InputSampler, uv);
    vec4 withoutEntity = texture(MaskSampler, uv);

    float diff = length(withEntity.rgb - withoutEntity.rgb);

    if (diff < 0.015) {
        discard;
    }

    vec2 Radius = Quality / InputResolution.xy;
    vec4 Color = withEntity;
    float totalWeight = 1.0;

    float step = TAU / 16.0;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            vec2 sampleUV = uv + vec2(cos(d), sin(d)) * Radius * i;
            vec4 sampleWith = texture(InputSampler, sampleUV);
            vec4 sampleWithout = texture(MaskSampler, sampleUV);
            float sampleDiff = length(sampleWith.rgb - sampleWithout.rgb);
            if (sampleDiff > 0.015) {
                Color += sampleWith;
                totalWeight += 1.0;
            }
        }
    }

    Color /= totalWeight;
    Color.rgb = Color.rgb + color1.rgb;

    float alpha = smoothstep(0.015, 0.08, diff) * Brightness * color1.a;
    fragColor = vec4(Color.rgb, alpha);
}
