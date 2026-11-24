plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))
	api(project(":jadx-commons:jadx-analysis"))

	testRuntimeOnly(project(":jadx-plugins:jadx-dex-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
}
