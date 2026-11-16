#include "shader_bridge.h"

namespace overcooked {

std::string ShaderBridge::MapUnityShaderToSpirvAsset(const std::string& unity_shader_name) {
    if (unity_shader_name == "UI/Default") {
        return "shaders/ui_default.frag.spv";
    }
    if (unity_shader_name == "Unlit/Transparent") {
        return "shaders/unlit_transparent.frag.spv";
    }
    return "shaders/overcooked_standard.frag.spv";
}

bool ShaderBridge::ValidateSpirvBinary(const std::vector<uint32_t>& spirv_words) {
    // Khronos SPIR-V Magic Number: 0x07230203
    return spirv_words.size() >= 5 && spirv_words[0] == 0x07230203u;
}

} // namespace overcooked
