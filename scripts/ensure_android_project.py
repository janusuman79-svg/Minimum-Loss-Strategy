#!/usr/bin/env python3
import os
import sys
import subprocess
import base64

def ensure_project(gradle_root):
    gradle_root = os.path.abspath(gradle_root)
    print(f"Ensuring Android project structure at: {gradle_root}")
    os.makedirs(gradle_root, exist_ok=True)
    os.makedirs(os.path.join(gradle_root, "gradle"), exist_ok=True)
    os.makedirs(os.path.join(gradle_root, "app"), exist_ok=True)

    # 1. settings.gradle.kts
    settings_path = os.path.join(gradle_root, "settings.gradle.kts")
    if not os.path.exists(settings_path):
        print("Creating missing settings.gradle.kts...")
        with open(settings_path, "w") as f:
            f.write("""pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
rootProject.name = "NSE F&O Options Scanner"
include(":app")
""")
    else:
        # Sanitize if present
        try:
            with open(settings_path, "r") as f:
                c = f.read()
            import re
            c = re.sub(r'google\s*\{\s*content\s*\{.*?\}\s*\}', 'google()', c, flags=re.DOTALL)
            if 'include(":app")' not in c and "include(':app')" not in c:
                c += '\ninclude(":app")\n'
            with open(settings_path, "w") as f:
                f.write(c)
        except Exception as e:
            print(f"Notice updating settings.gradle.kts: {e}")

    # 2. root build.gradle.kts
    root_build_path = os.path.join(gradle_root, "build.gradle.kts")
    if not os.path.exists(root_build_path):
        print("Creating missing root build.gradle.kts...")
        with open(root_build_path, "w") as f:
            f.write("""plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
""")

    # 3. gradle/libs.versions.toml
    toml_path = os.path.join(gradle_root, "gradle", "libs.versions.toml")
    if not os.path.exists(toml_path):
        print("Creating missing gradle/libs.versions.toml...")
        # Read from current local file if available
        local_toml = os.path.join(os.path.dirname(__file__), "..", "gradle", "libs.versions.toml")
        if os.path.exists(local_toml):
            with open(local_toml, "r") as src, open(toml_path, "w") as dst:
                dst.write(src.read())

    # 4. app/build.gradle.kts
    app_build_path = os.path.join(gradle_root, "app", "build.gradle.kts")
    if not os.path.exists(app_build_path):
        print("Creating missing app/build.gradle.kts...")
        local_app_build = os.path.join(os.path.dirname(__file__), "..", "app", "build.gradle.kts")
        if os.path.exists(local_app_build):
            with open(local_app_build, "r") as src, open(app_build_path, "w") as dst:
                dst.write(src.read())

    # 5. app/proguard-rules.pro
    pro_path = os.path.join(gradle_root, "app", "proguard-rules.pro")
    if not os.path.exists(pro_path):
        with open(pro_path, "w") as f:
            f.write("-dontwarn **\n")

    # 6. .env file
    env_path = os.path.join(gradle_root, ".env")
    if not os.path.exists(env_path):
        with open(env_path, "w") as f:
            f.write("GEMINI_API_KEY=\n")

    # 7. local.properties
    android_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if android_home:
        with open(os.path.join(gradle_root, "local.properties"), "w") as f:
            f.write(f"sdk.dir={android_home}\n")

    # 8. debug.keystore
    debug_keystore = os.path.join(gradle_root, "debug.keystore")
    if not os.path.exists(debug_keystore):
        print("Generating debug.keystore...")
        subprocess.run([
            "keytool", "-genkeypair", "-v",
            "-keystore", debug_keystore,
            "-storepass", "android",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US",
            "-storetype", "PKCS12"
        ], check=False)

    print("Android project structure is completely verified and ready for Gradle.")

if __name__ == "__main__":
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    ensure_project(root)
