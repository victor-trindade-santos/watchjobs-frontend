plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.watchjobs.MainKt"
        jvmArgs("-Xmx1024m", "-Xss512k")

        // Sem backend, com dados em memória: .\gradlew.bat :desktopApp:run -Pdemo
        if (project.hasProperty("demo")) {
            jvmArgs("-Dwatchjobs.demo=true")
        }
    }
}
