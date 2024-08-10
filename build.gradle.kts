import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "top.ntutn"
version = "1.0-SNAPSHOT"

tasks.register("buildGuidePdf") {
    val mdFile = project.file("docs/guide/Guide.md")
    val generatedResources = project.file("build/generated/resources")
    val guideDir = File(generatedResources, "guide")
    val outputFile = File(guideDir, "Guide.pdf")

    doLast {
        if (mdFile.exists().not()) {
            logger.log(LogLevel.WARN, "mdFile not exist, please create $mdFile.")
        } else if (outputFile.exists() && outputFile.lastModified() > mdFile.lastModified()) {
            logger.log(LogLevel.INFO, "Guide.pdf is UPDATE TO DATE.")
        } else {
            logger.log(LogLevel.INFO, "Guide.pdf not found, generating it.")
            generatedResources.deleteRecursively()
            generatedResources.mkdirs()
            guideDir.mkdirs()
            val command = arrayOf("pandoc", mdFile.absolutePath, "-o" , outputFile.absolutePath, "--pdf-engine=xelatex", "-V", "CJKmainfont=KaiTi")
            println(command.joinToString(" "))
            val process = ProcessBuilder(*command)
                .directory(mdFile.parentFile)
                .start()
            process.waitFor()
            process.inputStream.readAllBytes().decodeToString().let(::println)
            process.errorStream.readAllBytes().decodeToString().let(::println)
        }
    }
}

tasks.withType<ProcessResources> {
    dependsOn(":buildGuidePdf")
}

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

sourceSets {
    main {
        resources {
            srcDirs( "build/generated/resources")
        }
    }
}

compose.desktop {
    application {
        mainClass = "top.ntutn.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageVersion = "1.4.1"
            description = "A simple Windows desktop workspace manager."

            windows {
                packageName = "top_ntutn_ZDesktopManager"
                dirChooser = true
                menuGroup = "ntutn"
                upgradeUuid = "a2fde22b-ec5f-49e4-b63f-a4235ef4848c" // generate with https://www.guidgen.com
                // licenseFile.set(project.file("LICENSE.txt")) // 不能显示中文
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
