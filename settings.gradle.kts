pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    //本机没有JDK 25时自动下载
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        //DairoUI_Android
        maven("https://jitpack.io")
    }
}

rootProject.name = "DairoMUSIC-Android"
include(":app")
