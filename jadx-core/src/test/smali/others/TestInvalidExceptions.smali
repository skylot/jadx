.class public Lothers/TestInvalidExceptions;
.super Ljava/lang/Object;

.method private invalidException()V
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/String;
        }
    .end annotation

    new-instance v0, Ljava/io/FileNotFoundException;
    const-string v1, ""
    invoke-direct {v0, v1}, Ljava/io/FileNotFoundException;-><init>(Ljava/lang/String;)V
    throw v0
.end method
