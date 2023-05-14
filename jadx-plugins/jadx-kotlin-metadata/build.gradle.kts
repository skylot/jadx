plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")

	testImplementation(project(":jadx-core").dependencyProject.sourceSets.test.get().output)
	testImplementation("org.apache.commons:commons-lang3:3.12.0")

	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
}
