.class Lvariables/TestVariables7;
.super Ljava/lang/Object;

.method test([BII)I
    .registers 12
    .param p1, "a"    # [B
    .param p2, "b"    # I
    .param p3, "c"    # I

    .prologue
    .line 290
    invoke-static {p3}, Ljava/lang/Integer;->toOctalString(I)Ljava/lang/String;
    move-result-object v3

    .line 291
    .local v3, "oct":Ljava/lang/String;
    invoke-virtual {v3}, Ljava/lang/String;->length()I
    move-result v2

    .line 292
    .local v2, "len":I
    sub-int v4, p2, v2

    .line 293
    .local v4, "off":I
    const/4 v5, 0x0

    .line 294
    .local v5, "sum":I
    const/4 v1, 0x0
    .local v1, "j":I
    :goto_c
    if-lt v1, v2, :cond_f

    .line 299
    return v5

    .line 295
    :cond_f
    invoke-virtual {v3, v1}, Ljava/lang/String;->charAt(I)C
    move-result v0

    .line 296
    .local v0, "ch":C
    and-int/lit16 v6, v0, 0xff
    add-int/lit8 v6, v6, -0x30
    add-int/2addr v5, v6

    .line 297
    add-int v6, v4, v1
    int-to-byte v7, v0
    aput-byte v7, p1, v6

    .line 294
    add-int/lit8 v1, v1, 0x1
    goto :goto_c
.end method
