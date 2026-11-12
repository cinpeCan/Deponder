plugins {
    `java-platform`
    alias(libs.plugins.vanniktech.maven.publish)
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":deponder-core"))
        api(project(":deponder"))
        api(project(":deponder-compose"))
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "deponder-bom",
        version = project.version.toString(),
    )
    pom {
        name.set("Deponder BOM")
        description.set("Bill of Materials (BOM) aligning versions across Deponder core, Android View, and Compose Multiplatform artifacts.")
    }
}
