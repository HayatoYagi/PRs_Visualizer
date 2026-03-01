import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.process.JavaForkOptions

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val appDisplayName = "GitHub PRs Visualizer"
val isDevRun = providers.gradleProperty("appDisplayNameDev").orNull == "true"
val runtimeAppDisplayName = if (isDevRun) "$appDisplayName (dev)" else appDisplayName

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
        jvmArgs(
            "-Dapp.display.name=$appDisplayName",
            "-Xdock:name=$appDisplayName",
            "-Dapple.awt.application.name=$appDisplayName",
            "-Dcom.apple.mrj.application.apple.menu.about.name=$appDisplayName",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.hayatoyagi.prvisualizer"
            packageVersion = "1.0.0"
            
            macOS {
                bundleID = "io.github.hayatoyagi.prvisualizer"
                dockName = appDisplayName
                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>CFBundleName</key>
                        <string>$appDisplayName</string>
                        <key>CFBundleDisplayName</key>
                        <string>$appDisplayName</string>
                        """.trimIndent()
                }
            }
        }
    }
}

tasks.matching { it.name == "jvmRun" }.configureEach {
    (this as JavaForkOptions).jvmArgs(
        "-Dapp.display.name=$runtimeAppDisplayName",
        "-Xdock:name=$runtimeAppDisplayName",
        "-Dapple.awt.application.name=$runtimeAppDisplayName",
        "-Dcom.apple.mrj.application.apple.menu.about.name=$runtimeAppDisplayName",
    )
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
