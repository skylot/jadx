plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	api("net.fabricmc:mapping-io:0.5.1") {
		exclude("org.ow2.asm:asm")
		exclude("net.fabricmc:tiny-remapper")
	}
}
