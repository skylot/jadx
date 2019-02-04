.class public Ltrycatch/TestTryCatchNoMoveExc2;
.super Ljava/lang/Object;

.method private static test(Ljava/lang/AutoCloseable;)V
    .locals 0

    :try_start_0
    invoke-interface {p0}, Ljava/lang/AutoCloseable;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    invoke-static {}, Ljava/lang/System;->nanoTime()J

    return-void
.end method
