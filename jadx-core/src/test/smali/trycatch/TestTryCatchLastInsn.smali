.class public Ltrycatch/TestTryCatchLastInsn;
.super Ljava/lang/Object;
.source "TestTryCatchLastInsn.java"

.method public test()Ljava/lang/Exception;
    .registers 6

    .prologue
    const-string v1, "result"

    :try_start
    invoke-direct {p0}, Ltrycatch/TestTryCatchLastInsn;->call()Ljava/lang/Exception;
    move-result-object v1
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch

    :goto_return
    return-object v1

    :catch
    move-exception v4
    sget-object v3, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v3, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    move-object v1, v4
    goto :goto_return
.end method


.method private call()Ljava/lang/Exception;
    .registers 2
    new-instance v0, Ljava/lang/Exception;
    invoke-direct {v0}, Ljava/lang/Exception;-><init>()V
    return-object v0
.end method
