.class public Ltrycatch/TestFinally3;
.super Ljava/lang/Object;

.field public bytes:[B

.method public test()[B
    .registers 4
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    const/4 v0, 0x0

    :try_start_1
    iget-object v1, p0, Ltrycatch/TestFinally3;->bytes:[B

    if-nez v1, :cond_1a

    invoke-direct {p0}, Ltrycatch/TestFinally3;->validate()Z
    :try_end_8
    .catchall {:try_start_1 .. :try_end_8} :catchall_24

    move-result v1

    if-nez v1, :cond_10

    invoke-static {v0}, Ltrycatch/TestFinally3;->close(Ljava/io/InputStream;)V
    return-object v0

    :cond_10
    :try_start_10
    invoke-direct {p0}, Ltrycatch/TestFinally3;->getInputStream()Ljava/io/InputStream;
    move-result-object v0

    invoke-direct {p0, v0}, Ltrycatch/TestFinally3;->read(Ljava/io/InputStream;)[B
    move-result-object v1

    iput-object v1, p0, Ltrycatch/TestFinally3;->bytes:[B

    :cond_1a
    iget-object v1, p0, Ltrycatch/TestFinally3;->bytes:[B

    invoke-direct {p0, v1}, Ltrycatch/TestFinally3;->convert([B)[B
    :try_end_1f
    .catchall {:try_start_10 .. :try_end_1f} :catchall_24

    move-result-object v1

    invoke-static {v0}, Ltrycatch/TestFinally3;->close(Ljava/io/InputStream;)V
    return-object v1

    :catchall_24
    move-exception v1
    invoke-static {v0}, Ltrycatch/TestFinally3;->close(Ljava/io/InputStream;)V
    throw v1
.end method

.method private convert([B)[B
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    const/4 v0, 0x0
    new-array v0, v0, [B
    return-object v0
.end method

.method private static close(Ljava/io/InputStream;)V
    .registers 1
    return-void
.end method

.method private getInputStream()Ljava/io/InputStream;
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    new-instance v0, Ljava/io/ByteArrayInputStream;
    const/4 v1, 0x0
    new-array v1, v1, [B
    invoke-direct {v0, v1}, Ljava/io/ByteArrayInputStream;-><init>([B)V
    return-object v0
.end method

.method private read(Ljava/io/InputStream;)[B
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    const/4 v0, 0x0
    new-array v0, v0, [B
    return-object v0
.end method

.method private validate()Z
    .registers 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    const/4 v0, 0x0
    return v0
.end method


