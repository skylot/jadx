.class public Ltrycatch/TestTryCatchStartOnMove;
.super Ljava/lang/Object;


# direct methods
.method private static test(Ljava/lang/String;)V
    .registers 5

    :try_start
    move v3, p0
    invoke-static {v3}, Ltrycatch/TestTryCatchStartOnMove;->call(Ljava/lang/String;)V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch

    :goto_ret
    return-void

    :catch
    move-exception v0
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "Failed call for "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    goto :goto_ret
.end method


.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljadx/tests/api/SmaliTest;-><init>()V
    return-void
.end method


.method private static call(Ljava/lang/String;)V
    .registers 1
    return-void
.end method
