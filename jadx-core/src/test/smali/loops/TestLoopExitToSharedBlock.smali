.class public Lloops/TestLoopExitToSharedBlock;
.super Ljava/lang/Object;

.method public static log(Ljava/lang/String;Ljava/lang/String;)V
    .registers 2
    return-void
.end method

.method public static test(Ljava/lang/Object;[Ljava/lang/Object;Z)Z
    .registers 9

    const/4 p0, 0x1

    const-string v0, "TAG"

    if-nez p2, :cond_b

    const-string p1, "ignore"

    :goto_7
    invoke-static {v0, p1}, Lloops/TestLoopExitToSharedBlock;->log(Ljava/lang/String;Ljava/lang/String;)V

    return p0

    :cond_b
    array-length p2, p1

    const/4 v1, 0x0

    move v2, v1

    :goto_e
    if-ge v2, p2, :cond_3f

    aget-object v3, p1, v2

    if-nez v3, :cond_15

    goto :goto_3c

    :cond_15
    invoke-virtual {v3}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lloops/TestLoopExitToSharedBlock;->log(Ljava/lang/String;Ljava/lang/String;)V

    const-string v4, "abc"

    invoke-virtual {v3, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_3c

    const-string p1, "pass"

    goto :goto_7

    :cond_3c
    :goto_3c
    add-int/lit8 v2, v2, 0x1

    goto :goto_e

    :cond_3f
    return v1
.end method
