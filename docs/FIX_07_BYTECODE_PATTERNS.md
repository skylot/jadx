# Fix #7: Bytecode Pattern Deep Dive

## Overview

This document provides an in-depth analysis of bytecode patterns that cause "Illegal instructions before constructor call" errors.

---

## Pattern Transformation Flow

```
┌──────────────────┐
│  Kotlin Source   │
│  (High-level)    │
└────────┬─────────┘
         │ kotlinc
         ▼
┌──────────────────┐
│  JVM Bytecode    │
│  (Stack-based)   │
└────────┬─────────┘
         │ d8/R8
         ▼
┌──────────────────┐
│  DEX Bytecode    │  ← JADX reads this
│  (Register-based)│
└────────┬─────────┘
         │ JADX
         ▼
┌──────────────────┐
│  Java Source     │  ← May be invalid!
│  (High-level)    │
└──────────────────┘
```

---

## Pattern A: Variable Reuse in Ternary Operator

### A.1: String.valueOf Pattern

**Original Kotlin** (inferred):
```kotlin
class OutOfSpaceException(message: String, cause: Throwable?) :
    IOException(buildMessage(message), cause) {

    companion object {
        private fun buildMessage(msg: String): String {
            val str = msg.toString()  // String.valueOf equivalent
            return if (str.isNotEmpty()) "CodedOutputStream was writing to $str"
                   else "CodedOutputStream was writing..."
        }
    }
}
```

**DEX Bytecode**:
```smali
.method constructor <init>(Ljava/lang/String;Ljava/lang/Throwable;)V
    .registers 4

    # v0 = String.valueOf(str)
    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v0

    # v1 = v0.length()
    invoke-virtual {v0}, Ljava/lang/String;->length()I
    move-result v1

    # if (v1 != 0) use v0, else use constant
    if-eqz v1, :cond_use_constant

    # Case: v0.length() != 0 → concat with v0
    const-string v1, "CodedOutputStream was writing to "
    invoke-virtual {v1, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    goto :goto_call_super

    :cond_use_constant
    # Case: v0.length() == 0 → use constant
    const-string v0, "CodedOutputStream was writing..."

    :goto_call_super
    # super(v0, p2)
    invoke-direct {p0, v0, p2}, Ljava/io/IOException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V
    return-void
.end method
```

**Why Variable Reuse Occurs**:
```
v0 = String.valueOf(p1)     ← Definition
     ↓
if (v0.length() != 0)       ← First use (condition)
     ↓
    "...".concat(v0)        ← Second use (value)
```

**Why Inlining Fails**:
```java
// If we inline v0:
super(String.valueOf(p1).length() != 0
    ? "CodedOutputStream...".concat(String.valueOf(p1))  // ❌ Called TWICE!
    : "CodedOutputStream...",
    cause);
```

The method `String.valueOf()` would be called twice - potentially different results if the object's `toString()` has side effects.

---

### A.2: getApplicationContext Pattern

**Original Kotlin** (inferred from Google Play Services):
```kotlin
class GoogleApiClient(context: Context, looper: Looper, ...) :
    BaseClient(context.applicationContext ?: context, looper, ...) {
}
```

**DEX Bytecode**:
```smali
.method public constructor <init>(Landroid/content/Context;Landroid/os/Looper;...)V
    .registers 6

    # v0 = context.getApplicationContext()
    invoke-virtual {p1}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
    move-result-object v0

    # if (v0 == null) use p1 (original context), else use v0
    if-nez v0, :cond_use_app_context
    move-object v0, p1

    :cond_use_app_context
    # super(v0, looper, ...)
    invoke-direct {p0, v0, p2, ...}, LBaseClass;-><init>(Landroid/content/Context;...)V
    return-void
.end method
```

**Analysis**:
```
v0 = context.getApplicationContext()   ← Potentially returns null
     ↓
if (v0 != null)                        ← Check result
     ↓
use v0 as context                      ← Reuse same variable
```

**Why Problematic**:
- `getApplicationContext()` may have side effects
- Calling twice could yield different results (rare but possible)
- Memory/performance concern for repeated calls

---

## Pattern B: Control Flow (if/throw)

### B.1: Elvis with Throw Pattern

**Original Kotlin**:
```kotlin
class MultipartReader(
    private val source: BufferedSource,
    val boundary: String
) {
    constructor(response: ResponseBody) : this(
        source = response.source(),
        boundary = response.contentType()?.parameter("boundary")
            ?: throw ProtocolException("boundary required")
    )
}
```

