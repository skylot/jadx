.class public Lothers/A;
.super Ljava/lang/Object;

.method public static check(Ljava/lang/String;)V
    .registers 3

    if-nez p0, :cond_a

    new-instance v0, Ljava/io/IOException;

    const-string v1, "missing"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_a
    return-void
.end method

.method public static run(Ljava/lang/String;)V
    .registers 1

    invoke-static {p0}, Lothers/B;->use(Ljava/lang/String;)V

    return-void
.end method
