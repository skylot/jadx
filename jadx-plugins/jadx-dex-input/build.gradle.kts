plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	// TODO: finish own smali printer
	implementation("com.android.tools.smali:smali-baksmali:3.0.7") {
		exclude(group = "com.beust", module = "jcommander") // exclude old jcommander namespace
	}
	implementation("com.google.guava:guava:33.3.0-jre") // force the latest version for smali

	// compile smali files in tests
	testImplementation("com.android.tools.smali:smali:3.0.7") {
		exclude(group = "com.beust", module = "jcommander") // exclude old jcommander namespace
	}
}
