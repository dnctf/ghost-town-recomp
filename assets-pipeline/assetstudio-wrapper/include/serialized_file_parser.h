#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace overcooked {

struct SerializedFileHeader {
    uint32_t metadata_size;
    uint32_t file_size;
    uint32_t version;
    uint32_t data_offset;
    uint8_t endianness;
    uint8_t reserved[3];
};

class SerializedFileParser {
public:
    bool InspectHeader(const std::string& asset_path, SerializedFileHeader& out_header);
};

} // namespace overcooked
