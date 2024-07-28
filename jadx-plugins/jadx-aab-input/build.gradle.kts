plugins {
	id("jadx-library")
}

dependencies {
	compileOnly(project(":jadx-core"))

	implementation("com.android.tools.build:aapt2-proto:8.5.1-11315950")
	implementation("com.google.protobuf:protobuf-java") {
		version {
			require("3.25.3") // version 4 conflict with `aapt2-proto`
		}
	}

	implementation("com.android.tools.build:bundletool:1.17.1") {
		// All of this is unnecessary for parsing BundleConfig.pb except for protobuf
		exclude(group = "com.android.tools.build")
		exclude(group = "com.google.protobuf")
		exclude(group = "com.google.guava")
		exclude(group = "org.bitbucket.b_c")
		exclude(group = "org.slf4j")
		exclude(group = "com.google.auto.value")
		exclude(group = "com.google.dagger")
		exclude(group = "com.google.errorprone")
	}
}
