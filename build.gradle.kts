import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("net.ltgt.errorprone") version "4.1.0"
}

group = "com.livalife"
version = "1.2.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
    }
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    errorprone("com.uber.nullaway:nullaway:0.12.3")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.errorprone {
            error("NullAway")
            option("NullAway:AnnotatedPackages", "com.livalife")
        }
    }
}
