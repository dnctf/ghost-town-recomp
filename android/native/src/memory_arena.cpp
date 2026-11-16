#include "memory_arena.h"
#include <cstdlib>
#include <cstring>
#include <stdexcept>

namespace overcooked {

LinearMemoryArena::LinearMemoryArena(size_t capacity_bytes) : capacity_(capacity_bytes) {
    // 64-byte cache-line alignment untuk akses SIMD NEON ARM64
    if (posix_memalign(reinterpret_cast<void**>(&base_ptr_), 64, capacity_bytes) != 0 || !base_ptr_) {
        throw std::runtime_error("Gagal mengalokasikan LinearMemoryArena");
    }
    std::memset(base_ptr_, 0, capacity_bytes);
}

LinearMemoryArena::~LinearMemoryArena() {
    if (base_ptr_) {
        std::free(base_ptr_);
        base_ptr_ = nullptr;
    }
}

void* LinearMemoryArena::Allocate(size_t size, size_t alignment) {
    std::lock_guard<std::mutex> lock(mutex_);
    size_t aligned_offset = (offset_ + (alignment - 1)) & ~(alignment - 1);
    if (aligned_offset + size > capacity_) {
        return nullptr;
    }
    void* ptr = base_ptr_ + aligned_offset;
    offset_ = aligned_offset + size;
    return ptr;
}

void LinearMemoryArena::ResetFrameScratch() {
    std::lock_guard<std::mutex> lock(mutex_);
    offset_ = 0;
}

} // namespace overcooked
