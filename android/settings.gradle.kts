pluginManagement {
    repositories {
        maven { url = uri("/Users/fcj/workspace/Github_space/PhoneCL_APP/android/.gradle/caches/modules-2/files-2.1") }
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("/Users/fcj/workspace/Github_space/PhoneCL_APP/android/.gradle/caches/modules-2/files-2.1") }
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "ARMeasure"
include(":app")
