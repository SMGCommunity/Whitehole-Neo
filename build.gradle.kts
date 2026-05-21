plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.diffplug.spotless") version "8.5.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "whitehole"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("lib") { include("*.jar") })
}

application {
    mainClass.set("whitehole.Whitehole")
    applicationDefaultJvmArgs = listOf("--add-exports=java.desktop/sun.awt=ALL-UNNAMED","--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "whitehole.Whitehole",
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "lib/${it.name}" }
        )
    }
}

tasks.register<Zip>("packageRelease") {
    group = "build"

    dependsOn(tasks.jar)

    archiveFileName.set("Whitehole-Neo.zip")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    from(tasks.jar) {
        rename { "Whitehole.jar" }
    }
    from(configurations.runtimeClasspath) {
        into("lib")
    }
    from("data") {
        into("data")
    }
    from("Whitehole.bat")
}

spotless {
    java {
        licenseHeaderFile("HEADER.txt", "package ")
    }
    kotlin {
        licenseHeaderFile("HEADER.txt", "package ")
    }
}

// forces license to be updated
tasks.withType<JavaCompile> {
    dependsOn("spotlessApply")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("spotlessApply")
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}