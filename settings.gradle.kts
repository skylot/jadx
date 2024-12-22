plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

if (!JavaVersion.current().isJava11Compatible) {
	throw GradleException("Jadx requires at least Java 11 for build (current version is '${JavaVersion.current()}')")
}

rootProject.name = "jadx"

include("jadx-core")
include("jadx-cli")
include("jadx-gui")

include("jadx-plugins-tools")

include("jadx-commons:jadx-app-commons")

include("jadx-plugins:jadx-input-api")
include("jadx-plugins:jadx-dex-input")
include("jadx-plugins:jadx-java-input")
include("jadx-plugins:jadx-raung-input")
include("jadx-plugins:jadx-smali-input")
include("jadx-plugins:jadx-java-convert")
include("jadx-plugins:jadx-rename-mappings")
include("jadx-plugins:jadx-kotlin-metadata")
include("jadx-plugins:jadx-xapk-input")
include("jadx-plugins:jadx-aab-input")
include("jadx-plugins:jadx-apkm-input")

include("jadx-plugins:jadx-script:jadx-script-plugin")
include("jadx-plugins:jadx-script:jadx-script-runtime")
include("jadx-plugins:jadx-script:jadx-script-ide")
include("jadx-plugins:jadx-script:examples")
