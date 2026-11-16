#pragma once
#include <string>

namespace overcooked {

class MonoRuntimeHost {
public:
    bool Initialize(const std::string& managed_dir, const std::string& assembly_path);
    void TickUpdate(float delta_time);
    void Shutdown();

private:
    bool initialized_ = false;
    std::string assembly_path_;
};

} // namespace overcooked
