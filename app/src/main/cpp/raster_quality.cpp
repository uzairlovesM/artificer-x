#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>

namespace artificerx {

struct ImageStats {
    double edgeDensity;
    double averageLuminance;
    double redDominance;
    double greenDominance;
    double blueDominance;
};

ImageStats calculateImageStats(const uint8_t* rgba, int width, int height) {
    const size_t totalPixels = width * height;
    const size_t totalBytes = totalPixels * 4;

    // Early exit for invalid input
    if (!rgba || width <= 0 || height <= 0)
        return {0.0, 0.0, 0.0, 0.0, 0.0};

    // Calculate averages and edge density
    double redSum = 0, greenSum = 0, blueSum = 0;
    double edgeSum = 0.0;
    int pixelCount = 0;

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t i = static_cast<size_t>(y) * width * 4 + static_cast<size_t>(x) * 4;
            const uint8_t r = rgba[i], g = rgba[i + 1], b = rgba[i + 2];
            redSum += r; greenSum += g; blueSum += b;
            ++pixelCount;

            // Calculate edge density using RGB differences
            if (x > 0) {
                const size_t leftIndex = i - 4;
                edgeSum += std::abs(int(r) - int(rgba[leftIndex]))
                         + std::abs(int(g) - int(rgba[leftIndex + 1]))
                         + std::abs(int(b) - int(rgba[leftIndex + 2]));
            }

            if (y > 0) {
                const size_t upIndex = i - width * 4;
                edgeSum += std::abs(int(r) - int(rgba[upIndex]))
                         + std::abs(int(g) - int(rgba[upIndex + 1]))
                         + std::abs(int(b) - int(rgba[upIndex + 2]));
            }
        }
    }

    const double luminanceSum = redSum * 0.2126 + greenSum * 0.7152 + blueSum * 0.0722;
    const double edgeDensity = edgeSum / std::max(1.0, static_cast<double>(totalPixels * 2));

    ImageStats stats = {edgeDensity, luminanceSum / pixelCount, redSum, greenSum, blueSum};
    return stats;
}

extern "C" float artificerx_edge_density(const uint8_t* rgba, int width, int height) {
    // Advanced edge density calculation with channel normalization
    if (!rgba || width <= 1 || height <= 1) return 0.0f;

    const size_t totalPixels = width * height;
    const size_t stride = width * 4;

    double totalEdge = 0.0;
    int validPixels = 0;

    // Pre-calculate normalization factors
    const double inv255 = 1.0 / 255.0;
    const double inv765 = 1.0 / 765.0;

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t pixelIndex = size_t(y) * width + x;
            const size_t rgbaIndex = pixelIndex * 4;

            // Calculate RGB differences with neighbor pixels
            if (x > 0) {
                const size_t leftIndex = rgbaIndex - 4;
                const double rDiff = std::abs(int(rgba[leftIndex])) - std::abs(int(rgba[rgbaIndex]));
                const double gDiff = std::abs(int(rgba[leftIndex + 1])) - std::abs(int(rgba[rgbaIndex + 1]));
                const double bDiff = std::abs(int(rgba[leftIndex + 2])) - std::abs(int(rgba[rgbaIndex + 2]));

                totalEdge += (std::abs(rDiff) + std::abs(gDiff) + std::abs(bDiff)) * inv765;

                if (rgba[leftIndex] && rgba[leftIndex + 1] && rgba[leftIndex + 2] && rgba[leftIndex + 3])
                    ++validPixels;
            }

            if (y > 0) {
                const size_t upIndex = rgbaIndex - 4 * width;
                const double rDiff = std::abs(int(rgba[upIndex])) - std::abs(int(rgba[rgbaIndex]));
                const double gDiff = std::abs(int(rgba[upIndex + 1])) - std::abs(int(rgba[rgbaIndex + 1]));
                const double bDiff = std::abs(int(rgba[upIndex + 2])) - std::abs(int(rgba[rgbaIndex + 2]));

                totalEdge += (std::abs(rDiff) + std::abs(gDiff) + std::abs(bDiff)) * inv765;
            }
        }
    }

    // Return normalized edge density
    return static_cast<float>(totalEdge / std::max(1, validPixels));
}

} // namespace artificerx
