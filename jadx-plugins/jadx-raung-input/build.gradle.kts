plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("io.github.skylot:raung-asm:0.1.1")
}
