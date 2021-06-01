.class public Lothers/TestConstructorBranched;
.super Ljava/lang/Object;

.method public test(Ljava/util/Collection;)Ljava/util/Set;
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/Collection",
            "<",
            "Ljava/lang/String;",
            ">;)",
            "Ljava/util/Set",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    new-instance v0, Ljava/util/HashSet;

    if-nez p1, :cond_d
    invoke-direct {v0}, Ljava/util/HashSet;-><init>()V
    goto :goto_7

    :cond_d
    invoke-direct {v0, p1}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V

    :goto_7
    const-string v1, "end"
    invoke-interface {v0, v1}, Ljava/util/Set;->add(Ljava/lang/Object;)Z
    return-object v0
.end method
