#include "serialized_file_parser.h"
#include <fstream>

namespace overcooked {

namespace {
uint32_t SwapU32(uint32_t v) {
    return ((v & 0x000000FFu) << 24) |
           ((v & 0x0000FF00u) << 8)  |
           ((v & 0x00FF0000u) >> 8)  |
           ((v & 0xFF000000u) >> 24);
}
} // namespace

bool SerializedFileParser::InspectHeader(const std::string& asset_path, SerializedFileHeader& out_header) {
    std::ifstream file(asset_path, std::ios::binary);
    if (!file.is_open()) return false;

    file.read(reinterpret_cast<char*>(&out_header), sizeof(SerializedFileHeader));
    if (!file) return false;

    // Header Unity SerializedFile disimpan dalam Big-Endian
    out_header.metadata_size = SwapU32(out_header.metadata_size);
    out_header.file_size = SwapU32(out_header.file_size);
    out_header.version = SwapU32(out_header.version);
    out_header.data_offset = SwapU32(out_header.data_offset);
    return out_header.version >= 9 && out_header.version <= 22;
}

} // namespace overcooked
