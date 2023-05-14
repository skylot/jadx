plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")
	implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.21")
}
