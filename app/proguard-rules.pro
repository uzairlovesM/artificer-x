-keepattributes *Annotation*
-keep class com.waheed.artificerx.data.remote.dto.** { *; }
-keepclassmembers class com.waheed.artificerx.data.remote.dto.** { *; }
-dontwarn kotlinx.serialization.**

# slf4j is pulled in transitively (okhttp/apollo/other networking deps
# reference it optionally via LoggerFactory.bind()) but this project
# never ships a slf4j binding (logback/slf4j-simple/etc) — Timber +
# android.util.Log cover all real logging needs. Without this, R8
# fails release builds outright with "Missing class
# org.slf4j.impl.StaticLoggerBinder" because it can't prove that
# optional binding class is truly unreachable at runtime.
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
