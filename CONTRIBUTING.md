# Contributing

Please note, we have [code of conduct](CODE_OF_CONDUCT.md), please follow it in all your interactions with the project.

## Open Issue

1. Before proceed, please do:
    - check [Troubleshooting Q&A](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A) section on wiki
    - search existing issues by exception message

2. Describe error:
    - full name of method or class with error
    - full java stacktrace (no need to copy method fallback code (commented pseudocode))
    - **IMPORTANT!:** attach or provide link to apk file (double check apk version)

	  **Note**: GitHub don't allow attaching files with `.apk` extension, but you can change extension by adding `.zip` at the end :)


## Pull Request Process

1. Please don't submit any code style fixes or dependencies updates changes.

1. Use only features and API from Java 11 or below.

1. Make sure your code is correctly formatted, see description here: [Code Formatting](https://github.com/skylot/jadx/wiki/Code-Formatting).

1. Make sure your changes are passing build: `./gradlew clean build dist`
