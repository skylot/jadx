.class public LTest1;
.super Ljava/lang/Object;

.method public test()V
    .registers 3
    const/4 v0, 0x0
    move-object v1, v0
    check-cast v1, LT1;
    invoke-virtual {v0}, LT1;->foo1()V
    move-object v1, v0
    check-cast v1, LT2;
    invoke-virtual {v0}, LT2;->foo2()V
    return-void
.end method
