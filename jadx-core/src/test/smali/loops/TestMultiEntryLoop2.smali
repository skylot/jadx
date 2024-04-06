.class public Lloops/TestMultiEntryLoop2;
.super Ljava/lang/Object;

.field public list:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.method private test(Ljava/lang/String;II)V
    .registers 14

    if-ne p2, p3, :cond_3
    return-void

    :cond_3
    invoke-virtual {p1, p2}, Ljava/lang/String;->charAt(I)C

    move-result v0
    const/16 v1, 0x2f
    const-string v2, ""
    const/4 v3, 0x1

    if-eq v0, v1, :cond_1e
    const/16 v1, 0x5c
    if-ne v0, v1, :cond_13

    goto :goto_1e

    :cond_13
    iget-object v0, p0, Lloops/TestMultiEntryLoop2;->list:Ljava/util/List;
    invoke-interface {v0}, Ljava/util/List;->size()I
    move-result v1
    sub-int/2addr v1, v3
    invoke-interface {v0, v1, v2}, Ljava/util/List;->set(ILjava/lang/Object;)Ljava/lang/Object;
    goto :goto_29

    :cond_1e
    :goto_1e
    iget-object v0, p0, Lloops/TestMultiEntryLoop2;->list:Ljava/util/List;
    invoke-interface {v0}, Ljava/util/List;->clear()V
    .line 4
    iget-object v0, p0, Lloops/TestMultiEntryLoop2;->list:Ljava/util/List;
    invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z
    goto :goto_41

    :cond_29
    :goto_29
    move v6, p2
    if-ge v6, p3, :cond_44
    const-string p2, "/\\"
    .line 5
    invoke-static {p1, v6, p3, p2}, Lloops/TestMultiEntryLoop2;->delimiterOffset(Ljava/lang/String;IILjava/lang/String;)I
    move-result p2
    if-ge p2, p3, :cond_36
    move v0, v3
    goto :goto_37

    :cond_36
    const/4 v0, 0x0
    :goto_37
    const/4 v9, 0x1
    move-object v4, p0
    move-object v5, p1
    move v7, p2
    move v8, v0
    .line 6
    invoke-direct/range {v4 .. v9}, Lloops/TestMultiEntryLoop2;->push(Ljava/lang/String;IIZZ)V
    if-eqz v0, :cond_29

    :goto_41
    add-int/lit8 p2, p2, 0x1
    goto :goto_29

    :cond_44
    return-void
.end method

.method private push(Ljava/lang/String;IIZZ)V
    .locals 0
    return-void
.end method

.method private delimiterOffset(Ljava/lang/String;IILjava/lang/String;)I
    .locals 1
    const/4 v0, 0x0
    return v0
.end method