**DEX Bytecode** (annotated):
```smali
.method public constructor <init>(Lokhttp3/ResponseBody;)V
    .registers 4

    # ============ STAGE 1: Compute source ============
    invoke-virtual {p1}, Lokhttp3/ResponseBody;->source()Lokio/BufferedSource;
    move-result-object v0    # v0 = response.source()

    # ============ STAGE 2: Compute boundary ============
    invoke-virtual {p1}, Lokhttp3/ResponseBody;->contentType()Lokhttp3/MediaType;
    move-result-object v1    # v1 = response.contentType()

    # Safe call: v1?.parameter("boundary")
    if-nez v1, :cond_get_param
    const/4 v1, 0x0          # v1 = null
    goto :goto_check

    :cond_get_param
    const-string v2, "boundary"
    invoke-virtual {v1, v2}, Lokhttp3/MediaType;->parameter(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1    # v1 = contentType.parameter("boundary")

    :goto_check
    # ============ STAGE 3: Elvis operator (?:) ============
    if-eqz v1, :cond_throw   # if boundary is null, throw

    # Happy path: call this(source, boundary)
    invoke-direct {p0, v0, v1}, Lokhttp3/MultipartReader;-><init>(Lokio/BufferedSource;Ljava/lang/String;)V
    return-void

    :cond_throw
    # Sad path: throw exception
    new-instance v0, Ljava/net/ProtocolException;
    const-string v1, "expected the Content-Type to have a boundary parameter"
    invoke-direct {v0, v1}, Ljava/net/ProtocolException;-><init>(Ljava/lang/String;)V
    throw v0
.end method
```

**Control Flow Graph**:
```
            ┌─────────────────┐
            │ response.source()│
            │ response.contentType()│
            └────────┬────────┘
                     │
            ┌────────▼────────┐
            │ contentType == null?│
            └────────┬────────┘
                    /  \
                  yes   no
                  /       \
         ┌──────▼──┐   ┌──▼──────────┐
         │ v1=null │   │ v1=parameter()│
         └────┬────┘   └──────┬──────┘
              │               │
              └───────┬───────┘
                      │
            ┌─────────▼─────────┐
            │ v1 (boundary) == null?│
            └─────────┬─────────┘
                     / \
                   yes  no
                   /      \
          ┌───────▼───┐  ┌─▼────────────┐
          │   THROW   │  │ this(v0,v1)  │
          └───────────┘  │   RETURN     │
                         └──────────────┘
```

**Why Unsolvable**:
- The `this()` call is genuinely inside an `if` block
- Java requires `this()`/`super()` as the **unconditional first statement**
- Cannot restructure: both branches lead to termination (return or throw)

---

### B.2: Preconditions with Throw Pattern

**Original Java/Kotlin**:
```kotlin
class CustomCap(bitmapDescriptor: BitmapDescriptor, refWidth: Float) : Cap(...) {
    init {
        Preconditions.checkNotNull(bitmapDescriptor, "bitmapDescriptor must not be null")
        require(refWidth > 0f) { "refWidth must be positive" }
    }

    constructor(bitmap: BitmapDescriptor, width: Float) : super(bitmap, width)
}
```

**DEX Bytecode**:
```smali
.method public constructor <init>(Lcom/google/android/gms/maps/model/BitmapDescriptor;F)V
    .registers 4

    # Preconditions.checkNotNull(bitmapDescriptor, ...)
    const-string v0, "bitmapDescriptor must not be null"
    invoke-static {p1, v0}, Lcom/google/android/gms/common/internal/Preconditions;->checkNotNull(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Lcom/google/android/gms/maps/model/BitmapDescriptor;

    # if (refWidth <= 0) throw IllegalArgumentException
    const/4 v1, 0x0
    cmpl-float v1, p2, v1    # compare p2 (refWidth) with 0.0
    if-gtz v1, :cond_valid   # if > 0, skip throw

    new-instance v0, Ljava/lang/IllegalArgumentException;
    const-string v1, "refWidth must be positive"
    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V
    throw v0

    :cond_valid
    # Now safe to call super
    invoke-direct {p0, v0, p2}, Lcom/google/android/gms/maps/model/Cap;-><init>(Lcom/google/android/gms/maps/model/BitmapDescriptor;F)V

    # Field assignments
    iput-object p1, p0, Lcom/google/android/gms/maps/model/CustomCap;->bitmapDescriptor:Lcom/google/android/gms/maps/model/BitmapDescriptor;
    iput p2, p0, Lcom/google/android/gms/maps/model/CustomCap;->refWidth:F
    return-void
.end method
```

