.class public Lsynchronize/TestSynchronized6;
.super Ljava/lang/Object;

.field private final lock:Ljava/lang/Object;

.method public constructor <init>()V
    .registers 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    new-instance v0, Ljava/lang/Object;
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V
    iput-object v0, p0, Lsynchronize/TestSynchronized6;->lock:Ljava/lang/Object;
    return-void
.end method

.method private test(Ljava/lang/Object;)Z
    .locals 2

    .line 169
    iget-object v0, p0, Lsynchronize/TestSynchronized6;->lock:Ljava/lang/Object;
    monitor-enter v0

    .line 170
    :try_start_0
    invoke-direct {p0, p1}, Lsynchronize/TestSynchronized6;->isA(Ljava/lang/Object;)Z
    move-result v1

    if-nez v1, :cond_1

    invoke-direct {p0, p1}, Lsynchronize/TestSynchronized6;->isB(Ljava/lang/Object;)Z
    move-result p1

    if-eqz p1, :cond_0
    goto :goto_0

    :cond_0
    const/4 p1, 0x0
    goto :goto_1

    :cond_1
    :goto_0
    const/4 p1, 0x1

    :goto_1
    monitor-exit v0

    return p1

    :catchall_0
    move-exception p1

    .line 171
    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p1
.end method

.method private isA(Ljava/lang/Object;)Z
    .registers 3
    const/4 v0, 0x0
    return v0
.end method

.method private isB(Ljava/lang/Object;)Z
    .registers 3
    const/4 v0, 0x0
    return v0
.end method
