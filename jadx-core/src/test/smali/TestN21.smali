.class public LTestN21;
.super Ljava/lang/Object;

.method private static test([BI)I
    .locals 5

    const/4 v1, 0x0

    const/16 v0, 0xe

    aget-byte v0, p0, v0

    shl-int/lit8 v0, v0, 0x10

    move v2, v1

    :goto_0
    if-nez v2, :cond_1

    const/4 v2, 0x3

    and-int/lit16 v3, p1, 0xff

    :try_start_0
    aget-byte v3, p0, v3

    and-int/lit16 v3, v3, 0xff

    shr-int/lit8 v4, p1, 0x8

    and-int/lit16 v4, v4, 0xff

    aget-byte v4, p0, v4

    and-int/lit16 v4, v4, 0xff

    shl-int/lit8 v4, v4, 0x8

    or-int/2addr v3, v4

    shr-int/lit8 v4, p1, 0x10

    and-int/lit16 v4, v4, 0xff

    aget-byte v4, p0, v4

    and-int/lit16 v4, v4, 0xff

    shl-int/lit8 v4, v4, 0x10

    or-int/2addr v3, v4

    shr-int/lit8 v4, p1, 0x18

    and-int/lit16 v4, v4, 0xff

    aget-byte v0, p0, v4
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1

    shl-int/lit8 v0, v0, 0x18

    or-int/2addr v0, v3

    :cond_0
    :goto_1
    return v0

    :catch_0
    move-exception v2

    :cond_1
    if-nez v1, :cond_0

    const/4 v1, 0x2

    and-int/lit8 v2, p1, 0x7f

    :try_start_1
    aget-byte v0, p0, v2
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    shr-int/lit8 v0, v0, 0x8

    goto :goto_1

    :catch_1
    move-exception v3

    goto :goto_0
.end method

