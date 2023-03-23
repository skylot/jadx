.class public Linline/TestCls;
.super Ljava/lang/Object;

.method public test(Ljava/util/List;)Ljava/util/Map;
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Ljava/util/List",
            "<+TT;>;)",
            "Ljava/util/Map",
            "<TT;TT;>;"
        }
    .end annotation

    sget-object v0, Linline/Lambda$1;->INSTANCE:Linline/Lambda$1;
    invoke-static {p1, v0}, Linline/TestCls;->toMap(Ljava/util/List;Ljava/util/function/Function;)Ljava/util/Map;
    move-result-object v0
    return-object v0
.end method

.method private static toMap(Ljava/util/List;Ljava/util/function/Function;)Ljava/util/Map;
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Ljava/util/List",
            "<+TT;>;",
            "Ljava/util/function/Function",
            "<TT;TT;>;)",
            "Ljava/util/Map",
            "<TT;TT;>;"
        }
    .end annotation

    const/4 v0, 0x0
    return-object v0
.end method
