plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")

	implementation("org.openrewrite:plugin:6.19.1")
}

repositories {
	gradlePluginPortal()
}
