#include <jni.h>
#include <cstdint>
#include <cstdlib>
#include <algorithm>
#include <string>

namespace {
struct Stats {
    uint64_t pixels = 0, opaque = 0, transparent = 0;
    uint64_t luminanceSum = 0, redSum = 0, greenSum = 0, blueSum = 0;
    float edgeEnergy = 0.0f;
};

Stats analyze(const uint8_t* data, size_t bytes, int width, int height) {
    Stats s;
    if (!data || width <= 0 || height <= 0) return s;
    const size_t pixels = static_cast<size_t>(width) * static_cast<size_t>(height);
    if (bytes < pixels * 4u) return s;
    s.pixels = pixels;
    for (size_t i = 0; i < pixels; ++i) {
        const size_t o = i * 4u;
        const uint8_t r = data[o], g = data[o + 1], b = data[o + 2], a = data[o + 3];
        s.redSum += r; s.greenSum += g; s.blueSum += b;
        s.luminanceSum += (54u * r + 183u * g + 19u * b) >> 8u;
        if (a == 0) ++s.transparent; else ++s.opaque;
        if (i % static_cast<size_t>(width) != 0) {
            const size_t left = o - 4u;
            s.edgeEnergy += static_cast<float>(std::abs((int)r - (int)data[left])
                + std::abs((int)g - (int)data[left + 1])
                + std::abs((int)b - (int)data[left + 2])) / 765.0f;
        }
        if (i >= static_cast<size_t>(width)) {
            const size_t up = o - static_cast<size_t>(width) * 4u;
            s.edgeEnergy += static_cast<float>(std::abs((int)r - (int)data[up])
                + std::abs((int)g - (int)data[up + 1])
                + std::abs((int)b - (int)data[up + 2])) / 765.0f;
        }
    }
    s.edgeEnergy /= static_cast<float>(std::max<size_t>(1, pixels * 2u));
    return s;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_waheed_artificerx_core_native_NativeRasterCore_nativeAnalyzeRgba(
    JNIEnv* env, jclass, jbyteArray rgba, jint width, jint height) {
    if (!rgba) return env->NewStringUTF("invalid:null-buffer");
    const jsize bytes = env->GetArrayLength(rgba);
    if (bytes <= 0) return env->NewStringUTF("invalid:empty-buffer");
    jboolean copied = JNI_FALSE;
    jbyte* raw = env->GetByteArrayElements(rgba, &copied);
    if (!raw) return env->NewStringUTF("invalid:buffer-access");
    const Stats s = analyze(reinterpret_cast<const uint8_t*>(raw), static_cast<size_t>(bytes), width, height);
    env->ReleaseByteArrayElements(rgba, raw, JNI_ABORT);
    const double n = static_cast<double>(std::max<uint64_t>(1, s.pixels));
    const std::string out = "pixels=" + std::to_string(s.pixels)
        + ";opaque=" + std::to_string(s.opaque)
        + ";transparent=" + std::to_string(s.transparent)
        + ";mean_r=" + std::to_string(s.redSum / n)
        + ";mean_g=" + std::to_string(s.greenSum / n)
        + ";mean_b=" + std::to_string(s.blueSum / n)
        + ";mean_luminance=" + std::to_string(s.luminanceSum / n)
        + ";edge_energy=" + std::to_string(s.edgeEnergy);
    return env->NewStringUTF(out.c_str());
}
