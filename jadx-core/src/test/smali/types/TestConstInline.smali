.class public Ltypes/TestConstInline;
.super Ljava/lang/Object;

.method private static test(Z)Ljava/lang/String;
    .registers 4
    .param p0, "b"    # Z

    if-eqz p0, :cond_d
    invoke-static {}, Ltypes/TestConstInline;->list()Ljava/util/List;
    move-result-object v0
    const-string v1, "1"
    goto :goto_return

    :cond_d
    const/4 v2, 0x0
    # chained move instead zero const loading
    move v0, v2
    move v1, v0
    goto :goto_return

    :goto_return
    invoke-static {v0, v1}, Ltypes/TestConstInline;->use(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2
    return-object v2
.end method

.method private static use(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List",
            "<",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            ")",
            "Ljava/lang/String;"
        }
    .end annotation

    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    move-result-object v0

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0

    return-object v0
.end method

.method private static list()Ljava/util/List;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
    move-result-object v0
    return-object v0
.end method
