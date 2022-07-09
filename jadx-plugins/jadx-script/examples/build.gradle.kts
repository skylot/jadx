plugins {
    kotlin("jvm") version "1.7.20"
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
	implementation("org.jetbrains.kotlin:kotlin-script-runtime")

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
}

sourceSets {
	main {
		java.srcDirs("scripts", "context")
	}
}
