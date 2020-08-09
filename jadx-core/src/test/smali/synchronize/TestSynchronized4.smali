.class public Lsynchronize/TestSynchronized4;
.super Ljava/lang/Object;

.field private final obj:Ljava/lang/Object;

.method public constructor <init>()V
    .registers 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Ljava/lang/Object;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    iput-object v0, p0, Lsynchronize/TestSynchronized4;->obj:Ljava/lang/Object;

    return-void
.end method

.method public test(I)Z
    .registers 4

    iget-object v1, p0, Lsynchronize/TestSynchronized4;->obj:Ljava/lang/Object;

    monitor-enter v1

    :try_start_3
    invoke-direct {p0, p1}, Lsynchronize/TestSynchronized4;->isZero(I)Z
    move-result v0

    if-eqz v0, :cond_11

    iget-object v0, p0, Lsynchronize/TestSynchronized4;->obj:Ljava/lang/Object;

    invoke-direct {p0, v0, p1}, Lsynchronize/TestSynchronized4;->call(Ljava/lang/Object;I)Z
    move-result v0

    monitor-exit v1

    :goto_10
    return v0

    :cond_11
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    invoke-virtual {v0}, Ljava/io/PrintStream;->println()V

    invoke-direct {p0}, Lsynchronize/TestSynchronized4;->getField()Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_22

    const/4 v0, 0x1

    :goto_1d
    monitor-exit v1
    return v0

    :catchall_1f
    move-exception v0

    monitor-exit v1
    :try_end_21
    .catchall {:try_start_3 .. :try_end_21} :catchall_1f

    throw v0

    :cond_22
    const/4 v0, 0x0

    goto :goto_1d
.end method

.method private call(Ljava/lang/Object;I)Z
    .registers 4

    const/4 v0, 0x0

    return v0
.end method

.method private getField()Ljava/lang/Object;
    .registers 2

    const/4 v0, 0x0

    return-object v0
.end method

.method private isZero(I)Z
    .registers 3

    if-nez p1, :cond_4

    const/4 v0, 0x1

    :goto_3
    return v0

    :cond_4
    const/4 v0, 0x0

    goto :goto_3
.end method
