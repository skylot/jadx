<img src="https://raw.githubusercontent.com/skylot/jadx/master/jadx-gui/src/main/resources/logos/jadx-logo.png" width="64" align="left" />

## JADX

[![Build status](https://github.com/skylot/jadx/workflows/Build/badge.svg)](https://github.com/skylot/jadx/actions?query=workflow%3ABuild)
[![Alerts from lgtm.com](https://img.shields.io/lgtm/alerts/g/skylot/jadx.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/skylot/jadx/alerts/)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**jadx** - Dex to Java decompiler

Command line and GUI tools for producing Java source code from Android Dex and Apk files

**Main features:**
- decompile Dalvik bytecode to java classes from APK, dex, aar, aab and zip files
- decode `AndroidManifest.xml` and other resources from `resources.arsc`
- deobfuscator included

**jadx-gui features:**
- view decompiled code with highlighted syntax
- jump to declaration
- find usage
- full text search

**Upcoming unstable features:**
- smali debugger (thanks to [@LBJ-the-GOAT](https://github.com/LBJ-the-GOAT)), check [wiki page](https://github.com/skylot/jadx/wiki/Smali-debugger) for setup and usage

See these features in action here: [jadx-gui features overview](https://github.com/skylot/jadx/wiki/jadx-gui-features-overview)


![jadx-gui screenshot](https://i.imgur.com/h917IBZ.png)


### Download
- release from [github: ![Latest release](https://img.shields.io/github/release/skylot/jadx.svg)](https://github.com/skylot/jadx/releases/latest)
- latest [unstable build](https://nightly.link/skylot/jadx/workflows/build/master)

After download unpack zip file go to `bin` directory and run:
- `jadx` - command line version
- `jadx-gui` - UI version

On Windows run `.bat` files with double-click\
**Note:** ensure you have installed Java 8 or later 64-bit version.
For windows you can download it from [adoptopenjdk.net](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot#x64_win) (select "Install JRE").

### Install
1. Arch linux
    ```bash
        sudo pacman -S jadx
    ```
2. macOS
    ```bash
        brew install jadx
    ```

### Build from source
JDK 8 or higher must be installed:
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
jadx[-gui] [options] <input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab)
options:
  -d, --output-dir                    - output directory
  -ds, --output-dir-src               - output directory for sources
  -dr, --output-dir-res               - output directory for resources
  -r, --no-res                        - do not decode resources
  -s, --no-src                        - do not decompile source code
  --single-class                      - decompile a single class
  --output-format                     - can be 'java' or 'json', default: java
  -e, --export-gradle                 - save as android gradle project
  -j, --threads-count                 - processing threads count, default: 4
  --show-bad-code                     - show inconsistent code (incorrectly decompiled)
  --no-imports                        - disable use of imports, always write entire package name
  --no-debug-info                     - disable debug info
  --add-debug-lines                   - add comments with debug line numbers if available
  --no-inline-anonymous               - disable anonymous classes inline
  --no-inline-methods                 - disable methods inline
  --no-replace-consts                 - don't replace constant value with matching constant field
  --escape-unicode                    - escape non latin characters in strings (with \u)
  --respect-bytecode-access-modifiers - don't change original access modifiers
  --deobf                             - activate deobfuscation
  --deobf-min                         - min length of name, renamed if shorter, default: 3
  --deobf-max                         - max length of name, renamed if longer, default: 64
  --deobf-cfg-file                    - deobfuscation map file, default: same dir and name as input file with '.jobf' extension
  --deobf-rewrite-cfg                 - force to ignore and overwrite deobfuscation map file
  --deobf-use-sourcename              - use source file name as class name alias
  --deobf-parse-kotlin-metadata       - parse kotlin metadata to class and package names
  --rename-flags                      - fix options (comma-separated list of):
                                         'case' - fix case sensitivity issues (according to --fs-case-sensitive option),
                                         'valid' - rename java identifiers to make them valid,
                                         'printable' - remove non-printable chars from identifiers,
                                        or single 'none' - to disable all renames
                                        or single 'all' - to enable all (default)
  --fs-case-sensitive                 - treat filesystem as case sensitive, false by default
  --cfg                               - save methods control flow graph to dot file
  --raw-cfg                           - save methods control flow graph (use raw instructions)
  -f, --fallback                      - make simple dump (using goto instead of 'if', 'for', etc)
  --log-level                         - set log level, values: QUIET, PROGRESS, ERROR, WARN, INFO, DEBUG, default: PROGRESS
  -v, --verbose                       - verbose output (set --log-level to DEBUG)
  -q, --quiet                         - turn off output (set --log-level to QUIET)
  --version                           - print jadx version
  -h, --help                          - print this help
Examples:
  jadx -d out classes.dex
  jadx --rename-flags "none" classes.dex
  jadx --rename-flags "valid, printable" classes.dex
  jadx --log-level ERROR app.apk
```
These options also worked on jadx-gui running from command line and override options from preferences dialog

### Troubleshooting
Please check wiki page [Troubleshooting Q&A](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A)

### Contributing
To support this project you can:
  - Post thoughts about new features/optimizations that important to you
  - Submit decompilation issues, please read before proceed: [Open issue](CONTRIBUTING.md#Open-Issue)
  - Open pull request, please follow these rules: [Pull Request Process](CONTRIBUTING.md#Pull-Request-Process)

---------------------------------------
*Licensed under the Apache 2.0 License*
