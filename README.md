<img src="https://raw.githubusercontent.com/skylot/jadx/master/jadx-gui/src/main/resources/logos/jadx-logo.png" width="64" align="left" />

## JADX

![Build status](https://img.shields.io/github/actions/workflow/status/skylot/jadx/build-artifacts.yml)
![GitHub contributors](https://img.shields.io/github/contributors/skylot/jadx)
![GitHub all releases](https://img.shields.io/github/downloads/skylot/jadx/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/skylot/jadx/latest/total)
![Latest release](https://img.shields.io/github/release/skylot/jadx.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.skylot/jadx-core)](https://search.maven.org/search?q=g:io.github.skylot%20AND%20jadx)
![Java 11+](https://img.shields.io/badge/Java-11%2B-blue)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**jadx** - Dex to Java decompiler

Command line and GUI tools for producing Java source code from Android Dex and Apk files

> [!WARNING]
> Please note that in most cases **jadx** can't decompile all 100% of the code, so errors will occur.<br />
> Check [Troubleshooting guide](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A#decompilation-issues) for workarounds.

**Main features:**
- decompile Dalvik bytecode to Java code from APK, dex, aar, aab and zip files
- decode `AndroidManifest.xml` and other resources from `resources.arsc`
- deobfuscator included

**jadx-gui features:**
- view decompiled code with highlighted syntax
- jump to declaration
- find usage
- full text search
- smali debugger, check [wiki page](https://github.com/skylot/jadx/wiki/Smali-debugger) for setup and usage

Jadx-gui key bindings can be found [here](https://github.com/skylot/jadx/wiki/JADX-GUI-Key-bindings)

See these features in action here: [jadx-gui features overview](https://github.com/skylot/jadx/wiki/jadx-gui-features-overview)

<img src="https://user-images.githubusercontent.com/118523/142730720-839f017e-38db-423e-b53f-39f5f0a0316f.png" width="700"/>

### Download
- release
  from [github: ![Latest release](https://img.shields.io/github/release/skylot/jadx.svg)](https://github.com/skylot/jadx/releases/latest)
- latest [unstable build ![GitHub commits since tagged version (branch)](https://img.shields.io/github/commits-since/skylot/jadx/latest/master)](https://nightly.link/skylot/jadx/workflows/build-artifacts/master)

After download unpack zip file go to `bin` directory and run:
- `jadx` - command line version
- `jadx-gui` - UI version

On Windows run `.bat` files with double-click\
**Note:** ensure you have installed Java 11 or later 64-bit version.
For Windows, you can download it from [oracle.com](https://www.oracle.com/java/technologies/downloads/#jdk17-windows) (select x64 Installer).

### Install
- Arch Linux
  [![Arch Linux package](https://img.shields.io/archlinux/v/extra/any/jadx)](https://archlinux.org/packages/extra/any/jadx/)
  [![AUR Version](https://img.shields.io/aur/version/jadx-git)](https://aur.archlinux.org/packages/jadx-git)
  ```bash
  sudo pacman -S jadx
  ```
- macOS
  [![homebrew version](https://img.shields.io/homebrew/v/jadx)](https://formulae.brew.sh/formula/jadx)
  ```bash
  brew install jadx
  ```
- Flathub
  [![Flathub Version](https://img.shields.io/flathub/v/com.github.skylot.jadx)](https://flathub.org/apps/com.github.skylot.jadx)
  ```bash
  flatpak install flathub com.github.skylot.jadx
  ```

### Use jadx as a library
You can use jadx in your java projects, check details on [wiki page](https://github.com/skylot/jadx/wiki/Use-jadx-as-a-library)

### Build from source
JDK 11 or higher must be installed:
```
git clone https://github.com/skylot/jadx.git
cd jadx
./gradlew dist
```

(on Windows, use `gradlew.bat` instead of `./gradlew`)

Scripts for run jadx will be placed in `build/jadx/bin`
and also packed to `build/jadx-<version>.zip`

### Usage
```
jadx[-gui] [command] [options] <input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab, .xapk, .apkm, .jadx.kts)
commands (use '<command> --help' for command options):
  plugins	  - manage jadx plugins

options:
  -d, --output-dir                       - output directory
  -ds, --output-dir-src                  - output directory for sources
  -dr, --output-dir-res                  - output directory for resources
  -r, --no-res                           - do not decode resources
  -s, --no-src                           - do not decompile source code
  -j, --threads-count                    - processing threads count, default: 4
  --single-class                         - decompile a single class, full name, raw or alias
  --single-class-output                  - file or dir for write if decompile a single class
  --output-format                        - can be 'java' or 'json', default: java
  -e, --export-gradle                    - save as gradle project (set '--export-gradle-type' to 'auto')
  --export-gradle-type                   - Gradle project template for export:
                                            'auto' - detect automatically
                                            'android-app' - Android Application (apk)
                                            'android-library' - Android Library (aar)
                                            'simple-java' - simple Java
  -m, --decompilation-mode               - code output mode:
                                            'auto' - trying best options (default)
                                            'restructure' - restore code structure (normal java code)
                                            'simple' - simplified instructions (linear, with goto's)
                                            'fallback' - raw instructions without modifications
  --show-bad-code                        - show inconsistent code (incorrectly decompiled)
  --no-xml-pretty-print                  - do not prettify XML
  --no-imports                           - disable use of imports, always write entire package name
  --no-debug-info                        - disable debug info parsing and processing
  --add-debug-lines                      - add comments with debug line numbers if available
  --no-inline-anonymous                  - disable anonymous classes inline
  --no-inline-methods                    - disable methods inline
  --no-move-inner-classes                - disable move inner classes into parent
  --no-inline-kotlin-lambda              - disable inline for Kotlin lambdas
  --no-finally                           - don't extract finally block
  --no-restore-switch-over-string        - don't restore switch over string
  --no-replace-consts                    - don't replace constant value with matching constant field
  --escape-unicode                       - escape non latin characters in strings (with \u)
  --respect-bytecode-access-modifiers    - don't change original access modifiers
  --mappings-path                        - deobfuscation mappings file or directory. Allowed formats: Tiny and Tiny v2 (both '.tiny'), Enigma (.mapping) or Enigma directory
  --mappings-mode                        - set mode for handling the deobfuscation mapping file:
                                            'read' - just read, user can always save manually (default)
                                            'read-and-autosave-every-change' - read and autosave after every change
                                            'read-and-autosave-before-closing' - read and autosave before exiting the app or closing the project
                                            'ignore' - don't read or save (can be used to skip loading mapping files referenced in the project file)
  --deobf                                - activate deobfuscation
  --deobf-min                            - min length of name, renamed if shorter, default: 3
  --deobf-max                            - max length of name, renamed if longer, default: 64
  --deobf-whitelist                      - space separated list of classes (full name) and packages (ends with '.*') to exclude from deobfuscation, default: android.support.v4.* android.support.v7.* android.support.v4.os.* android.support.annotation.Px androidx.core.os.* androidx.annotation.Px
  --deobf-cfg-file                       - deobfuscation mappings file used for JADX auto-generated names (in the JOBF file format), default: same dir and name as input file with '.jobf' extension
  --deobf-cfg-file-mode                  - set mode for handling the JADX auto-generated names' deobfuscation map file:
                                            'read' - read if found, don't save (default)
                                            'read-or-save' - read if found, save otherwise (don't overwrite)
                                            'overwrite' - don't read, always save
                                            'ignore' - don't read and don't save
  --deobf-res-name-source                - better name source for resources:
                                            'auto' - automatically select best name (default)
                                            'resources' - use resources names
                                            'code' - use R class fields names
  --use-source-name-as-class-name-alias  - use source name as class name alias:
                                            'always' - always use source name if it's available
                                            'if-better' - use source name if it seems better than the current one
                                            'never' - never use source name, even if it's available
  --source-name-repeat-limit             - allow using source name if it appears less than a limit number, default: 10
  --use-kotlin-methods-for-var-names     - use kotlin intrinsic methods to rename variables, values: disable, apply, apply-and-hide, default: apply
  --rename-flags                         - fix options (comma-separated list of):
                                            'case' - fix case sensitivity issues (according to --fs-case-sensitive option),
                                            'valid' - rename java identifiers to make them valid,
                                            'printable' - remove non-printable chars from identifiers,
                                           or single 'none' - to disable all renames
                                           or single 'all' - to enable all (default)
  --integer-format                       - how integers are displayed:
                                            'auto' - automatically select (default)
                                            'decimal' - use decimal
                                            'hexadecimal' - use hexadecimal
  --fs-case-sensitive                    - treat filesystem as case sensitive, false by default
  --cfg                                  - save methods control flow graph to dot file
  --raw-cfg                              - save methods control flow graph (use raw instructions)
  -f, --fallback                         - set '--decompilation-mode' to 'fallback' (deprecated)
  --use-dx                               - use dx/d8 to convert java bytecode
  --comments-level                       - set code comments level, values: error, warn, info, debug, user-only, none, default: info
  --log-level                            - set log level, values: quiet, progress, error, warn, info, debug, default: progress
  -v, --verbose                          - verbose output (set --log-level to DEBUG)
  -q, --quiet                            - turn off output (set --log-level to QUIET)
  --disable-plugins                      - comma separated list of plugin ids to disable, default:
  --version                              - print jadx version
  -h, --help                             - print this help

Plugin options (-P<name>=<value>):
  dex-input: Load .dex and .apk files
    - dex-input.verify-checksum          - verify dex file checksum before load, values: [yes, no], default: yes
  java-convert: Convert .class, .jar and .aar files to dex
    - java-convert.mode                  - convert mode, values: [dx, d8, both], default: both
    - java-convert.d8-desugar            - use desugar in d8, values: [yes, no], default: no
  kotlin-metadata: Use kotlin.Metadata annotation for code generation
    - kotlin-metadata.class-alias        - rename class alias, values: [yes, no], default: yes
    - kotlin-metadata.method-args        - rename function arguments, values: [yes, no], default: yes
    - kotlin-metadata.fields             - rename fields, values: [yes, no], default: yes
    - kotlin-metadata.companion          - rename companion object, values: [yes, no], default: yes
    - kotlin-metadata.data-class         - add data class modifier, values: [yes, no], default: yes
    - kotlin-metadata.to-string          - rename fields using toString, values: [yes, no], default: yes
    - kotlin-metadata.getters            - rename simple getters to field names, values: [yes, no], default: yes
  kotlin-smap: Use kotlin.SourceDebugExtension annotation for rename class alias
    - kotlin-smap.class-alias-source-dbg - rename class alias from SourceDebugExtension, values: [yes, no], default: no
  rename-mappings: various mappings support
    - rename-mappings.format             - mapping format, values: [AUTO, TINY_FILE, TINY_2_FILE, ENIGMA_FILE, ENIGMA_DIR, SRG_FILE, XSRG_FILE, JAM_FILE, CSRG_FILE, TSRG_FILE, TSRG_2_FILE, PROGUARD_FILE, INTELLIJ_MIGRATION_MAP_FILE, RECAF_SIMPLE_FILE, JOBF_FILE], default: AUTO
    - rename-mappings.invert             - invert mapping on load, values: [yes, no], default: no
  smali-input: Load .smali files
    - smali-input.api-level              - Android API level, default: 27

Environment variables:
  JADX_DISABLE_XML_SECURITY - set to 'true' to disable all security checks for XML files
  JADX_DISABLE_ZIP_SECURITY - set to 'true' to disable all security checks for zip files
  JADX_ZIP_MAX_ENTRIES_COUNT - maximum allowed number of entries in zip files (default: 100 000)
  JADX_CONFIG_DIR - custom config directory, using system by default
  JADX_CACHE_DIR - custom cache directory, using system by default
  JADX_TMP_DIR - custom temp directory, using system by default

Examples:
  jadx -d out classes.dex
  jadx --rename-flags "none" classes.dex
  jadx --rename-flags "valid, printable" classes.dex
  jadx --log-level ERROR app.apk
  jadx -Pdex-input.verify-checksum=no app.apk
```
These options also work in jadx-gui running from command line and override options from preferences' dialog

Usage for `plugins` command
```
usage: plugins [options]
options:
  -i, --install <locationId>      - install plugin with locationId
  -j, --install-jar <path-to.jar> - install plugin from jar file
  -l, --list                      - list installed plugins
  -a, --available                 - list available plugins from jadx-plugins-list (aka marketplace)
  -u, --update                    - update installed plugins
  --uninstall <pluginId>          - uninstall plugin with pluginId
  --disable <pluginId>            - disable plugin with pluginId
  --enable <pluginId>             - enable plugin with pluginId
  --list-all                      - list all plugins including bundled and dropins
  --list-versions <locationId>    - fetch latest versions of plugin from locationId (will download all artefacts, limited to 10)
  -h, --help                      - print this help
```


### Troubleshooting
Please check wiki page [Troubleshooting Q&A](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A)

### Contributing
To support this project you can:
  - Post thoughts about new features/optimizations that important to you
  - Submit decompilation issues, please read before proceed: [Open issue](CONTRIBUTING.md#Open-Issue)
  - Open pull request, please follow these rules: [Pull Request Process](CONTRIBUTING.md#Pull-Request-Process)

---------------------------------------
*Licensed under the Apache 2.0 License*
