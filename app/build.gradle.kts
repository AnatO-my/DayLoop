plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.dayloop.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dayloop.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-beta.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
            jniLibs.srcDir(layout.buildDirectory.dir("generated/rust/androidTest/jniLibs"))
        }
    }
}

val rustAndroidTestJniLibs = layout.buildDirectory.dir("generated/rust/androidTest/jniLibs")
val isWindowsHost = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

if (isWindowsHost) {
    val buildRustArm64TestLibrary by tasks.registering(Exec::class) {
        group = "build"
        description = "Builds the DayLoop Rust FFI library for arm64 instrumentation tests."
        workingDir(rootProject.projectDir)
        commandLine(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", rootProject.file("rust/scripts/build-android-ffi.ps1").absolutePath,
            "-OutputDirectory", rustAndroidTestJniLibs.get().asFile.absolutePath,
        )
        inputs.files(fileTree(rootProject.file("rust")) {
            exclude("target/**")
        })
        outputs.dir(rustAndroidTestJniLibs)
    }

    tasks.matching { it.name == "mergeDebugAndroidTestJniLibFolders" }.configureEach {
        dependsOn(buildRustArm64TestLibrary)
    }
} else {
    tasks.register("buildRustArm64TestLibrary") {
        group = "build"
        description = "Windows-only ARM64 Rust JNI build; skipped on ${System.getProperty("os.name")}."
        onlyIf {
            logger.lifecycle(
                "Skipping ARM64 Rust JNI build: the checked-in Android test assembly runs on Windows; " +
                    "Ubuntu CI still verifies Kotlin unit tests and APK assembly.",
            )
            false
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.bundles.androidx.compose)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.mlkitSubjectSegmentation)
    implementation(libs.mlkitImageLabeling)

    kapt(libs.androidx.room.compiler)

    debugImplementation(libs.bundles.androidx.compose.debug)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.bundles.androidx.instrumentation.test)
    androidTestImplementation(libs.androidx.room.testing)
}
