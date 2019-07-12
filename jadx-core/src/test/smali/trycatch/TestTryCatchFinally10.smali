.class public Ltrycatch/TestTryCatchFinally10;
.super Ljava/lang/Object;


# static fields
.field private static final l:Llog/DebugLogger;

.method public static test(Landroid/content/Context;I)Ljava/lang/String;
    .locals 2

    .line 46
    invoke-static {p0}, LCommonContracts;->requireNonNull(Ljava/lang/Object;)V

    const/4 v0, 0x0

    .line 50
    :try_start_0
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p0

    invoke-virtual {p0, p1}, Landroid/content/res/Resources;->openRawResource(I)Ljava/io/InputStream;

    move-result-object v0

    .line 51
    new-instance p0, Ljava/util/Scanner;

    invoke-direct {p0, v0}, Ljava/util/Scanner;-><init>(Ljava/io/InputStream;)V

    const-string p1, "\\A"

    invoke-virtual {p0, p1}, Ljava/util/Scanner;->useDelimiter(Ljava/lang/String;)Ljava/util/Scanner;

    move-result-object p0

    .line 52
    invoke-virtual {p0}, Ljava/util/Scanner;->hasNext()Z

    move-result p1

    if-eqz p1, :cond_0

    invoke-virtual {p0}, Ljava/util/Scanner;->next()Ljava/lang/String;

    move-result-object p0

    goto :goto_0

    :cond_0
    const-string p0, ""
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :goto_0
    if-eqz v0, :cond_1

    .line 56
    :try_start_1
    invoke-virtual {v0}, Ljava/io/InputStream;->close()V
    :try_end_1
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_1

    :catch_0
    move-exception p1

    .line 58
    sget-object v0, Ltrycatch/TestTryCatchFinally10;->l:Llog/DebugLogger;

    sget-object v1, Llog/DebugLogger$LogLevel;->ERROR:Llog/DebugLogger$LogLevel;

    invoke-virtual {v0, v1, p1}, Llog/DebugLogger;->logException(Llog/DebugLogger$LogLevel;Ljava/lang/Exception;)V

    :cond_1
    :goto_1
    return-object p0

    :catchall_0
    move-exception p0

    if-eqz v0, :cond_2

    .line 56
    :try_start_2
    invoke-virtual {v0}, Ljava/io/InputStream;->close()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_1

    goto :goto_2

    :catch_1
    move-exception p1

    .line 58
    sget-object v0, Ltrycatch/TestTryCatchFinally10;->l:Llog/DebugLogger;

    sget-object v1, Llog/DebugLogger$LogLevel;->ERROR:Llog/DebugLogger$LogLevel;

    invoke-virtual {v0, v1, p1}, Llog/DebugLogger;->logException(Llog/DebugLogger$LogLevel;Ljava/lang/Exception;)V

    .line 61
    :cond_2
    :goto_2
    throw p0
.end method
