plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")

	implementation("org.openrewrite:plugin:6.19.1")
	implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:4.2.0")
	implementation("net.ltgt.nullaway:net.ltgt.nullaway.gradle.plugin:2.3.0")
}

repositories {
	gradlePluginPortal()
}
