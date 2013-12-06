## JADX 
**jadx** - Dex to Java decompiler

Command line and GUI tools for produce Java source code from Android Dex and Apk files

Note: jadx-gui now in experimental stage


### Downloads
- [unstable](https://drone.io/github.com/skylot/jadx/files)
[![Build Status](https://drone.io/github.com/skylot/jadx/status.png)](https://drone.io/github.com/skylot/jadx/latest)
[![Build Status](https://travis-ci.org/skylot/jadx.png?branch=master)](https://travis-ci.org/skylot/jadx)
- from [github](https://github.com/skylot/jadx/releases)
- from [sourceforge](http://sourceforge.net/projects/jadx/files/) 


### Building from source  
    git clone https://github.com/skylot/jadx.git
    cd jadx
    ./gradlew dist
    
(on Windows, use `gradlew.bat` instead of `./gradlew`)

Scripts for run jadx will be placed in `build/jadx/bin`
and also packed to `build/jadx-<version>.zip`


### Run
Run **jadx** on itself:

    cd build/jadx/
    bin/jadx -d out lib/jadx-core-*.jar
    #or
    bin/jadx-gui lib/jadx-core-*.jar


### Usage
```
jadx[-gui] [options] <input file> (.dex, .apk or .jar)
options:
 -d, --output-dir    - output directory
 -j, --threads-count - processing threads count
 -f, --fallback      - make simple dump (using goto instead of 'if', 'for', etc)
     --cfg           - save methods control flow graph to dot file
     --raw-cfg       - save methods control flow graph (use raw instructions)
 -v, --verbose       - verbose output
 -h, --help          - print this help
Example:
 jadx -d out classes.dex
```

*Licensed under the Apache 2.0 License*

*Copyright 2013 by Skylot*
