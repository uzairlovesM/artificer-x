import re
import sys
import urllib.request

GRADLE_VERSION = "9.1.0"   # AGP 9.1.0 ke saath compatible Gradle; ./gradlew --version se verify karein

def edit(path, pairs):
    with open(path, encoding="utf-8") as f:
        text = f.read()
    for old, new in pairs:
        count = text.count(old)
        if count != 1:
            sys.exit(f"[ERROR] {path}: '{old.strip()}' {count} dafa mila (1 hona chahiye)")
        text = text.replace(old, new)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"[OK] {path} update ho gaya")

# 1. Version catalog: AGP, compileSdk, Hilt
edit("gradle/libs.versions.toml", [
    ('agp = "8.13.2"', 'agp = "9.1.0"'),
    ('androidCompileSdk = "36"', 'androidCompileSdk = "37"'),
    ('hilt = "2.58"', 'hilt = "2.59"'),
])

# 2. App module: AGP 9 mein Kotlin built-in hai, alag kotlin-android plugin nahi chahiye
edit("app/build.gradle.kts", [
    ("    alias(libs.plugins.kotlin.android)\n", ""),
])

# 3. Gradle wrapper: version aur official SHA-256 (Gradle ki apni .sha256 file se)
sha_url = f"https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip.sha256"
with urllib.request.urlopen(sha_url, timeout=30) as r:
    sha = r.read().decode().split()[0].strip()
if len(sha) != 64:
    sys.exit("[ERROR] SHA-256 sahi format mein nahi aaya")

wrapper = "gradle/wrapper/gradle-wrapper.properties"
with open(wrapper, encoding="utf-8") as f:
    text = f.read()
text, n1 = re.subn(r"distributionUrl=.*", f"distributionUrl=https\\://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip", text)
text, n2 = re.subn(r"distributionSha256Sum=.*", f"distributionSha256Sum={sha}", text)
if n1 != 1 or n2 != 1:
    sys.exit("[ERROR] wrapper properties mein distributionUrl/distributionSha256Sum nahi mila")
with open(wrapper, "w", encoding="utf-8") as f:
    f.write(text)
print(f"[OK] Gradle wrapper {GRADLE_VERSION} par set ho gaya")