**Problem Structure**:
```
Validation   →  if (invalid) throw  →  super()  →  field init
    ↑                                     ↑
    └── NOT ALLOWED IN JAVA ──────────────┘
```

---

## Pattern C: Loop Before Constructor

### C.1: Collection Building Pattern

**Original Kotlin** (inferred):
```kotlin
class DataHolder(name: String, data: DataObject) {
    constructor(name: String, rawData: RawData) : this(
        name,
        DataObject(
            rawData.items?.map { DataItem(it.key, it.value) }
                ?: rawData.map.entries.map { DataItem(it.key, it.value) }
        )
    )
}
```

**DEX Bytecode**:
```smali
.method constructor <init>(Ljava/lang/String;LRawData;)V
    .registers 7

    # Check if items list exists
    iget-object v0, p2, LRawData;->items:Ljava/util/List;
    if-nez v0, :cond_use_items

    # Items is null - build from map
    iget-object v0, p2, LRawData;->map:Ljava/util/Map;
    invoke-interface {v0}, Ljava/util/Map;->size()I
    move-result v1
    new-instance v2, Ljava/util/ArrayList;
    invoke-direct {v2, v1}, Ljava/util/ArrayList;-><init>(I)V

    # for (entry : map.entrySet())
    invoke-interface {v0}, Ljava/util/Map;->entrySet()Ljava/util/Set;
    move-result-object v0
    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;
    move-result-object v0

    :loop_start
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
    move-result v1
    if-eqz v1, :loop_end

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Ljava/util/Map$Entry;

    # Create DataItem from entry
    new-instance v3, LDataItem;
    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;
    move-result-object v4
    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;
    move-result-object v1
    invoke-direct {v3, v4, v1}, LDataItem;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V

    # Add to list
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    goto :loop_start

    :loop_end
    move-object v0, v2

    :cond_use_items
    # Now v0 contains the list - call this()
    invoke-direct {p0, p1, v0}, LDataHolder;-><init>(Ljava/lang/String;LDataObject;)V
    return-void
.end method
```

**Problem**:
- A `for` loop executes before `this()`
- Java doesn't allow any statement (including loops) before `this()`/`super()`

---

## Bytecode Instruction Reference

### Instructions That Appear Before Constructor Call

| Instruction | Purpose | Java Equivalent |
|-------------|---------|-----------------|
| `invoke-static` | Static method call | `ClassName.method()` |
| `invoke-virtual` | Instance method call | `obj.method()` |
| `invoke-interface` | Interface method call | `iface.method()` |
| `if-eqz` / `if-nez` | Null/zero check | `if (x == null)` |
| `if-eq` / `if-ne` | Equality check | `if (x == y)` |
| `if-lt` / `if-gt` etc. | Comparison | `if (x < y)` |
| `move-result-object` | Store method result | `var = method()` |
| `const-string` | Load string constant | `"literal"` |
| `new-instance` | Create object | `new ClassName()` |
| `throw` | Throw exception | `throw new Exception()` |
| `goto` | Unconditional jump | (control flow) |

### Constructor Call Instructions

| Instruction | Meaning |
|-------------|---------|
| `invoke-direct {p0, ...}, <init>` | Call `this()` |
| `invoke-direct {p0, ...}, Super;-><init>` | Call `super()` |

---

## Summary: Why Each Pattern is Problematic

| Pattern | Root Cause | Java Limitation |
|---------|------------|-----------------|
| **A: Variable Reuse** | Same value needed in condition and result | Cannot duplicate side-effect expressions |
| **B: Control Flow** | Constructor call is conditional | `this()`/`super()` must be unconditional |
| **C: Loop** | Need iteration before constructor | No statements allowed before `this()` |

---

## Appendix: JVM vs DEX Bytecode Comparison

### JVM Bytecode (Stack-based)
```
aload_1           // Push parameter onto stack
invokevirtual     // Pop, call method, push result
astore_2          // Pop and store in local variable
```

### DEX Bytecode (Register-based)
```smali
invoke-virtual {p1}, Method    # Call with register p1
move-result-object v2          # Store result in register v2
```

Both allow code before `<init>` call - the restriction is purely in Java source syntax.
