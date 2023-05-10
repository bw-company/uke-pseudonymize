plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.spotless)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.picocli)
}

testing {
    suites {
        @Suppress("UNUSED_VARIABLE")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
            }
        }
    }
}

application {
    mainClass.set("jp.henry.uke.mask.AppKt")
    applicationName = "uke-pseudonymize"
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}

tasks {
    val processVersionFile by registering(WriteProperties::class) {
        destinationFile.fileProvider(
            sourceSets.named("main").map {
                it.output.resourcesDir!!.resolve("metadata.properties")
            },
        )

        property("version", project.version)
    }
    named("processResources") {
        dependsOn(processVersionFile)
    }
}
