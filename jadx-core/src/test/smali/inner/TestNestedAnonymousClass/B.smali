.class synthetic Linner/B;
.super Ljava/lang/Object;
.implements Ljava/util/concurrent/Callable;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Ljava/util/concurrent/Callable",
        "<",
        "Ljava/lang/Runnable;",
        ">;"
    }
.end annotation

.field final synthetic this$0:Linner/A;

.method constructor <init>(Linner/A;)V
    .registers 2
    .prologue
    iput-object p1, p0, Linner/B;->this$0:Linner/A;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method


.method public bridge synthetic call()Ljava/lang/Object;
    .registers 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    invoke-virtual {p0}, Linner/B;->call()Ljava/lang/Runnable;
    move-result-object v0
    return-object v0
.end method

.method public call()Ljava/lang/Runnable;
    .registers 2
    .prologue
    new-instance v0, Linner/C;
    invoke-direct {v0, p0}, Linner/C;-><init>(Linner/B;)V
    return-object v0
.end method
