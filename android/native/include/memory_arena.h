#pragma once
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <vector>

namespace overcooked {

enum class SoCMemoryTier {
    TIER_4GB_SD845,     // Budget: 512 MB - 1 GB
    TIER_8GB_SD8GEN1,   // Budget: 1.5 GB - 2 GB
    TIER_12GB_SD8GEN3   // Budget: 2 GB - 3 GB
};

class LinearMemoryArena {
public:
    explicit LinearMemoryArena(size_t capacity_bytes);
    ~LinearMemoryArena();

    LinearMemoryArena(const LinearMemoryArena&) = delete;
    LinearMemoryArena& operator=(const LinearMemoryArena&) = delete;

    void* Allocate(size_t size, size_t alignment = 16);
    void ResetFrameScratch();
    size_t UsedBytes() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return offset_;
    }
    size_t CapacityBytes() const { return capacity_; }

private:
    uint8_t* base_ptr_ = nullptr;
    size_t capacity_ = 0;
    size_t offset_ = 0;
    mutable std::mutex mutex_;
};

} // namespace overcooked
