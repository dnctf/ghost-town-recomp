#version 450

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexCoord;

layout(set = 0, binding = 0) uniform PerFrameUBO {
    mat4 unity_MatrixVP;
    vec4 unity_LightDir;
    vec4 unity_LightColor;
} uFrame;

layout(push_constant) uniform PerDrawPush {
    mat4 unity_ObjectToWorld;
} uDraw;

layout(location = 0) out mediump vec2 fragUV;
layout(location = 1) out mediump vec3 fragWorldNormal;

void main() {
    vec4 worldPos = uDraw.unity_ObjectToWorld * vec4(inPosition, 1.0);
    gl_Position = uFrame.unity_MatrixVP * worldPos;
    fragUV = inTexCoord;
    fragWorldNormal = normalize(mat3(uDraw.unity_ObjectToWorld) * inNormal);
}
