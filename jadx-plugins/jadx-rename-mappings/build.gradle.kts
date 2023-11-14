plugins {
	id("jadx-java")
	id("java-library") // don't publish to maven
}

dependencies {
	api(project(":jadx-core"))

	api("net.fabricmc:mapping-io:0.5.0") {
		exclude("org.ow2.asm:asm")
		exclude("net.fabricmc:tiny-remapper")
	}
}
