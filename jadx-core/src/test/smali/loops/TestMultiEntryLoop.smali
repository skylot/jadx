.class public Lloops/TestMultiEntryLoop;
.super Ljava/lang/Object;

.field private static arr:[B

.method private static test(III)Ljava/lang/String;
    .registers 9

    mul-int/lit8 p1, p1, 0x2

    rsub-int/lit8 p1, p1, 0x6f

    mul-int/lit8 p0, p0, 0x2

    add-int/lit8 p0, p0, 0x1c

    mul-int/lit8 p2, p2, 0x2

    add-int/lit8 p2, p2, 0x4

    new-instance v0, Ljava/lang/String;

    const/4 v5, -0x1

    sget-object v4, Lloops/TestMultiEntryLoop;->arr:[B

    new-array v1, p0, [B

    add-int/lit8 p0, p0, -0x1

    if-nez v4, :cond_1e

    move v2, p1

    move v3, p2

    :goto_19
    add-int/2addr v2, v3

    add-int/lit8 p1, v2, -0x8

    add-int/lit8 p2, p2, 0x1

    :cond_1e
    add-int/lit8 v5, v5, 0x1

    int-to-byte v2, p1

    aput-byte v2, v1, v5

    if-ne v5, p0, :cond_2a

    invoke-direct {v0, v1}, Ljava/lang/String;-><init>([B)V

    return-object v0

    :cond_2a
    move v2, p1

    aget-byte v3, v4, p2

    goto :goto_19
.end method
