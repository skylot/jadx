.class public Lloops/TestLoopRestore;
.super Ljava/lang/Object;
.source "SourceFile.java"

.method private test([B)Ljava/lang/String;
    .registers 10

    const/16 v0, 0x10
    new-array v0, v0, [C
    fill-array-data v0, :array_3c

    :try_start_7
    const-string v1, "MD5"
    invoke-static {v1}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    move-result-object v1

    invoke-virtual {v1, p1}, Ljava/security/MessageDigest;->update([B)V
    invoke-virtual {v1}, Ljava/security/MessageDigest;->digest()[B
    move-result-object p1

    array-length v1, p1
    mul-int/lit8 v2, v1, 0x2
    new-array v2, v2, [C
    :try_end_19
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_19} :catch_3a

    const/4 v3, 0x0
    const/4 v4, 0x0

    :goto_1b
    if-ge v3, v1, :cond_34

    aget-byte v5, p1, v3
    add-int/lit8 v6, v4, 0x1
    ushr-int/lit8 v7, v5, 0x4
    and-int/lit8 v7, v7, 0xf
    aget-char v7, v0, v7
    aput-char v7, v2, v4
    add-int/lit8 v4, v6, 0x1
    and-int/lit8 v5, v5, 0xf
    aget-char v5, v0, v5
    aput-char v5, v2, v6
    add-int/lit8 v3, v3, 0x1
    goto :goto_1b

    :cond_34
    new-instance p1, Ljava/lang/String;
    invoke-direct {p1, v2}, Ljava/lang/String;-><init>([C)V
    return-object p1

    :catch_3a
    const/4 p1, 0x0
    return-object p1

    :array_3c
    .array-data 2
        0x30s
        0x31s
        0x32s
        0x33s
        0x34s
        0x35s
        0x36s
        0x37s
        0x38s
        0x39s
        0x61s
        0x62s
        0x63s
        0x64s
        0x65s
        0x66s
    .end array-data
.end method
