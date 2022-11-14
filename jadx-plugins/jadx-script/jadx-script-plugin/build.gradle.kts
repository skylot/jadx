plugins {
	id("jadx-library")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}
