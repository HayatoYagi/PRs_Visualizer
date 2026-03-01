import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

// Read version from version.properties
val versionFile = file("${rootProject.projectDir}/version.properties")
if (!versionFile.exists()) {
    throw GradleException("version.properties file not found at ${versionFile.absolutePath}")
}
val versionProps = Properties()
versionFile.inputStream().use { versionProps.load(it) }
val appVersion = versionProps.getProperty("version")
    ?: throw GradleException("'version' property not found in version.properties")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("org.json:json:20250107")
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.hayatoyagi.prvisualizer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.hayatoyagi.prvisualizer"
            packageVersion = appVersion
        }
    }
}

ktlint {
    version.set("1.3.1")
    android.set(false)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    source.setFrom(
        files("src/jvmMain/kotlin"),
        files("src/commonMain/kotlin"),
    )
}
