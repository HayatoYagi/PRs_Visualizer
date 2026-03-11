import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

// Read version from version.properties
val versionFile = file("${rootProject.projectDir}/version.properties")
if (!versionFile.exists()) {
    throw GradleException("version.properties file not found at ${versionFile.absolutePath}")
}
val versionProps = Properties()
versionFile.inputStream().use { versionProps.load(it) }
val appVersion =
    versionProps.getProperty("version")
        ?: throw GradleException("'version' property not found in version.properties")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val appDisplayName = "PRs Visualizer for GitHub"

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        allWarningsAsErrors.set(false)
        freeCompilerArgs.add("-Xwarning-level=UNUSED_VARIABLE:error")
    }

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
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.hayatoyagi.prvisualizer.generated.resources"
    publicResClass = true
}

compose.desktop {
    application {
        mainClass = "io.github.hayatoyagi.prvisualizer.MainKt"
        val currentOs = OperatingSystem.current()
        val appJvmArgs =
            buildList {
                add("-Dapp.display.name=$appDisplayName")
                if (currentOs.isMacOsX) {
                    add("-Xdock:name=$appDisplayName")
                    add("-Dapple.awt.application.name=$appDisplayName")
                    add("-Dcom.apple.mrj.application.apple.menu.about.name=$appDisplayName")
                    add("-Xdock:icon=${project.projectDir}/src/jvmMain/composeResources/drawable/icon.png")
                }
            }
        jvmArgs(*appJvmArgs.toTypedArray())

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PRsVisualizerForGitHub"
            packageVersion = appVersion
            modules("java.net.http")

            // Application icon configuration
            macOS {
                bundleID = "io.github.hayatoyagi.prvisualizer"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/icon.png"))
            }
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
