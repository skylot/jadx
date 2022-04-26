.class public Ltrycatch/TestLoopInTryCatch;
.super Ljava/lang/Object;
.source "SourceFile"

.method public static test()V
    .registers 6

    :try_start

    :loop

    invoke-static {}, Ltrycatch/TestLoopInTryCatch;->getI()I
    move-result v1

    const/4 v2, 0x1

    if-eq v1, v2, :cond

    const/4 v3, 0x2

    if-eq v1, v3, :cond

    goto :loop

    :cond
    if-eq v1, v2, :end
	invoke-static {}, Ltrycatch/TestLoopInTryCatch;->getI()I
    return-void

    :try_end
    .catch Ljava/lang/RuntimeException; {:try_start .. :try_end} :end

    :end
    return-void
.end method

.method public static getI()I
    .locals 2

    const/4 v1, 0x1
    return v1
.end method
