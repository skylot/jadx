# Fix #7: Unresolved Constructor Issues

## Overview

Unresolved cases where "Illegal instructions before constructor call" warning still occurs in JADX.
These cases cannot be perfectly converted due to Java language constraints.

| Item | Value |
|------|-------|
| **Total Unresolved** | 15 (12 files) |
| **Cause** | Java constructor constraints |
| **Status** | Cannot be resolved (language limitation) |

---

## Case Classification

| Type | Count | Description | Resolvability |
|------|-------|-------------|--------------|
| **A: Variable reuse** | 10 | Same variable used twice in ternary operator | Theoretically possible (side-effect analysis needed) |
| **B: Control flow** | 4 | if/throw, if/else, loop patterns | Impossible (Java language constraint) |
| **C: Combined** | 1 | Multiple pattern combination | Impossible |

---

## Type A: Variable Reuse Pattern (10 cases)

Pattern where the same variable is used in both the **condition** and **value** of a ternary operator.

### Problem

```java
// Original code
Context applicationContext = context.getApplicationContext();
super(applicationContext == null ? context : applicationContext, ...);
//    ^^^^^^^^^^^^^^^^^^^ condition    ^^^^^^^^^^^^^^^^^^^ value
```

### Problem with Inlining

```java
// Inlining causes method to be called twice!
super(context.getApplicationContext() == null
    ? context
    : context.getApplicationContext(),  // Second call!
    ...);
```

### Affected Cases

---

### A-1: RecyclerView.java
**File**: `androidx/recyclerview/widget/RecyclerView.java`

```java
public RecyclerView(Context context, AttributeSet attributeSet, int i) {
    Context context2 = context;  // MOVE: copy context to context2
    super(context2, attributeSet, i);
}
```

**Problem**: Simple MOVE but `context2` may be used in subsequent code

---

### A-2: DefaultBHttpServerConnection.java
**File**: `cz/msebera/android/httpclient/impl/DefaultBHttpServerConnection.java`

```java
public DefaultBHttpServerConnection(..., ContentLengthStrategy contentLengthStrategy, ...) {
    ContentLengthStrategy contentLengthStrategy3 = contentLengthStrategy;

    super(..., contentLengthStrategy3 == null
        ? DisallowIdentityContentLengthStrategy.INSTANCE
        : contentLengthStrategy3, ...);

    // contentLengthStrategy3 used after super()
    this.requestParser = (httpMessageParserFactory2 == null ? ... : httpMessageParserFactory2).create(...);
}
```

**Problem**:
- `contentLengthStrategy3` used twice in ternary
- Other variables used after super()

---

### A-3, A-4, A-5: DefaultDrmSessionManager.java (3 cases)
**File**: `com/google/android/exoplayer2/drm/DefaultDrmSessionManager.java`

```java
public DefaultDrmSessionManager(UUID uuid, ExoMediaDrm<T> exoMediaDrm,
        MediaDrmCallback mediaDrmCallback, HashMap<String, String> map) {
    HashMap<String, String> map2 = map;
    this(uuid, exoMediaDrm, mediaDrmCallback,
        map2 == null ? new HashMap<>() : map2, false, 3);
}
```

**Problem**: `map2` used in both condition (`== null`) and else value

---

### A-6: zzbsq.java
**File**: `com/google/android/gms/internal/ads/zzbsq.java`

```java
public zzbsq(Context context, Looper looper, ...) {
    Context applicationContext = context.getApplicationContext();
    super(applicationContext == null ? context : applicationContext, looper, 8, ...);
}
```

**Problem**: `getApplicationContext()` result used twice in ternary

---

### A-7, A-8, A-9, A-10: String.valueOf Pattern (4 cases)
**Files**: `zzga.java`, `zzwq.java`, `zzbn.java` (2 cases)

```java
zza(String str, Throwable th) {
    String strValueOf = String.valueOf(str);
    super(strValueOf.length() != 0
        ? "CodedOutputStream was writing...".concat(strValueOf)
        : new String("CodedOutputStream was writing..."), th);
}
```

**Problem**: `strValueOf` used in condition (`.length() != 0`) and value (`.concat(strValueOf)`)

---

## Type B: Control Flow Pattern (4 cases)

Patterns with **control flow** (if/else, throw, loop) before constructor call.
Java does not allow conditionals before super()/this().

---

### B-1: MultipartReader.java
**File**: `okhttp3/MultipartReader.java`

```java
public MultipartReader(ResponseBody response) throws IOException {
    BufferedSource source = response.getSource();
    MediaType mediaType = response.get$contentType();
    String strParameter = mediaType == null ? null : mediaType.parameter("boundary");

    if (strParameter != null) {
        this(source, strParameter);  // Conditional constructor call
        return;
    }
    throw new ProtocolException("expected the Content-Type to have a boundary parameter");
}
```

**Problem**:
- if condition determines whether this() is called
- If false, throws exception
- Java doesn't allow conditional constructor calls

