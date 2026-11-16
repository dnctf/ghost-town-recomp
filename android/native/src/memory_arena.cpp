#include "memory_arena.h"
#include <cstdlib>
#include <cstring>
#include <algorithm>

namespace overcooked {

LinearMemoryArena::LinearMemoryArena(size_t capacity_bytes) : capacity_(capacity_bytes) {
    // 64-byte alignment untuk akses SIMD NEON ARM64
    if (posix_memalign(reinterpret_cast<void**>(&base_ptr_), 64, capacity_bytes) != 0 || !base_ptr_) {
        base_ptr_ = static_cast<uint8_t*>(std::malloc(capacity_bytes));
    }
    if (!base_ptr_) {
        // Fallback alokasi darurat 8 MB
        capacity_ = 8ULL * 1024ULL * 1024ULL;
        base_ptr_ = static_cast<uint8_t*>(std::malloc(capacity_));
    }
    // Hanya bersihkan blok awal agar tidak memicu Android/HarmonyOS Low Memory Killer (OOM)
    if (base_ptr_) {
        std::memset(base_ptr_, 0, std::min<size_t>(capacity_, 64ULL * 1024ULL));
    }
}

LinearMemoryArena::~LinearMemoryArena() {
    if (base_ptr_) {
        std::free(base_ptr_);
        base_ptr_ = nullptr;
    }
}

void* LinearMemoryArena::Allocate(size_t size, size_t alignment) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!base_ptr_) return nullptr;
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
