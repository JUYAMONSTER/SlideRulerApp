pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // ◀◀◀ 이 주소가 가장 중요합니다!
    }
}
rootProject.name = "SlideRulerApp"
include(":app")