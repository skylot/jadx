.class public Linvoke/TestConstructorWithMoves;
.super Ljava/lang/Object;

.method static public test()Z
    .registers 11

    new-instance v5, Ljava/lang/Boolean;
    move-object v8, v5
    move-object v5, v8
    move-object v6, v8
    const-string v7, "test"
    invoke-direct {v6, v7}, Ljava/lang/Boolean;-><init>(Ljava/lang/String;)V
    check-cast v5, Ljava/lang/Boolean;
    invoke-virtual {v5}, Ljava/lang/Boolean;->booleanValue()Z
    move-result v5
    move v3, v5
    return v3
.end method

