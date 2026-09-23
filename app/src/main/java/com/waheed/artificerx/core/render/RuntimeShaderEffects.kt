package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AGSL effect engine for API 33+. It works on Android Canvas bitmaps and does
 * not mix Skiko shader objects into Compose's Android ShaderBrush abstraction.
 */
@Singleton
class RuntimeShaderEffects @Inject constructor(
    private val telemetry: RenderTelemetry,
) {
    fun apply(
        source: Bitmap,
        effect: ShaderEffect,
        intensity: Float = 1f,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (source.isRecycled) return false

        return runCatching {
            telemetry.section("artificerx-agsl-filter") {
            val shader = RuntimeShader(effect.source)
            shader.setInputShader("inputImage", BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            shader.setFloatUniform("intensity", intensity.coerceIn(0f, 4f))
            shader.setFloatUniform("resolution", source.width.toFloat(), source.height.toFloat())

            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawColor(android.graphics.Color.TRANSPARENT)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), paint)

            val originalCanvas = Canvas(source)
            originalCanvas.drawBitmap(result, 0f, 0f, null)
            result.recycle()
            true
            }
        }.getOrElse { false }
    }

    enum class ShaderEffect(val source: String) {
        THRESHOLD(
            """
            uniform shader inputImage;
            uniform float intensity;
            uniform float2 resolution;
            half4 main(float2 p) {
                half4 c = sample(inputImage, p);
                half l = dot(c.rgb, half3(0.2126, 0.7152, 0.0722));
                half t = step(0.5 - 0.45 * half(intensity), l);
                return half4(t.xxx, c.a);
            }
            """.trimIndent(),
        ),
        VIGNETTE(
            """
            uniform shader inputImage;
            uniform float intensity;
            uniform float2 resolution;
            half4 main(float2 p) {
                half4 c = sample(inputImage, p);
                float2 uv = p / resolution;
                float2 d = uv - 0.5;
                float v = smoothstep(0.15, 0.85, dot(d, d) * (1.3 + 1.8 * intensity));
                return half4(c.rgb * (1.0 - 0.85 * v), c.a);
            }
            """.trimIndent(),
        ),
        CHROMATIC_ABERRATION(
            """
            uniform shader inputImage;
            uniform float intensity;
            uniform float2 resolution;
            half4 main(float2 p) {
                float2 uv = p / resolution - 0.5;
                float2 dir = normalize(uv + float2(0.0001));
                float px = max(0.5, intensity) * 2.0;
                half r = sample(inputImage, p + dir * px).r;
                half g = sample(inputImage, p).g;
                half b = sample(inputImage, p - dir * px).b;
                half a = sample(inputImage, p).a;
                return half4(r, g, b, a);
            }
            """.trimIndent(),
        ),
        SHARPEN(
            """
            uniform shader inputImage;
            uniform float intensity;
            uniform float2 resolution;
            half4 main(float2 p) {
                float2 e = 1.0 / resolution;
                half4 c = sample(inputImage, p);
                half4 n = sample(inputImage, p + float2(0.0, e.y));
                half4 s = sample(inputImage, p - float2(0.0, e.y));
                half4 w = sample(inputImage, p - float2(e.x, 0.0));
                half4 ee = sample(inputImage, p + float2(e.x, 0.0));
                half4 high = c * 5.0 - n - s - w - ee;
                return half4(mix(c.rgb, high.rgb, clamp(intensity, 0.0, 1.0)), c.a);
            }
            """.trimIndent(),
        ),
        POSTERIZE(
            """
            uniform shader inputImage;
            uniform float intensity;
            uniform float2 resolution;
            half4 main(float2 p) {
                half4 c = sample(inputImage, p);
                float steps = max(2.0, floor(2.0 + intensity * 14.0));
                half3 q = floor(c.rgb * steps + 0.5) / steps;
                return half4(q, c.a);
            }
            """.trimIndent(),
        ),
    }
}
