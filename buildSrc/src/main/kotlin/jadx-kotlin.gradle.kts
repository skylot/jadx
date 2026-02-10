import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("jadx-java")
	id("org.jetbrains.kotlin.jvm")
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect")) // don't work from plugin classloader
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_11)
	}
}
