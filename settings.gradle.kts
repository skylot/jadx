rootProject.name = "jadx"

include("jadx-core")
include("jadx-cli")
include("jadx-gui")

include("jadx-plugins")
include("jadx-plugins:jadx-plugins-api")
include("jadx-plugins:jadx-dex-input")
include("jadx-plugins:jadx-java-input")
include("jadx-plugins:jadx-raung-input")
include("jadx-plugins:jadx-smali-input")
include("jadx-plugins:jadx-java-convert")

include("jadx-plugins:jadx-script:jadx-script-plugin")
include("jadx-plugins:jadx-script:jadx-script-runtime")
include("jadx-plugins:jadx-script:jadx-script-ide")
include("jadx-plugins:jadx-script:examples")
