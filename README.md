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
/* JADX WARN: Illegal instructions before constructor call */
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

### Solved (PR Submitted)

| Issue | Description | PR | Status |
|-------|-------------|-----|--------|
| [#2760](https://github.com/skylot/jadx/issues/2760) | Inline optimization for constructor call pattern | [#2767](https://github.com/skylot/jadx/pull/2767) | Pending Review |

### Unsolvable (Java Language Constraints)

| Issue | Type | Description | Cases | Root Cause |
|-------|------|-------------|-------|------------|
| [#2761](https://github.com/skylot/jadx/issues/2761) | A: Variable Reuse | Same var in ternary condition & value | 10 | Side-effect duplication |
| [#2762](https://github.com/skylot/jadx/issues/2762) | B-1: Elvis+Throw | `x ?: throw Exception()` pattern | 1 | Conditional constructor |
| [#2763](https://github.com/skylot/jadx/issues/2763) | B-2: Preconditions | Validation before super() | 1 | Conditional constructor |
| [#2764](https://github.com/skylot/jadx/issues/2764) | B-3: Loop | For loop before this() | 1 | Loop not expressible |
| [#2765](https://github.com/skylot/jadx/issues/2765) | B-4: if/else | Complex conditionals | 1 | Multi-branch logic |
| [#2766](https://github.com/skylot/jadx/issues/2766) | C: Combined | Multiple patterns combined | 1 | Multiple constraints |

---

## Pattern Classification

### Type A: Variable Reuse in Ternary (10 cases)

**Problem**: Same variable used in both condition and value of ternary operator.

```java
// Cannot inline - would call getApplicationContext() twice!
Context appContext = context.getApplicationContext();
super(appContext == null ? context : appContext, ...);
//    ^^^^^^^^^^^ condition   ^^^^^^^^^^^ value (same var!)
```

**Why Unsolvable**: Inlining would duplicate side-effect method calls.

### Type B: Control Flow Before Constructor (4 cases)

**B-1: Elvis + Throw**
```java
if (boundary != null) {
    this(source, boundary);  // this() inside if block!
    return;
}
throw new ProtocolException("boundary required");
```

**B-2: Preconditions**
```java
Preconditions.checkNotNull(param, "must not be null");
if (value <= 0) throw new IllegalArgumentException();
super(param, value);  // after validation
```

**B-3: Loop**
```java
for (Entry entry : map.entrySet()) {
    list.add(new Item(entry.getKey(), entry.getValue()));
}
this(name, list);  // this() after loop
```

**B-4: if/else Chain**
```java
String msg;
if (type == 0) msg = "Error A";
else if (type == 1) msg = "Error B";
else msg = "Unknown";
this(msg, cause);  // this() after conditionals
```

**Why Unsolvable**: Java requires `this()`/`super()` as unconditional first statement.

### Type C: Combined Patterns (1 case)

Multiple unsolvable patterns in single constructor:
- String concatenation + API level check + instanceof + conditional assignment

---

## Decompiler Comparison

This is a **known issue across all Java decompilers**:

| Decompiler | Constructor Issue | Kotlin Support | Notes |
|------------|------------------|----------------|-------|
| **JADX** | Warning + PR fix | Java output | 80% improvement with PR #2767 |
| **CFR** | [Issue #265](https://github.com/leibnitz27/cfr/issues/265) | Java output | Generates invalid code |
| **Fernflower** | Similar issues | Java output | IntelliJ's decompiler |
| **[Vineflower](https://github.com/Vineflower/vineflower)** | Some fixes | Kotlin plugin | Most advanced fork |
| **[kotlin-decompiler](https://github.com/Earthcomputer/kotlin-decompiler)** | N/A | **Kotlin output** | Outputs Kotlin directly |

### Why All Java Decompilers Have This Issue

```
┌──────────────────┐
│  Kotlin Source   │  ← Code before this()/super() is VALID
└────────┬─────────┘
         │ kotlinc
         ▼
┌──────────────────┐
│  DEX Bytecode    │  ← No restriction in bytecode
└────────┬─────────┘
         │ Decompiler
         ▼
┌──────────────────┐
│  Java Source     │  ← this()/super() MUST be first statement
└──────────────────┘
         ↑
    CONSTRAINT!
```

**The only complete solution**: Output Kotlin instead of Java.

---

## Implementation Details

### JADX Source Code Analysis

The warning is generated in `PrepareForCodeGen.java`:

```java
// jadx-core/src/main/java/jadx/core/dex/visitors/PrepareForCodeGen.java
private void moveConstructorInConstructor(MethodNode mth) {
    // ...
    Set<RegisterArg> regArgs = new HashSet<>();
    ctrInsn.getRegisterArgs(regArgs);
    regArgs.remove(mth.getThisArg());
    mth.getArgRegs().forEach(regArgs::remove);

    if (!regArgs.isEmpty()) {
        // Local variables used before constructor = WARNING
        mth.addWarnComment("Illegal instructions before constructor call");
        return;
    }
}
```

### Why Standard Inline Fails

`CodeShrinkVisitor.java` controls inlining, but `canReorder()` returns `false` for:

| InsnType | canReorder | Reason |
|----------|------------|--------|
| `INVOKE` | false | Method calls may have side effects |
| `SGET/IGET` | false | Field access may have side effects |
| `CONST` | true | Safe to reorder |

### PR #2767 Solution

Added special handling in constructor context:

```java
private boolean isInlineableInstruction(InsnNode insn) {
    switch (insn.getType()) {
        case SGET:       // Static field read
        case IGET:       // Instance field read
        case CONST:
        case CONST_STR:
        case CHECK_CAST:
        case INVOKE:     // Safe in constructor context if single-use
            return true;
        default:
            return false;
    }
}
```

Key methods added:
- `tryInlineSimpleInstructions()` - Inline expressions into constructor args
- `tryMoveNonEssentialInstructions()` - Move null checks after constructor
- `isKotlinDefaultParamConstructor()` - Hide synthetic Kotlin constructors

---

## Build & Use

```bash
# Clone and build with fix
git clone https://github.com/devload/jadx.git
cd jadx
git checkout fix/constructor-call-inline-optimization
./gradlew dist

# Use
build/jadx/bin/jadx -d output/ your-app.apk
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Root Cause Analysis](docs/FIX_07_ROOT_CAUSE_ANALYSIS.md) | Kotlin → DEX → JADX pipeline analysis |
| [Bytecode Patterns](docs/FIX_07_BYTECODE_PATTERNS.md) | Detailed smali/DEX bytecode analysis |
| [Unresolved Cases](docs/FIX_07_UNRESOLVED.md) | 15 unsolvable cases with explanations |
| [Decompiler Comparison](docs/DECOMPILER_COMPARISON.md) | How other decompilers handle this |

---

## Future Improvements

| Approach | Difficulty | Impact |
|----------|------------|--------|
| **PR #2767** (current) | Done | 80% cases fixed |
| Kotlin output support | High | 100% cases fixed |
| Factory method transformation | Medium | Changes public API |
| Pseudo-Java with annotations | Low | Invalid but documented |

---

## Contributing

Ideas for solving the remaining 15 cases? Comment on the related issues!

- [#2761](https://github.com/skylot/jadx/issues/2761) - Variable reuse pattern
- [#2762](https://github.com/skylot/jadx/issues/2762) - Elvis+throw pattern
- [#2763](https://github.com/skylot/jadx/issues/2763) - Preconditions pattern
- [#2764](https://github.com/skylot/jadx/issues/2764) - Loop pattern
- [#2765](https://github.com/skylot/jadx/issues/2765) - if/else chain pattern
- [#2766](https://github.com/skylot/jadx/issues/2766) - Combined patterns

---

*Licensed under the Apache 2.0 License*
