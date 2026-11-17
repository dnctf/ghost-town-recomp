#include <windows.h>
#include <d3d11.h>
#include <dxgi.h>
#include <cstdio>

#pragma comment(lib, "d3d11.lib")
#pragma comment(lib, "dxgi.lib")

static LRESULT CALLBACK WindowProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    if (message == WM_DESTROY) {
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcA(hwnd, message, wParam, lParam);
}

int main() {
    const char* className = "GhostTownD3D11Smoke";
    WNDCLASSA wc{};
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = GetModuleHandleA(nullptr);
    wc.lpszClassName = className;
    if (!RegisterClassA(&wc)) {
        std::printf("D3D11_SMOKE_FAIL RegisterClass error=%lu\n", GetLastError());
        return 10;
    }

    HWND hwnd = CreateWindowExA(0, className, "Ghost Town D3D11 Smoke Test",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 800, 480,
        nullptr, nullptr, wc.hInstance, nullptr);
    if (!hwnd) {
        std::printf("D3D11_SMOKE_FAIL CreateWindow error=%lu\n", GetLastError());
        return 11;
    }
    ShowWindow(hwnd, SW_SHOW);

    DXGI_SWAP_CHAIN_DESC desc{};
    desc.BufferDesc.Width = 800;
    desc.BufferDesc.Height = 480;
    desc.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    desc.BufferCount = 2;
    desc.OutputWindow = hwnd;
    desc.Windowed = TRUE;
    desc.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

    ID3D11Device* device = nullptr;
    ID3D11DeviceContext* context = nullptr;
    IDXGISwapChain* swapChain = nullptr;
    D3D_FEATURE_LEVEL featureLevel{};
    const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_1, D3D_FEATURE_LEVEL_10_0
    };

    HRESULT hr = D3D11CreateDeviceAndSwapChain(
        nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, 0,
        levels, ARRAYSIZE(levels), D3D11_SDK_VERSION,
        &desc, &swapChain, &device, &featureLevel, &context);

    if (FAILED(hr)) {
        std::printf("D3D11_SMOKE_FAIL CreateDeviceAndSwapChain hr=0x%08lx\n", (unsigned long)hr);
        return 12;
    }

    ID3D11Texture2D* backBuffer = nullptr;
    hr = swapChain->GetBuffer(0, __uuidof(ID3D11Texture2D), (void**)&backBuffer);
    if (FAILED(hr)) {
        std::printf("D3D11_SMOKE_FAIL GetBuffer hr=0x%08lx\n", (unsigned long)hr);
        return 13;
    }

    ID3D11RenderTargetView* target = nullptr;
    hr = device->CreateRenderTargetView(backBuffer, nullptr, &target);
    backBuffer->Release();
    if (FAILED(hr)) {
        std::printf("D3D11_SMOKE_FAIL CreateRenderTargetView hr=0x%08lx\n", (unsigned long)hr);
        return 14;
    }

    std::printf("D3D11_SMOKE_DEVICE_OK feature_level=0x%x\n", (unsigned)featureLevel);
    for (int frame = 0; frame < 300; ++frame) {
        MSG msg{};
        while (PeekMessageA(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) frame = 300;
            TranslateMessage(&msg);
            DispatchMessageA(&msg);
        }
        const float color[] = { 0.05f, 0.35f, 0.75f, 1.0f };
        context->OMSetRenderTargets(1, &target, nullptr);
        context->ClearRenderTargetView(target, color);
        hr = swapChain->Present(0, 0);
        if (FAILED(hr)) {
            std::printf("D3D11_SMOKE_FAIL Present frame=%d hr=0x%08lx\n", frame, (unsigned long)hr);
            target->Release(); swapChain->Release(); context->Release(); device->Release();
            return 15;
        }
        Sleep(16);
    }

    std::printf("D3D11_SMOKE_OK frames=300\n");
    target->Release();
    swapChain->Release();
    context->Release();
    device->Release();
    DestroyWindow(hwnd);
    return 0;
}
