.class public Lvariables/TestVariablesGeneric;
.super Ljava/lang/Object;
.source "SourceFile"

.method public static a(Lrx/i;Lrx/c;)Lrx/j;
    .locals 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Lrx/i<",
            "-TT;>;",
            "Lrx/c<",
            "TT;>;)",
            "Lrx/j;"
        }
    .end annotation

    if-nez p0, :cond_0

    .line 10325
    new-instance p0, Ljava/lang/IllegalArgumentException;

    const-string p1, "subscriber can not be null"

    invoke-direct {p0, p1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p0

    .line 10327
    :cond_0
    iget-object v0, p1, Lrx/c;->a:Lrx/c$a;

    if-nez v0, :cond_1

    .line 10328
    new-instance p0, Ljava/lang/IllegalStateException;

    const-string p1, "onSubscribe function can not be null."

    invoke-direct {p0, p1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    .line 10336
    :cond_1
    invoke-virtual {p0}, Lrx/i;->onStart()V

    .line 10343
    instance-of v0, p0, Lrx/c/c;

    if-nez v0, :cond_2

    .line 10345
    new-instance v0, Lrx/c/c;

    invoke-direct {v0, p0}, Lrx/c/c;-><init>(Lrx/i;)V

    move-object p0, v0

    .line 10352
    :cond_2
    :try_start_0
    iget-object v0, p1, Lrx/c;->a:Lrx/c$a;

    invoke-static {p1, v0}, Lrx/d/c;->a(Lrx/c;Lrx/c$a;)Lrx/c$a;

    move-result-object p1

    invoke-interface {p1, p0}, Lrx/c$a;->call(Ljava/lang/Object;)V

    .line 10353
    invoke-static {p0}, Lrx/d/c;->a(Lrx/j;)Lrx/j;

    move-result-object p1
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-object p1

    :catch_0
    move-exception p1

    .line 10356
    invoke-static {p1}, Lrx/exceptions/a;->b(Ljava/lang/Throwable;)V

    .line 10358
    invoke-virtual {p0}, Lrx/i;->isUnsubscribed()Z

    move-result v0

    if-eqz v0, :cond_3

    .line 10359
    invoke-static {p1}, Lrx/d/c;->b(Ljava/lang/Throwable;)Ljava/lang/Throwable;

    move-result-object p0

    invoke-static {p0}, Lrx/d/c;->a(Ljava/lang/Throwable;)V

    goto :goto_0

    .line 10363
    :cond_3
    :try_start_1
    invoke-static {p1}, Lrx/d/c;->b(Ljava/lang/Throwable;)Ljava/lang/Throwable;

    move-result-object v0

    invoke-virtual {p0, v0}, Lrx/i;->onError(Ljava/lang/Throwable;)V
    :try_end_1
    .catch Ljava/lang/Throwable; {:try_start_1 .. :try_end_1} :catch_1

    .line 10375
    :goto_0
    invoke-static {}, Lrx/f/e;->b()Lrx/j;

    move-result-object p0

    return-object p0

    :catch_1
    move-exception p0

    .line 10365
    invoke-static {p0}, Lrx/exceptions/a;->b(Ljava/lang/Throwable;)V

    .line 10368
    new-instance v0, Lrx/exceptions/OnErrorFailedException;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "Error occurred attempting to subscribe ["

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, "] and then again while trying to pass to onError."

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {v0, p1, p0}, Lrx/exceptions/OnErrorFailedException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V

    .line 10370
    invoke-static {v0}, Lrx/d/c;->b(Ljava/lang/Throwable;)Ljava/lang/Throwable;

    .line 10372
    throw v0
.end method
