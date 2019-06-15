## JADX

[![Build Status](https://travis-ci.org/skylot/jadx.png?branch=master)](https://travis-ci.org/skylot/jadx)
[![Code Coverage](https://codecov.io/gh/skylot/jadx/branch/master/graph/badge.svg)](https://codecov.io/gh/skylot/jadx)
[![SonarQube Bugs](https://sonarcloud.io/api/project_badges/measure?project=jadx&metric=bugs)](https://sonarcloud.io/dashboard?id=jadx)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

**jadx** - Dex to Java decompiler

Command line and GUI tools for produce Java source code from Android Dex and Apk files

![jadx-gui screenshot](https://i.imgur.com/h917IBZ.png)


### Downloads
- latest [unstable build: ![Download](https://api.bintray.com/packages/skylot/jadx/unstable/images/download.svg) ](https://bintray.com/skylot/jadx/unstable/_latestVersion#files)
- release from [github: ![Latest release](https://img.shields.io/github/release/skylot/jadx.svg)](https://github.com/skylot/jadx/releases/latest)
- release from [bintray: ![Download](https://api.bintray.com/packages/skylot/jadx/releases/images/download.svg) ](https://bintray.com/skylot/jadx/releases/_latestVersion#files)

After download unpack zip file go to `bin` directory and run:
- `jadx` - command line version
- `jadx-gui` - graphical version

On Windows run `.bat` files with double-click\
**Note:** ensure you have installed Java 8 64-bit version


### Related projects:
- [PyJadx](https://github.com/romainthomas/pyjadx) - python binding for jadx by [@romainthomas](https://github.com/romainthomas)


### Building jadx from source
JDK 8 or higher must be installed:

    git clone https://github.com/skylot/jadx.git
    cd jadx
    ./gradlew dist

(on Windows, use `gradlew.bat` instead of `./gradlew`)

Scripts for run jadx will be placed in `build/jadx/bin`
and also packed to `build/jadx-<version>.zip`

### macOS
You can install using brew:

	brew install jadx

### Run
Run **jadx** on itself:

    cd build/jadx/
    bin/jadx -d out lib/jadx-core-*.jar
    # or
    bin/jadx-gui lib/jadx-core-*.jar


### Usage
```
jadx[-gui] [options] <input file> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc)
options:
  -d, --output-dir                    - output directory
  -ds, --output-dir-src               - output directory for sources
  -dr, --output-dir-res               - output directory for resources
  -j, --threads-count                 - processing threads count
  -r, --no-res                        - do not decode resources
  -s, --no-src                        - do not decompile source code
  --single-class                      - decompile a single class
  --output-format                     - can be 'java' or 'json' (default: java)
  -e, --export-gradle                 - save as android gradle project
  --show-bad-code                     - show inconsistent code (incorrectly decompiled)
  --no-imports                        - disable use of imports, always write entire package name
  --no-debug-info                     - disable debug info
  --no-inline-anonymous               - disable anonymous classes inline
  --no-replace-consts                 - don't replace constant value with matching constant field
  --escape-unicode                    - escape non latin characters in strings (with \u)
  --respect-bytecode-access-modifiers - don't change original access modifiers
  --deobf                             - activate deobfuscation
  --deobf-min                         - min length of name, renamed if shorter (default: 3)
  --deobf-max                         - max length of name, renamed if longer (default: 64)
  --deobf-rewrite-cfg                 - force to save deobfuscation map
  --deobf-use-sourcename              - use source file name as class name alias
  --rename-flags                      - what to rename, comma-separated, 'case' for system case sensitivity, 'valid' for java identifiers, 'printable' characters, 'none' or 'all'
  --fs-case-sensitive                 - treat filesystem as case sensitive, false by default
  --cfg                               - save methods control flow graph to dot file
  --raw-cfg                           - save methods control flow graph (use raw instructions)
  -f, --fallback                      - make simple dump (using goto instead of 'if', 'for', etc)
  -v, --verbose                       - verbose output
  --version                           - print jadx version
  -h, --help                          - print this help
Example:
 jadx -d out classes.dex
 jadx --rename-flags "none" classes.dex
 jadx --rename-flags "valid,printable" classes.dex
```
These options also worked on jadx-gui running from command line and override options from preferences dialog

### Troubleshooting
##### Out of memory error:
  - Reduce processing threads count (`-j` option)
  - Increase maximum java heap size:
    * command line (example for linux):
      `JAVA_OPTS="-Xmx4G" jadx -j 1 some.apk`
    * edit 'jadx' script (jadx.bat on Windows) and setup bigger heap size:
      `DEFAULT_JVM_OPTS="-Xmx2500M"`

---------------------------------------
*Licensed under the Apache 2.0 License*

*Copyright 2018 by Skylot*
