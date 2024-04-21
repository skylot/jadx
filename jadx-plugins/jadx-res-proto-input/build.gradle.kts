plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("com.android.tools.build:aapt2-proto:8.3.2-10880808")
	implementation("com.google.protobuf:protobuf-java") {
		version {
			require("3.25.3") // version 4 conflict with `aapt2-proto`
		}
	}
}
