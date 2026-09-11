#include "artificerx_native.hpp"
#include <jni.h>
#include <cstdint>

extern "C" JNIEXPORT jfloat JNICALL
Java_com_waheed_artificerx_ArtificerXApp_nativeEdgeDensity(
        JNIEnv* env, jobject /* this */, jbyteArray rgba, jint width, jint height) {
    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    if (!bytes) return 0.0f;

    float result = artificerx_edge_density(
            reinterpret_cast<const uint8_t*>(bytes), width, height);

    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);
    return result;
}
