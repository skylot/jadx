.class public Linner/A;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    .prologue
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public test()V
    .registers 2

    .prologue
    new-instance v0, Linner/B;
    invoke-direct {v0, p0}, Linner/B;-><init>(Linner/A;)V
    invoke-virtual {p0, v0}, Linner/A;->use(Ljava/util/concurrent/Callable;)V
    return-void
.end method

.method public use(Ljava/util/concurrent/Callable;)V
    .registers 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/concurrent/Callable",
            "<",
            "Ljava/lang/Runnable;",
            ">;)V"
        }
    .end annotation
    .prologue
    return-void
.end method
