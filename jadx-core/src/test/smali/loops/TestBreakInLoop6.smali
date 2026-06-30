.class public Lloops/TestBreakInLoop6;
.super Ljava/lang/Object;


.method public test()J
    .registers 19
    move-object/from16 v0, p0
    const-wide v1, 0x1L
    iget-object v3, v0, Ltest/Test;->j:[Ltest/Test;
    array-length v4, v3
    const/4 v6, 0x0
    :goto_c
    if-ge v6, v4, :cond_64

    aget-object v7, v3, v6
    invoke-interface {v7}, Ltest/Test;->h()J

    move-result-wide v8
    const-wide v11, 0x2L

    cmp-long v13, v8, v11

    if-eqz v13, :cond_4e
    cmp-long v13, v1, v11

    if-nez v13, :cond_4e
    move-wide v1, v8
    iget-object v11, v0, Ltest/Test;->i:[Ltest/Test;

    array-length v12, v11

    const/4 v13, 0x0

    :goto_28
    if-ge v13, v12, :cond_4e

    aget-object v14, v11, v13
    if-ne v14, v7, :cond_2f
    goto :cond_4e
    :cond_2f
    invoke-interface {v14, v1, v2}, Ltest/Test;->f(J)J
    move-result-wide v15
    cmp-long v17, v15, v1
    if-nez v17, :cond_3a
    add-int/lit8 v13, v13, 0x1
    goto :goto_28

    :cond_3a
    new-instance v3, Ljava/lang/IllegalStateException;
    invoke-direct {v3}, Ljava/lang/IllegalStateException;-><init>()V
    throw v3

    :cond_4e
    add-int/lit8 v6, v6, 0x1
    goto :goto_c

    :cond_64
    return-wide v1
.end method