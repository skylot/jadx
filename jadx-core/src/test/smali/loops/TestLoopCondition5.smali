.class public LTestLoopCondition5;
.super Ljava/lang/Object;
.source "TestLoopCondition5.java"

.method private static lastIndexOf([IIII)I
    .locals 1

    add-int/lit8 p3, p3, -0x1

    :goto_0
    const/4 v0, -0x1

    if-lt p3, p2, :cond_1

    .line 219
    aget v0, p0, p3

    if-ne v0, p1, :cond_0

    return p3

    :cond_0
    add-int/lit8 p3, p3, -0x1

    goto :goto_0

    :cond_1
    move p3, v0

    return p3
.end method
