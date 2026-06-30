.class public LTestIfInLoop4;
.super Ljava/lang/Object;

.method public test()Z
    .registers 5

    move-object/from16 v0, p0

    const/4 v2, 0x0
    const/4 v3, 0x1

    :goto_0
    iget v1, v0, LTestIfInLoop4;->x:I

    if-ge v2, v1, :goto_1
    if-gtz v2, :cond
    if-gez v2, :cond

    if-ltz v2, :goto_1
    if-ge v2, v1, :goto_1

    goto :goto_1

    :cond
    goto :goto_0

    :goto_1
    return v3
.end method

