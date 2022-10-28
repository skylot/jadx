plugins {
	id("jadx-library")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")

	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
}
