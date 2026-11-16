#pragma once
#include <cstdint>
#include <string>

namespace overcooked {

#pragma pack(push, 1)
struct Il2CppGlobalMetadataHeader {
    uint32_t sanity;               // Wajib 0xFAB11BAF
    int32_t version;               // Versi 21..24 untuk Unity 5.x/2017.x
    int32_t stringLiteralOffset;
    int32_t stringLiteralCount;
    int32_t stringLiteralDataOffset;
    int32_t stringLiteralDataCount;
    int32_t stringOffset;
    int32_t stringCount;
    int32_t eventsOffset;
    int32_t eventsCount;
    int32_t propertiesOffset;
    int32_t propertiesCount;
    int32_t methodsOffset;
    int32_t methodsCount;
};
#pragma pack(pop)

class Il2CppMetadataLoader {
public:
    bool LoadFromFile(const std::string& metadata_path);
    int32_t Version() const { return header_.version; }
    int32_t EstimatedMethodCount() const { return header_.methodsCount / 32; }

private:
    Il2CppGlobalMetadataHeader header_{};
};

} // namespace overcooked
