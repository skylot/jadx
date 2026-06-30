.class Lvariables/TestVariables8;
.super Ljava/lang/Object;

.method private static test(III)J
    .registers 13
    .param p0, "k"    # I
    .param p1, "j"    # I
    .param p2, "i"    # I

    .prologue
    const-wide/16 v4, -0x1
    const/4 v8, 0x1

    .line 614
    shl-int/lit8 v6, p0, 0x6
    xor-int/lit8 v7, p1, -0x1
    and-int/lit8 v7, v7, 0x3f
    or-int v0, v6, v7

    .local v0, "e":I
    const/4 v1, 0x6

    .line 616
    .local v1, "l":I
    :goto_c
    if-ltz v1, :cond_13

    shl-int v6, v8, v1
    and-int/2addr v6, v0

    if-eqz v6, :cond_16

    .line 619
    :cond_13
    if-ge v1, v8, :cond_19

    .line 633
    :cond_15
    return-wide v4

    .line 617
    :cond_16
    add-int/lit8 v1, v1, -0x1
    goto :goto_c

    .line 620
    :cond_19
    shl-int v0, v8, v1
    .line 621
    add-int/lit8 v1, v0, -0x1
    .line 622
    and-int/2addr p1, v1
    .line 623
    if-eq p1, v1, :cond_15

    .line 624
    and-int/2addr p2, v1
    .line 625
    add-int/lit8 v6, p1, 0x1
    rsub-int/lit8 v6, v6, 0x40
    ushr-long v2, v4, v6

    .line 626
    .local v2, "m":J
    ushr-long v6, v2, p2
    sub-int v8, v0, p2
    shl-long v8, v2, v8
    or-long v2, v6, v8

    .line 627
    move-wide v4, v2
    .line 628
    .local v4, "r":J
    move p2, v0
    .line 629
    :goto_31
    const/16 v6, 0x40
    if-ge p2, v6, :cond_15

    .line 630
    shl-long v6, v2, p2
    or-long/2addr v4, v6
    .line 631
    add-int/2addr p2, v0
    goto :goto_31
.end method
