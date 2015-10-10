### Run jadx on test android application

This module contains build scripts for test recompilation of simple android app from:
https://github.com/skylot/jadx-test-app

For run tests type follow commands in jadx root directory:

```java
git submodule init
git submodule update
./gradlew testAppCheck
```

Note: You will need connected device or emulator for success
