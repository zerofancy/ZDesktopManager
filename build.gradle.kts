import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "top.ntutn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:5.14.0")
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("com.github.qurben:jico:v2.2.0")
    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.16.1")
}

compose.desktop {
    application {
        mainClass = "top.ntutn.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageVersion = "1.2.0"
            description = "A simple Windows desktop workspace manager."

            windows {
                packageName = "top_ntutn_ZDesktopManager"
                dirChooser = true
                menuGroup = "ntutn"
                upgradeUuid = "fee07328-e896-48ab-a4a2-e65be6d82d19" // generate with https://www.guidgen.com
                // licenseFile.set(project.file("LICENSE.txt")) // 不能显示中文
            }
        }
    }
}
