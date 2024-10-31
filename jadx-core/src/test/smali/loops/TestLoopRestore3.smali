.class public Lloops/TestLoopRestore3;
.super Ljava/lang/Object;

.method public final b(Ljava/lang/String;Lb/U53$b;)V
    .registers 8

    iget-object v0, p0, Lb/X53;->e:Ljava/util/concurrent/atomic/AtomicReference;
    :goto_2
    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;
    move-result-object v1
    move-object v2, v1
    check-cast v2, Ljava/util/List;
    move-object v3, v2
    check-cast v3, Ljava/lang/Iterable;
    instance-of v4, v3, Ljava/util/Collection;
    if-eqz v4, :cond_1a

    move-object v4, v3
    check-cast v4, Ljava/util/Collection;
    invoke-interface {v4}, Ljava/util/Collection;->isEmpty()Z
    move-result v4
    if-eqz v4, :cond_1a
    goto :goto_33

    :cond_1a
    invoke-interface {v3}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;
    move-result-object v3

    :cond_1e
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z
    move-result v4
    if-eqz v4, :cond_33

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v4
    check-cast v4, Lb/X53$c;
    iget-object v4, v4, Lb/X53$c;->b:Ljava/lang/String;
    invoke-static {v4, p1}, Lkotlin/jvm/internal/Intrinsics;->a(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v4
    if-eqz v4, :cond_1e
    goto :goto_40

    :cond_33
    :goto_33
    check-cast v2, Ljava/util/Collection;
    new-instance v3, Lb/X53$c;
    sget-object v4, Lb/Pd2;->a:Lb/Pd2;
    invoke-direct {v3, p2, p1, v4}, Lb/X53$c;-><init>(Lb/U53$b;Ljava/lang/String;Ljava/util/List;)V
    invoke-static {v2, v3}, Lb/R31;->a0(Ljava/util/Collection;Ljava/lang/Object;)Ljava/util/ArrayList;
    move-result-object v2

    :cond_40
    :goto_40
    invoke-virtual {v0, v1, v2}, Ljava/util/concurrent/atomic/AtomicReference;->compareAndSet(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v3
    if-eqz v3, :cond_47
    return-void

    :cond_47
    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;
    move-result-object v3
    if-eq v3, v1, :cond_40
    goto :goto_2
.end method
