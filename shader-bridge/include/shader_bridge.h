#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace overcooked {

class ShaderBridge {
public:
    static std::string MapUnityShaderToSpirvAsset(const std::string& unity_shader_name);
    static bool ValidateSpirvBinary(const std::vector<uint32_t>& spirv_words);
};

} // namespace overcooked
