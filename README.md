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

---

## Fork: Constructor Call Pattern Fix

This fork contains improvements to JADX's handling of **Kotlin constructor decompilation**.

### Problem

When decompiling Kotlin code, JADX produces invalid Java with "Illegal instructions before constructor call" warning:

```java
// Decompiled Kotlin - Invalid Java!
public Regex(String pattern) {
    Intrinsics.checkNotNullParameter(pattern, "pattern");  // BEFORE this()!
    this(Pattern.compile(pattern));  // Java requires this() to be FIRST
}
```

### Root Cause

| Language | Constructor Rules |
|----------|------------------|
| **Kotlin** | Code allowed before `this()`/`super()` |
| **Java** | `this()`/`super()` must be **first statement** |
| **JVM/DEX** | No restriction in bytecode |

### Solution

Implemented inline optimization: **74 → 15 errors (80% improvement)**

```java
// After fix - Valid Java
public Regex(String pattern) {
    this(Pattern.compile(pattern));  // this() is now first!
    Intrinsics.checkNotNullParameter(pattern, "pattern");
}
```

---

## Issue Tracking

### ✅ Solved (PR Submitted)

| Issue | Description | PR |
|-------|-------------|-----|
| [#2760](https://github.com/skylot/jadx/issues/2760) | Inline optimization for constructor call pattern | [#2767](https://github.com/skylot/jadx/pull/2767) |

### ❌ Unsolvable (Java Language Constraints)

| Issue | Type | Description | Cases |
|-------|------|-------------|-------|
| [#2761](https://github.com/skylot/jadx/issues/2761) | A: Variable Reuse | Same var in ternary condition & value | 10 |
| [#2762](https://github.com/skylot/jadx/issues/2762) | B-1: Elvis+Throw | `x ?: throw Exception()` pattern | 1 |
| [#2763](https://github.com/skylot/jadx/issues/2763) | B-2: Preconditions | Validation before super() | 1 |
| [#2764](https://github.com/skylot/jadx/issues/2764) | B-3: Loop | For loop before this() | 1 |
| [#2765](https://github.com/skylot/jadx/issues/2765) | B-4: if/else | Complex conditionals | 1 |
| [#2766](https://github.com/skylot/jadx/issues/2766) | C: Combined | Multiple patterns | 1 |

---

## Pattern Examples

### Type A: Variable Reuse (10 cases) - Partially Solvable
```java
Context appContext = context.getApplicationContext();
super(appContext == null ? context : appContext, ...);
//    ^^^^^^^^^^^ condition   ^^^^^^^^^^^ value (same var!)
```

### Type B: Control Flow (4 cases) - Unsolvable
```java
if (boundary != null) {
    this(source, boundary);  // this() inside if block!
    return;
}
throw new ProtocolException();
```

### Type C: Combined (1 case) - Unsolvable
String concatenation + if condition + API level check before this()

---

## Build & Use

```bash
git clone https://github.com/devload/jadx.git
cd jadx
git checkout fix/constructor-call-inline-optimization
./gradlew dist
```

Output: `build/jadx/bin/jadx`

---

## Documentation

| Document | Description |
|----------|-------------|
| [Root Cause Analysis](docs/FIX_07_ROOT_CAUSE_ANALYSIS.md) | Kotlin → DEX → JADX pipeline |
| [Bytecode Patterns](docs/FIX_07_BYTECODE_PATTERNS.md) | Detailed smali analysis |
| [Unresolved Cases](docs/FIX_07_UNRESOLVED.md) | 15 unsolvable cases |

---

## Contributing

Ideas for solving the remaining 15 cases? Comment on the related issues!

---

*Licensed under the Apache 2.0 License*
