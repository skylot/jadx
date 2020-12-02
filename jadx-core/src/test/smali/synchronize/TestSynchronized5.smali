.class public Lsynchronize/TestSynchronized5;
.super Ljava/lang/Object;

.method public final get()V
    .registers 6

    monitor-enter p0

    :try_start_1

    const/4 v0, 0

    if-eqz v0, :cond_1

    monitor-exit p0

    return-void

    :cond_1

    monitor-exit p0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    monitor-enter p0

    :try_start_2

    const/4 v1, 1

    if-eqz v1, :cond_2

    invoke-static {}, Ljava/lang/System;->gc()V

    :cond_2

    monitor-exit p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    return-void

    :catchall_2
    move-exception v0

    :try_start_3
    monitor-exit p0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_2

    throw v0

    :catchall_1
    move-exception v0

    :try_start_4
    monitor-exit p0
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    throw v0
.end method
