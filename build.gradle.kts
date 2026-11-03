plugins {
    base
    checkstyle
}

allprojects {
    group = "io.github.yluttsev"
    version = "1.0-SNAPSHOT"
}

subprojects {
    pluginManager.withPlugin("java") {
        pluginManager.apply("checkstyle")

        extensions.configure<CheckstyleExtension> {
            toolVersion = "14.4.0"
            configFile = rootProject.file("checkstyle.xml")
            isIgnoreFailures = false
            maxErrors = 0
            maxWarnings = 0
        }

        val moduleCheck = tasks.named("check")
        rootProject.tasks.named("check") {
            dependsOn(moduleCheck)
        }
    }
}
