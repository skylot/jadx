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

    sget-object v0, Linline/Lambda;->INSTANCE:Linline/Lambda;
    invoke-static {p1, v0}, Linline/TestCls;->toMap(Ljava/util/List;Ljava/util/function/Function;)Ljava/util/Map;
    move-result-object v0
    return-object v0
.end method


.method private static synthetic lambda$toMap$0(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 1
    return-object p0
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

    invoke-interface {p0}, Ljava/util/List;->stream()Ljava/util/stream/Stream;
    move-result-object v0
    invoke-custom {}, call_site_0("apply", ()Ljava/util/function/Function;, (Ljava/lang/Object;)Ljava/lang/Object;, invoke-static@Linline/TestCls;->lambda$toMap$0(Ljava/lang/Object;)Ljava/lang/Object;, (Ljava/lang/Object;)Ljava/lang/Object;)@Ljava/lang/invoke/LambdaMetafactory;->metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    move-result-object v1
    invoke-static {v1, p1}, Ljava/util/stream/Collectors;->toMap(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/stream/Collector;
    move-result-object v1
    invoke-interface {v0, v1}, Ljava/util/stream/Stream;->collect(Ljava/util/stream/Collector;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Ljava/util/Map;
    return-object v0
.end method