---

### B-2: CustomCap.java
**File**: `com/google/android/gms/maps/model/CustomCap.java`

```java
public CustomCap(BitmapDescriptor bitmapDescriptor, float f) {
    BitmapDescriptor bitmapDescriptor2 = (BitmapDescriptor) Preconditions.checkNotNull(
        bitmapDescriptor, "bitmapDescriptor must not be null");

    if (f <= 0.0f) {
        throw new IllegalArgumentException("refWidth must be positive");
    }

    super(bitmapDescriptor2, f);
}
```

**Problem**:
- `Preconditions.checkNotNull()` call before super()
- if/throw condition before super()
- Java doesn't allow validation before super()

---

### B-3: zzalh.java
**File**: `com/google/android/gms/internal/ads/zzalh.java`

```java
zzalh(String str, zzajx zzajxVar) {
    List arrayList = zzajxVar.zzh;
    if (arrayList == null) {
        Map map = zzajxVar.zzg;
        arrayList = new ArrayList(map.size());
        for (Map.Entry entry : map.entrySet()) {  // Loop!
            arrayList.add(new zzakg(...));
        }
    }
    this(str, str2, j, j2, j3, j4, arrayList);
}
```

**Problem**:
- if condition and for loop before this()
- Conditional list creation logic

---

### B-4: zzhj.java
**File**: `com/google/android/gms/internal/ads/zzhj.java`

```java
private zzhj(int i, Throwable th, ...) {
    String str3;
    if (i == 0) {
        str3 = "Source error";
    } else if (i != 1) {
        str3 = "Unexpected runtime error";
    } else {
        str3 = str2 + " error, index=" + i3 + ...;
    }

    this(TextUtils.isEmpty(null) ? str3 : str3.concat(": null"), th, ...);
}
```

**Problem**:
- if/else if/else chain determines `str3` value
- Complex string concatenation
- Java doesn't allow conditionals before this()

---

## Type C: Combined Pattern (1 case)

---

### C-1: zzqz.java
**File**: `com/google/android/gms/internal/ads/zzqz.java`

```java
public zzqz(zzaf zzafVar, Throwable th, boolean z, zzqx zzqxVar) {
    String str = "Decoder init failed: " + zzqxVar.zza + ", " + String.valueOf(zzafVar);
    String str2 = zzafVar.zzm;
    String diagnosticInfo = null;

    if (zzew.zza >= 21 && (th instanceof MediaCodec.CodecException)) {
        diagnosticInfo = ((MediaCodec.CodecException) th).getDiagnosticInfo();
    }

    this(str, th, str2, false, zzqxVar, ((MediaCodec.CodecException) th).getDiagnosticInfo(), null);
}
```

**Problem**:
- String concatenation before this()
- if condition determines `diagnosticInfo` value
- Complex initialization logic

---

## Resolvability Analysis

### Type A (Variable reuse) - Theoretically Possible

**Condition**: Inlined expression must have no side effects

| Expression | Side-effect | Inlineable |
|------------|------------|------------|
| `String.valueOf(str)` | None | Yes |
| `map` (parameter) | None | Yes |
| `context.getApplicationContext()` | Uncertain | Risky |
| `contentLengthStrategy` (parameter) | None | Yes |

**Implementation approach**:
1. Direct parameter references can be safely inlined
2. Pure functions like `String.valueOf()` can be inlined
3. General method calls need side-effect analysis

### Type B (Control flow) - Impossible

Java language specification requires constructor's first statement to be super() or this().
This constraint cannot be changed.

**Alternatives**:
- Convert to static factory methods (changes code structure)
- Show warning and keep current code

### Type C (Combined) - Impossible

Multiple patterns combined, resolution is very complex.

---

## Reference: Java Constructor Constraints

In Java, the constructor's first statement must be:
- Either `super(...)` or `this(...)` call
- No code can execute before this

```java
// Java - Invalid
public MyClass(int x) {
    if (x < 0) throw new IllegalArgumentException();  // Not allowed!
    super(x);
}

// Java - Valid
public MyClass(int x) {
    super(validateAndReturn(x));  // Static method call is OK
}

private static int validateAndReturn(int x) {
    if (x < 0) throw new IllegalArgumentException();
    return x;
}
```

This constraint is defined in the Java Language Specification.
Bytecode doesn't have this constraint, which causes decompilation issues.

Kotlin, Scala, and other JVM languages have different rules,
so code written in these languages often encounters this problem when decompiled to Java.

---

## Conclusion

| Type | Count | Resolvability | Notes |
|------|-------|--------------|-------|
| A: Variable reuse | 10 | Partially possible | Side-effect analysis needed |
| B: Control flow | 4 | Impossible | Java language constraint |
| C: Combined | 1 | Impossible | Complex pattern |
| **Total** | **15** | | |

Currently JADX shows a warning and generates the best possible code.
This reflects the Java constructor constraints.
