.class public Ltrycatch/TestTryCatchMultiException2;
.super Ljava/lang/Object;


.method public static test()Z
    .registers 5

    :try_start_b
    const-string v0, "c"
    invoke-static {v0}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    move-result-object v1

    const/4 v0, 0x0
    const-string v2, "b"
    new-array v3, v0, [Ljava/lang/Class;
    invoke-virtual {v1, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v2

    new-array v3, v0, [Ljava/lang/Object;
    invoke-virtual {v2, v1, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v1

    check-cast v1, Ljava/lang/Boolean;
    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v1
    :try_end_2f
    .catch Ljava/lang/ClassNotFoundException; {:try_start_b .. :try_end_2f} :catch_30
    .catch Ljava/lang/NoSuchMethodException; {:try_start_b .. :try_end_2f} :catch_30
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_2f} :catch_30
    .catchall {:try_start_b .. :try_end_2f} :catchall_30

    return v1

    :catch_30
    :catchall_30
    return v0
.end method
