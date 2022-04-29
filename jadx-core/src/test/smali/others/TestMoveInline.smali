.class public Lothers/TestMoveInline;
.super Ljava/lang/Object;

.field private h:[B
.field private a:Lothers/TestMoveInline;

.method public k([BII)V
    .registers 5
    return-void
.end method

.method public test(I)V
    .registers 7

    const/4 v0, 0x0
    move v1, v0

    :goto_2
    and-int/lit8 v2, p1, -0x80
    if-nez v2, :cond_13

    .line 1
    iget-object v2, p0, Lothers/TestMoveInline;->h:[B
    add-int/lit8 v3, v1, 0x1
    int-to-byte p1, p1
    aput-byte p1, v2, v1

    .line 2
    iget-object p1, p0, Lothers/TestMoveInline;->a:Lothers/TestMoveInline;
    invoke-virtual {p1, v2, v0, v3}, Lothers/TestMoveInline;->k([BII)V
    return-void

    .line 3
    :cond_13
    iget-object v2, p0, Lothers/TestMoveInline;->h:[B
    add-int/lit8 v3, v1, 0x1
    and-int/lit8 v4, p1, 0x7f
    or-int/lit16 v4, v4, 0x80
    int-to-byte v4, v4
    aput-byte v4, v2, v1
    ushr-int/lit8 p1, p1, 0x7
    move v1, v3
    goto :goto_2
.end method
