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

//开发时直接引用本地的DairoUI_Android源码,和iOS版引用本地的DairoUI_IOS一样;本地没有时使用jitpack上发布的版本
if (file("../DairoUI_Android").exists()) {
    includeBuild("../DairoUI_Android") {
        dependencySubstitution {
            substitute(module("com.github.DAIRO-HY:DairoUI_Android")).using(project(":dairoui"))
        }
    }
}
