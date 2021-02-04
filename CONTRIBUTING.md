# Contributing

Please note we have a [code of conduct](CODE_OF_CONDUCT.md), please follow it in all your interactions with the project.

## Open Issue

1. Before proceed please do:
    - check [Troubleshooting Q&A](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A) section on wiki
    - search existing issues by exception message

2. Describe error
    **Describe error**
    - full name of method or class with error
    - full java stacktrace (no need to copy method fallback code (commented pseudocode))
    - **IMPORTANT!:** attach or provide link to apk file (double check apk version)

      **Note**: GitHub don't allow attach files with `.apk` extension, but you can change extension by adding `.zip` at the end :)


## Pull Request Process

1. Please don't submit any code style fixes, dependencies updates or other changes which are not fixing any issues.

1. Before start working on PR please discuss the change you wish to make via issue. PR without corresponding issue will be rejected.

1. Use only features and API from Java 8 or below.

1. If possible don't add additional dependencies especially if they are big.

1. Make sure your code is correctly formatted, see description here: [Code Formatting](https://github.com/skylot/jadx/wiki/Code-Formatting).

1. Make sure your changes is passing build: `./gradlew clean build dist`
