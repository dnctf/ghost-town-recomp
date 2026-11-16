#version 450
precision mediump float;

layout(location = 0) in mediump vec2 fragUV;
layout(location = 1) in mediump vec3 fragWorldNormal;

layout(set = 0, binding = 0) uniform PerFrameUBO {
    mat4 unity_MatrixVP;
    vec4 unity_LightDir;
    vec4 unity_LightColor;
} uFrame;

layout(set = 1, binding = 0) uniform sampler2D uMainTex;

layout(location = 0) out lowp vec4 outColor;

void main() {
    lowp vec4 albedo = texture(uMainTex, fragUV);
    mediump float ndotl = max(dot(normalize(fragWorldNormal), normalize(-uFrame.unity_LightDir.xyz)), 0.25);
    lowp vec3 lit = albedo.rgb * uFrame.unity_LightColor.rgb * ndotl;
    outColor = vec4(lit, albedo.a);
}
