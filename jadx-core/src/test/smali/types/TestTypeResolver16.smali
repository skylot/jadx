.class public Ltypes/TestTypeResolver16;
.super Ljava/lang/Object;

.method public final test(Ljava/util/List;Ljava/util/Set;Ljava/util/function/Function;)Ljava/util/List;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            "K:",
            "Ljava/lang/Object;",
            ">(",
            "Ljava/util/List<",
            "+TT;>;",
            "Ljava/util/Set<",
            "+TT;>;",
            "Ljava/util/function/Function<",
            "-TT;+TK;>;)",
            "Ljava/util/List<",
            "TT;>;"
        }
    .end annotation

    const-string v0, "distinctBy"

    invoke-static {p3, v0}, Ltypes/TestTypeResolver16;->checkParameterIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V

    if-eqz p2, :cond_1

    if-eqz p1, :cond_0

    .line 85
    move-object v0, p1

    check-cast v0, Ljava/util/Collection;

    check-cast p2, Ljava/lang/Iterable;

    invoke-static {v0, p2, p3}, Ltypes/TestTypeResolver16;->union(Ljava/util/Collection;Ljava/lang/Iterable;Ljava/util/function/Function;)Ljava/util/List;

    move-result-object p2

    goto :goto_0

    :cond_0
    const/4 p2, 0x0

    :goto_0
    if-eqz p2, :cond_1

    move-object p1, p2

    :cond_1
    if-eqz p1, :cond_2

    goto :goto_1

    :cond_2
    invoke-static {}, Ltypes/TestTypeResolver16;->emptyList()Ljava/util/List;

    move-result-object p1

    :goto_1
    return-object p1
.end method


.method public static final union(Ljava/util/Collection;Ljava/lang/Iterable;Ljava/util/function/Function;)Ljava/util/List;
    .locals 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            "K:",
            "Ljava/lang/Object;",
            ">(",
            "Ljava/util/Collection<",
            "+TT;>;",
            "Ljava/lang/Iterable<",
            "+TT;>;",
            "Ljava/util/function/Function<",
            "-TT;+TK;>;)",
            "Ljava/util/List<",
            "TT;>;"
        }
    .end annotation

	const/4 v0, 0x0
    return-object v0
.end method

.method public static checkParameterIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V
    .locals 0
    return-void
.end method

.method public static final emptyList()Ljava/util/List;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">()",
            "Ljava/util/List<",
            "TT;>;"
        }
    .end annotation

	const/4 v0, 0x0
    return-object v0
.end method
