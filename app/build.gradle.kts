plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("com.diffplug.spotless") version "6.13.0"

    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("info.picocli:picocli:4.7.1")
}

testing {
    suites {
        @Suppress("UNUSED_VARIABLE")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:5.5.4")
                implementation("io.kotest:kotest-assertions-core:5.5.4")
                implementation("io.kotest:kotest-property:5.5.4")
            }
        }
    }
}

application {
    mainClass.set("jp.henry.uke.mask.AppKt")
    applicationName = "uke-anonymizer"
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
