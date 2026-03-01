# Constructor Call Pattern Fix for JADX

This fork contains improvements to JADX's handling of Kotlin constructor decompilation.

## Problem

When decompiling Kotlin code, JADX produces invalid Java with "Illegal instructions before constructor call" warning:

```java
// Decompiled Kotlin - Invalid Java!
public Regex(String pattern) {
    Intrinsics.checkNotNullParameter(pattern, "pattern");  // BEFORE this()!
    Pattern patternCompile = Pattern.compile(pattern);
    this(patternCompile);  // Java requires this() to be FIRST
}
```

### Root Cause

| Language | Constructor Rules |
|----------|------------------|
| **Kotlin** | Code allowed before `this()`/`super()` |
| **Java** | `this()`/`super()` must be **first statement** |
| **JVM/DEX** | No such restriction in bytecode |

## Solution

Implemented inline optimization that reduces errors from **74 to 15 (80% improvement)**.

### How It Works

1. Identify inlineable instructions (SGET, IGET, CONST, INVOKE, etc.)
2. Inline them directly into constructor arguments
3. Move non-essential code (null checks) after constructor call

```java
// After fix - Valid Java
public Regex(String pattern) {
    this(Pattern.compile(pattern));  // this() is now first!
    Intrinsics.checkNotNullParameter(pattern, "pattern");
}
```

## Issue Tracking

### ✅ Solved (PR Submitted)

| Issue | Description | Status |
|-------|-------------|--------|
| [#2760](https://github.com/skylot/jadx/issues/2760) | Inline optimization for constructor call | [PR #2767](https://github.com/skylot/jadx/pull/2767) |

### ❌ Unsolvable (Java Language Constraints)

| Issue | Type | Description | Cases |
|-------|------|-------------|-------|
| [#2761](https://github.com/skylot/jadx/issues/2761) | A: Variable Reuse | Same var in ternary condition & value | 10 |
| [#2762](https://github.com/skylot/jadx/issues/2762) | B-1: Elvis+Throw | `x ?: throw Exception()` pattern | 1 |
| [#2763](https://github.com/skylot/jadx/issues/2763) | B-2: Preconditions | Validation before super() | 1 |
| [#2764](https://github.com/skylot/jadx/issues/2764) | B-3: Loop | For loop before this() | 1 |
| [#2765](https://github.com/skylot/jadx/issues/2765) | B-4: if/else | Complex conditionals | 1 |
| [#2766](https://github.com/skylot/jadx/issues/2766) | C: Combined | Multiple patterns | 1 |

## Pattern Details

### Type A: Variable Reuse in Ternary (10 cases)

```java
Context appContext = context.getApplicationContext();
super(appContext == null ? context : appContext, ...);
//    ^^^^^^^^^^^ condition   ^^^^^^^^^^^ value (same var!)
```

**Why unsolvable**: Inlining would call `getApplicationContext()` twice.

### Type B: Control Flow (4 cases)

```kotlin
// Kotlin
constructor(response: ResponseBody) : this(
    boundary = response.contentType()?.parameter("boundary")
        ?: throw ProtocolException("boundary required")
)
```

```java
// Decompiled - Invalid!
if (boundary != null) {
    this(source, boundary);  // ❌ this() inside if!
    return;
}
throw ProtocolException();
```

**Why unsolvable**: Java doesn't allow conditional constructor calls.

### Type C: Combined Pattern (1 case)

Multiple issues combined: string concatenation + if condition + API level check.

## Usage

### Build Modified JADX

```bash
git clone https://github.com/devload/jadx.git
cd jadx
git checkout fix/constructor-call-inline-optimization
./gradlew dist
```

### Use Pre-built JAR

```bash
java -jar build/jadx-dev-all.jar -d output input.apk
```

## Documentation

Detailed analysis documents are available:

- [Root Cause Analysis](docs/FIX_07_ROOT_CAUSE_ANALYSIS.md)
- [Bytecode Patterns](docs/FIX_07_BYTECODE_PATTERNS.md)
- [Unresolved Cases](docs/FIX_07_UNRESOLVED.md)

## Contributing

If you have ideas for solving the remaining 15 cases, please comment on the related issues!

## License

Same as JADX - Apache License 2.0
