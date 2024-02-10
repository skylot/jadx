.class public abstract Ltypes/TestTypeResolver25;
.super Ljava/lang/Object;

.field public final a:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;"
        }
    .end annotation
.end field

.field public volatile b:Z

.method public k(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;
    .registers 6
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Ljava/lang/String;",
            "TT;)TT;"
        }
    .end annotation

    .line 1
    iget-object v0, p0, Ltypes/TestTypeResolver25;->a:Ljava/util/Map;

    monitor-enter v0

    .line 2
    :try_start_3
    iget-object v1, p0, Ltypes/TestTypeResolver25;->a:Ljava/util/Map;

    invoke-interface {v1, p1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    if-nez v1, :cond_10

    .line 3
    iget-object v2, p0, Ltypes/TestTypeResolver25;->a:Ljava/util/Map;

    invoke-interface {v2, p1, p2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 4
    :cond_10
    monitor-exit v0
    :try_end_11
    .catchall {:try_start_3 .. :try_end_11} :catchall_2c

    if-nez v1, :cond_14

    goto :goto_15

    :cond_14
    move-object p2, v1

    .line 5
    :goto_15
    iget-boolean p1, p0, Ltypes/TestTypeResolver25;->b:Z

    if-eqz p1, :cond_2b

    .line 6
    instance-of p1, p2, Ljava/io/Closeable;

    if-eqz p1, :cond_2b

    .line 7
    :try_start_1d
    move-object p1, p2

    check-cast p1, Ljava/io/Closeable;

    invoke-interface {p1}, Ljava/io/Closeable;->close()V
    :try_end_23
    .catch Ljava/io/IOException; {:try_start_1d .. :try_end_23} :catch_24

    goto :goto_2b

    :catch_24
    move-exception p1

    .line 8
    new-instance p2, Ljava/lang/RuntimeException;

    invoke-direct {p2, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V

    throw p2

    :cond_2b
    :goto_2b
    return-object p2

    :catchall_2c
    move-exception p1

    .line 9
    :try_start_2d
    monitor-exit v0
    :try_end_2e
    .catchall {:try_start_2d .. :try_end_2e} :catchall_2c

    throw p1
.end method
