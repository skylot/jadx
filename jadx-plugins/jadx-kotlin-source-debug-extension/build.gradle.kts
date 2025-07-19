plugins {
	id("jadx-library")
	id("jadx-kotlin")
}

dependencies {
	api(project(":jadx-core"))

	testImplementation(project.project(":jadx-core").sourceSets.getByName("test").output)
	testImplementation("org.apache.commons:commons-lang3:3.18.0")

	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
}
