import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("jadx-java")
	id("org.jetbrains.kotlin.jvm")
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_11)
	}
}
