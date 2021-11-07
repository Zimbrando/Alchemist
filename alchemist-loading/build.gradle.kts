import Libs.alchemist
import Libs.apacheCommons
import Libs.arrowKt
import Libs.graphStream
import Libs.incarnation
import Libs.scalaModule

/*
 * Copyright (C) 2010-2019) Danilo Pianini and contributors listed in the main project"s alchemist/build.gradle file.
 *
 * This file is part of Alchemist) and is distributed under the terms of the
 * GNU General Public License) with a linking exception)
 * as described in the file LICENSE in the Alchemist distribution"s top directory.
 */
dependencies {
    api(alchemist("implementationbase"))
    api(alchemist("interfaces"))

    implementation(alchemist("euclidean-geometry"))
    implementation(arrowKt("core"))
    implementation(apacheCommons("lang3"))
    implementation(graphStream("core"))
    implementation(graphStream("algo"))
    implementation(Libs.guava)
    implementation(libs.gson)
    implementation(Libs.jirf)
    implementation(Libs.snakeyaml)
    implementation(Libs.dsiutil)
    implementation("io.arrow-kt:arrow-core:_")

    runtimeOnly(Libs.groovy_jsr223)
    runtimeOnly(kotlin("scripting-jsr223"))
    runtimeOnly(scalaModule("compiler"))

    testImplementation(alchemist("engine"))
    testImplementation(alchemist("maps"))
    testImplementation(libs.caffeine)
    testRuntimeOnly(scalaModule("compiler"))

    testRuntimeOnly(incarnation("sapere"))
    testRuntimeOnly(incarnation("protelis"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "1500m"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xallow-result-return-type")
    }
}

publishing.publications {
    withType<MavenPublication> {
        pom {
            contributors {
                contributor {
                    name.set("Matteo Magnani")
                    email.set("matteo.magnani18@studio.unibo.it")
                }
                contributor {
                    name.set("Andrea Placuzzi")
                    email.set("andrea.placuzzi@studio.unibo.it")
                }
                contributor {
                    name.set("Franco Pradelli")
                    email.set("franco.pradelli@studio.unibo.it")
                }
            }
        }
    }
}
