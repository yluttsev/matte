plugins {
    id("java")
}

dependencies {
    implementation(project(":core"))
    implementation("tools.jackson.core:jackson-core:3.2.4")
    implementation("tools.jackson.core:jackson-databind:3.2.4")
    implementation("org.snakeyaml:snakeyaml-engine:3.2")
    implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.48")

    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
    }

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
