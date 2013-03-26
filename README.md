## About
**jadx** - Dex to Java decompiler

### Downloads
Latest version available at 
[sourceforge](http://sourceforge.net/projects/jadx/files/) 
or
[bintray](http://bintray.com/pkg/show/general/skylot/jadx/jadx-bundle)

### Build
jadx uses [gradle](http://www.gradle.org/) for build:

    git clone https://github.com/skylot/jadx.git
    cd jadx
    ./gradlew build
    
(on windows, use `gradlew.bat` instead of `./gradlew`)

Scripts for run jadx will be placed in `build/install/jadx/bin`
and also packed to `build/distributions/jadx-<version>.zip`

### Run
Run **jadx** on itself:

    cd build/install/jadx/
    bin/jadx -d out lib/jadx-*.jar

### Usage
```
jadx [options] <input files> (.dex, .apk, .jar or .class)
options:
 -d, --output-dir     - output directory
 -j, --threads-count  - processing threads count
 -f, --fallback       - make simple dump (using goto instead of 'if', 'for', etc)
     --not-obfuscated - set this flag if code not obfuscated
     --cfg            - save methods control flow graph
     --raw-cfg        - save methods control flow graph (use raw instructions)
 -v, --verbose        - verbose output
 -h, --help           - print this help
Example:
 jadx -d out classes.dex
```

*Licensed under the Apache 2.0 License*

*Copyright 2013 by Skylot*
