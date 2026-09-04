#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>

extern "C" float artificerx_edge_density(const uint8_t* rgba, int width, int height) {
    if (!rgba || width <= 1 || height <= 1) return 0.0f;
    double sum = 0.0;
    const size_t stride = static_cast<size_t>(width) * 4u;
    const size_t pixels = static_cast<size_t>(width) * static_cast<size_t>(height);
    for (int y=0; y<height; ++y) {
        for (int x=0; x<width; ++x) {
            const size_t i = static_cast<size_t>(y)*stride + static_cast<size_t>(x)*4u;
            if (x+1 < width) {
                const size_t j=i+4u;
                sum += (std::abs(int(rgba[i])-int(rgba[j])) + std::abs(int(rgba[i+1])-int(rgba[j+1])) + std::abs(int(rgba[i+2])-int(rgba[j+2]))) / 765.0;
            }
            if (y+1 < height) {
                const size_t j=i+stride;
                sum += (std::abs(int(rgba[i])-int(rgba[j])) + std::abs(int(rgba[i+1])-int(rgba[j+1])) + std::abs(int(rgba[i+2])-int(rgba[j+2]))) / 765.0;
            }
        }
    }
    return static_cast<float>(sum / std::max<size_t>(1, pixels * 2u));
}
