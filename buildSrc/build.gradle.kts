plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.10")

	implementation("org.openrewrite:plugin:6.19.1")
}

repositories {
	gradlePluginPortal()
}
