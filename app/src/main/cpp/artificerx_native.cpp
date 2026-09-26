#include "artificerx_native.hpp"
#include <jni.h>
#include <cstdint>
#include <cstdio>
#include <limits>

namespace {

bool validRgbaBuffer(JNIEnv* env, jbyteArray rgba, jint width, jint height) {
    if (rgba == nullptr || width <= 0 || height <= 0) return false;

    const jlong pixel_count = static_cast<jlong>(width) * static_cast<jlong>(height);
    const jlong max_jlong = std::numeric_limits<jlong>::max();
    if (pixel_count <= 0 || pixel_count > (max_jlong / 4LL)) return false;

    const jlong expected_bytes = pixel_count * 4LL;
    return expected_bytes <= static_cast<jlong>(env->GetArrayLength(rgba));
}

}  // namespace

extern "C" JNIEXPORT jfloat JNICALL
Java_com_waheed_artificerx_ArtificerXApp_nativeEdgeDensity(
        JNIEnv* env, jobject /* this */, jbyteArray rgba, jint width, jint height) {
    if (!validRgbaBuffer(env, rgba, width, height)) return 0.0f;

    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    if (!bytes) return 0.0f;

    float result = artificerx_edge_density(
            reinterpret_cast<const uint8_t*>(bytes), width, height);

    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_waheed_artificerx_core_nativeops_NativeRasterCore_nativeAnalyzeRgba(
        JNIEnv* env, jclass /* clazz */, jbyteArray rgba, jint width, jint height) {
    if (!validRgbaBuffer(env, rgba, width, height)) {
        return env->NewStringUTF("{\"error\":\"invalid_buffer\"}");
    }

    const jlong pixel_count = static_cast<jlong>(width) * static_cast<jlong>(height);

    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    if (!bytes) {
        return env->NewStringUTF("{\"error\":\"buffer_access_failed\"}");
    }

    uint64_t sum_r = 0;
    uint64_t sum_g = 0;
    uint64_t sum_b = 0;
    uint64_t sum_a = 0;
    uint64_t sum_luma = 0;
    uint64_t opaque = 0;

    const auto* pixels = reinterpret_cast<const uint8_t*>(bytes);
    for (jlong i = 0; i < pixel_count; ++i) {
        const uint8_t r = pixels[i * 4 + 0];
        const uint8_t g = pixels[i * 4 + 1];
        const uint8_t b = pixels[i * 4 + 2];
        const uint8_t a = pixels[i * 4 + 3];
        sum_r += r;
        sum_g += g;
        sum_b += b;
        sum_a += a;
        sum_luma += static_cast<uint64_t>(
                2126u * static_cast<uint32_t>(r) +
                7152u * static_cast<uint32_t>(g) +
                722u * static_cast<uint32_t>(b));
        if (a == 255u) ++opaque;
    }

    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);

    const double inv_pixels = 1.0 / static_cast<double>(pixel_count);
    const double avg_r = static_cast<double>(sum_r) * inv_pixels;
    const double avg_g = static_cast<double>(sum_g) * inv_pixels;
    const double avg_b = static_cast<double>(sum_b) * inv_pixels;
    const double avg_a = static_cast<double>(sum_a) * inv_pixels;
    const double avg_luma = static_cast<double>(sum_luma) * inv_pixels / 10000.0;
    const double opaque_coverage = static_cast<double>(opaque) * inv_pixels;

    char result[320];
    std::snprintf(
            result, sizeof(result),
            "{\"avg_rgba\":[%.3f,%.3f,%.3f,%.3f],\"avg_luminance\":%.5f,\"opaque_coverage\":%.5f,\"pixels\":%lld}",
            avg_r, avg_g, avg_b, avg_a, avg_luma, opaque_coverage,
            static_cast<long long>(pixel_count));
    return env->NewStringUTF(result);
}
