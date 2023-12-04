plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation(project(":jadx-plugins:jadx-dex-input"))

	implementation("org.smali:smali:2.5.2") {
		exclude(group = "junit", module = "junit") // ignore junit 4 transitive dependency
		exclude(group = "com.beust", module = "jcommander") // exclude old jcommander namespace
	}
	// force the latest version for smali
	constraints {
		implementation("com.google.guava:guava:30.1.1-jre")
	}
}
