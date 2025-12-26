#version 150

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform vec2 uSize;
uniform vec2 uLocation;
uniform float radius;
uniform float BlurStrength;
uniform float RefractionStrength;
uniform float EdgeWidth;
uniform float Brightness;
uniform float Saturation;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec4 blur(vec2 uv, float strength) {
    vec4 col = vec4(0.0);
    float total = 0.0;
    vec2 texelSize = 1.0 / InputResolution;
    for (float i = -4.0; i <= 4.0; i += 1.0) {
        for (float j = -4.0; j <= 4.0; j += 1.0) {
            float dist = length(vec2(i, j));
            float weight = exp(-dist * dist / 8.0);
            vec2 offset = vec2(i, j) * texelSize * strength;
            col += texture(InputSampler, clamp(uv + offset, 0.0, 1.0)) * weight;
            total += weight;
        }
    }
    return col / total;
}

vec3 adjustSaturation(vec3 color, float sat) {
    float grey = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(grey), color, sat);
}

void main() {
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 uv = fragCoord / InputResolution;

    vec2 halfSize = uSize / 2.0;
    vec2 center = uLocation + halfSize;
    vec2 pos = fragCoord - center;

    float sdf = sdRoundedBox(pos, halfSize, radius);

    if (sdf > 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    float distFromCenter = length(pos);
    vec2 dirFromCenter = distFromCenter > 0.001 ? pos / distFromCenter : vec2(0.0);
    
    float maxDist = length(halfSize);
    float normalizedDist = clamp(distFromCenter / maxDist, 0.0, 1.0);
    
    float lensStrength = RefractionStrength * 1.5;
    float distortion = pow(normalizedDist, 1.2) * lensStrength;
    
    vec2 refractOffset = -dirFromCenter * distortion / InputResolution;
    vec2 refractedUV = uv + refractOffset;
    
    float edgeFactor = smoothstep(0.0, 1.0, normalizedDist);
    float blurAmount = BlurStrength * (0.5 + edgeFactor * 0.5);
    vec4 baseColor = blur(refractedUV, blurAmount);
    
    float chromaStrength = distortion * 0.08;
    vec2 chromaOffset = -dirFromCenter * chromaStrength / InputResolution;
    
    float r = texture(InputSampler, clamp(refractedUV - chromaOffset, 0.0, 1.0)).r;
    float b = texture(InputSampler, clamp(refractedUV + chromaOffset, 0.0, 1.0)).b;
    baseColor.rgb = mix(baseColor.rgb, vec3(r, baseColor.g, b), edgeFactor * 0.4);

    vec2 lightDir = normalize(vec2(0.6, 0.6));
    float specular = pow(max(0.0, dot(-dirFromCenter, lightDir)), 4.0) * edgeFactor * 0.08;
    float rim = pow(edgeFactor, 2.5) * 0.06;

    baseColor.rgb += vec3(1.0) * (specular + rim) * Brightness;
    
    baseColor.rgb = adjustSaturation(baseColor.rgb, Saturation);
    baseColor.rgb *= Brightness;
    baseColor.rgb = clamp(baseColor.rgb, 0.0, 1.0);

    float alpha = smoothstep(0.0, 4.0, -sdf);

    fragColor = vec4(baseColor.rgb, alpha);
}
