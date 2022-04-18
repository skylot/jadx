.class public Ltypes/TestTypeResolver20;
.super Ljava/lang/Object;
.source "SourceFile"


.method public static final max(Lkotlin/sequences/Sequence;)Ljava/lang/Comparable;
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T::",
            "Ljava/lang/Comparable<",
            "-TT;>;>(",
            "Lkotlin/sequences/Sequence<",
            "+TT;>;)TT;"
        }
    .end annotation

    .line 1147
    invoke-interface {p0}, Lkotlin/sequences/Sequence;->iterator()Ljava/util/Iterator;
    move-result-object p0

    .line 1148
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z
    move-result v0

    if-nez v0, :cond_11

    const/4 p0, 0x0
    return-object p0

    .line 1149
    :cond_11
    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Ljava/lang/Comparable;

    .line 1150
    :cond_17
    :goto_17
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z
    move-result v1

    if-eqz v1, :cond_2b

    .line 1151
    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Ljava/lang/Comparable;

    .line 1152
    invoke-interface {v0, v1}, Ljava/lang/Comparable;->compareTo(Ljava/lang/Object;)I
    move-result v2

    if-gez v2, :cond_17

    move-object v0, v1
    goto :goto_17

    :cond_2b
    return-object v0
.end method
