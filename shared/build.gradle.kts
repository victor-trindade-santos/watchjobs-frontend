import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

// URL do backend definida no build, não no código (F7-05): -Pwatchjobs.apiUrl=https://api.exemplo.com
// Vazia = cada plataforma usa a sua padrão de desenvolvimento.
val apiUrl = providers.gradleProperty("watchjobs.apiUrl").orElse("")
val generateBuildEnv = tasks.register("generateBuildEnv") {
    val output = layout.buildDirectory.dir("generated/buildEnv/kotlin")
    val url = apiUrl
    inputs.property("apiUrl", url)
    outputs.dir(output)
    doLast {
        val file = output.get().file("com/watchjobs/BuildEnv.kt").asFile
        file.parentFile.mkdirs()
        val escaped = url.get().replace("\\", "\\\\").replace("\"", "\\\"")
        file.writeText("package com.watchjobs\n\ninternal object BuildEnv {\n    const val API_URL: String = \"$escaped\"\n}\n")
    }
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.watchjobs.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.material.icons.core)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain {
            kotlin.srcDir(generateBuildEnv)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

// Permite apontar o teste integrado para outro backend: gradlew :shared:jvmTest -Pwatchjobs.baseUrl=http://localhost:8082
tasks.withType<Test>().configureEach {
    providers.gradleProperty("watchjobs.baseUrl").orNull?.let { systemProperty("watchjobs.baseUrl", it) }
}
