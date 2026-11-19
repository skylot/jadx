.class public Lgenerics/NBoxImpl;
.super Ljava/lang/Object;

.implements Lgenerics/ZMutable;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Lgenerics/ZMutable<",
        "Lgenerics/MBox<",
        "Ljava/lang/String;",
        ">;>;"
    }
.end annotation

.field private value:Lgenerics/MBox;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public getValue()Lgenerics/MBox;
    .registers 2
    iget-object v0, p0, Lgenerics/NBoxImpl;->value:Lgenerics/MBox;
    return-object v0
.end method

.method public bridge synthetic getValue()Ljava/lang/Object;
    .registers 2
    invoke-virtual {p0}, Lgenerics/NBoxImpl;->getValue()Lgenerics/MBox;
    move-result-object v0
    return-object v0
.end method

.method public setValue(Lgenerics/MBox;)V
    .registers 2
    iput-object p1, p0, Lgenerics/NBoxImpl;->value:Lgenerics/MBox;
    return-void
.end method

.method public bridge synthetic setValue(Ljava/lang/Object;)V
    .registers 2
    check-cast p1, Lgenerics/MBox;
    invoke-virtual {p0, p1}, Lgenerics/NBoxImpl;->setValue(Lgenerics/MBox;)V
    return-void
.end method
