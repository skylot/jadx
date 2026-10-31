.class public Ltrycatch/TestTryCatch12;
.super Ljava/lang/Object;
.source "TestTryCatch12.java"

.method public static test(Ltrycatch/TestTryCatch12;)I
    .registers 5

    const-string v0, "TAG"

    const/4 v1, -0x1

    :try_start_3
    const-string v2, "key"

    invoke-virtual {p0, v2}, Ltrycatch/TestTryCatch12;->get(Ljava/lang/String;)Ltrycatch/TestTryCatch12;

    move-result-object p0

    if-eqz p0, :cond_25

    const-string v2, "level"

    invoke-virtual {p0, v2, v1}, Ltrycatch/TestTryCatch12;->getInt(Ljava/lang/String;I)I

    move-result p0

    return p0

    :cond_25
    const-string p0, "Not found"

    invoke-static {v0, p0}, Ltrycatch/TestTryCatch12;->log(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2a
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_2a} :catch_2b

    goto :goto_31

    :catch_2b
    move-exception p0

    const-string v2, "Error"

    invoke-static {v0, v2, p0}, Ltrycatch/TestTryCatch12;->logError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V

    :goto_31
    return v1
.end method

.method public get(Ljava/lang/String;)Ltrycatch/TestTryCatch12;
    .registers 2
    return-object p0
.end method

.method public getInt(Ljava/lang/String;I)I
    .registers 3
    return p2
.end method

.method private static log(Ljava/lang/String;Ljava/lang/String;)V
    .registers 2
    return-void
.end method

.method private static logError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .registers 3
    return-void
.end method
