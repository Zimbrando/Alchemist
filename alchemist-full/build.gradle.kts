/*
 * Copyright (C) 2010-2019, Danilo Pianini and contributors listed in the main(project"s alchemist/build.gradle file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution"s top directory.
 */

import Libs.alchemist
import Util.isInCI
import Util.isMac
import Util.isMultiplatform
import Util.isWindows
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    application
    alias(libs.plugins.jpackage)
}

dependencies {
    runtimeOnly(rootProject)
    rootProject.subprojects.filterNot { it == project }.forEach {
        if (it.isMultiplatform) {
            runtimeOnly(project(path = ":${it.name}", configuration = "default"))
        } else {
            runtimeOnly(it)
        }
    }
    testImplementation(rootProject)
    testImplementation(alchemist("physics"))
}

application {
    mainClass.set("it.unibo.alchemist.Alchemist")
}

val copyForPackaging by tasks.registering(Copy::class) {
    val jarFile = tasks.shadowJar.get().archiveFileName.get()
    from("${rootProject.projectDir}/build/shadow/$jarFile")
    into("${rootProject.projectDir}/build/package-input")
    dependsOn(tasks.shadowJar)
}

open class CustomJPackageTask() : JPackageTask() {
    @TaskAction
    override fun action() {
        var types: List<ImageType>
        when {
            isWindows -> types = listOf(ImageType.EXE, ImageType.MSI)
            isMac -> types = listOf(ImageType.DMG, ImageType.PKG)
            else -> types = listOf(ImageType.DEB, ImageType.RPM)
        }
        types.forEach {
            setType(it)
            super.action()
        }
    }
}

// jpackageFull should be used instead
tasks.jpackage {
    enabled = false
}

val jpackageFull by tasks.registering(CustomJPackageTask::class) {
    group = "Distribution"
    description = "Creates application bundle in every supported type using jpackage"
    // General info
    resourceDir = "${project.projectDir}/package-settings"
    appName = rootProject.name
    appVersion = rootProject.version.toString().substringBefore('-')
    copyright = "Copyright (C) 2010-2023, Danilo Pianini and contributors"
    appDescription = rootProject.description
    vendor = ""
    licenseFile = "${rootProject.projectDir}/LICENSE.md"
    verbose = isInCI

    // Packaging settings
    input = rootProject.layout.buildDirectory.map { it.dir("package-input") }.get().asFile.path
    destination = rootProject.layout.buildDirectory.map { it.dir("package") }.get().asFile.path
    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = application.mainClass.get()

    linux {
        icon = "${project.projectDir}/package-settings/logo.png"
    }
    windows {
        icon = "${project.projectDir}/package-settings/logo.ico"
        winDirChooser = true
        winShortcutPrompt = true
        winPerUserInstall = isInCI
    }
    mac {
        icon = "${project.projectDir}/package-settings/logo.png"
    }
    dependsOn(copyForPackaging)
}

val deleteJpackageOutput by tasks.registering(Delete::class) {
    when {
        isMac -> setDelete(File("/Applications/${rootProject.name}.app"))
        else -> setDelete(project.file("build/package/install"))
    }
}

tasks.register("testJpackageOutput") {
    group = "Verification"
    description = "Verifies the jpackage output correctness for the OS running the script"
    val workingDirectory = rootProject.file("build/package/")
    val linuxOutputDirs = mapOf("rpm" to "install-rpm", "deb" to "install-deb")
    val windowsOutputDirs = mapOf("msi" to "install-msi", "exe" to "install-exe")
    doFirst {
        val version = rootProject.version.toString().substringBefore('-')
        tasks.withType<Exec>() {
            workingDir = workingDirectory
        }
        // Extract the packets
        when {
            isWindows -> {
                exec {
                    commandLine("msiexec", "-i", "${rootProject.name}-$version.msi", "-quiet", "INSTALLDIR=${workingDirectory.path}\\${windowsOutputDirs["msi"]}")
                }
                exec {
                    commandLine("${rootProject.name}-$version.exe", "-quiet", "INSTALLDIR=${workingDirectory.path}\\${windowsOutputDirs["exe"]}")
                }
            }
            isMac -> {
                exec {
                    commandLine("sudo", "installer", "-pkg", "${rootProject.name}-$version.pkg", "-target", "/")
                }
            }
            else -> {
                workingDirectory.resolve(linuxOutputDirs.getValue("rpm")).mkdirs()
                workingDirectory.resolve(linuxOutputDirs.getValue("deb")).mkdirs()
                exec {
                    commandLine("bsdtar", "-xf", "${rootProject.name}-$version-1.x86_64.rpm", "-C", "${linuxOutputDirs["rpm"]}")
                }
                exec {
                    commandLine("dpkg-deb", "-x", "${rootProject.name}_$version-1_amd64.deb", "${linuxOutputDirs["deb"]}")
                }
            }
        }
    }
    doLast {
        // Check if package contains every file needed
        when {
            isWindows -> {
                windowsOutputDirs.forEach {
                    val execFiles = workingDirectory.resolve(it.value).listFiles().map { it.name }
                    val appFiles = workingDirectory.resolve("${it.value}/app").listFiles().map { it.name }
                    require("${rootProject.name}.exe" in execFiles)
                    require(jpackageFull.get().mainJar in appFiles)
                }
            }
            isMac -> {
                val root = File("/Applications/${rootProject.name}.app")
                val execFiles = root.resolve("Contents/MacOS").listFiles().map { it.name }
                val appFiles = root.resolve("Contents/app").listFiles().map { it.name }
                require(rootProject.name in execFiles)
                require(jpackageFull.get().mainJar in appFiles)
            }
            else -> {
                linuxOutputDirs.forEach {
                    val execFiles = workingDirectory.resolve("${it.value}/opt/alchemist/bin").listFiles().map { it.name }
                    val appFiles = workingDirectory.resolve("${it.value}/opt/alchemist/lib/app").listFiles().map { it.name }
                    require(rootProject.name in execFiles)
                    require(jpackageFull.get().mainJar in appFiles)
                }
            }
        }
    }

    dependsOn(jpackageFull)
    finalizedBy(deleteJpackageOutput)
}

tasks.withType<AbstractArchiveTask> {
    duplicatesStrategy = DuplicatesStrategy.WARN
}

publishing.publications {
    withType<MavenPublication> {
        pom {
            contributors {
                contributor {
                    name.set("Angelo Filaseta")
                    email.set("angelo.filaseta@studio.unibo.it")
                }
            }
        }
    }
}
