plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	api(project(":jadx-core"))

	implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.20")

	testImplementation(project.project(":jadx-core").sourceSets.getByName("test").output)
	testImplementation("org.apache.commons:commons-lang3:3.19.0")

	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-java-input"))
}
