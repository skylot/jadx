.class public LTestInlineVarArg;
.super Ljava/lang/Object;

.method public static varargs f([Ljava/lang/String;)V
    .registers 1
    return-void
.end method

.method public test()V
    .registers 5

    const/4 v2, 0x3

    new-array v1, v2, [Ljava/lang/String;

    move-object v0, v1

    const/4 v2, 0x0

    const-string v3, "a"

    aput-object v3, v0, v2

    const/4 v2, 0x1

    const-string v3, "b"

    aput-object v3, v0, v2

    const/4 v2, 0x2

    const-string v3, "c"

    aput-object v3, v0, v2

    move-object v1, v0

    invoke-static {v1}, LTestInlineVarArg;->f([Ljava/lang/String;)V

    return-void
.end method
