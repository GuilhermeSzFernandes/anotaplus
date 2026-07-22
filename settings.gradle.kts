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
        mavenCentral()
        // JitPack: só pro MPAndroidChart (gráfico de saldo do Financeiro),
        // não está no Maven Central.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AnotaPlus"
include(":app")
