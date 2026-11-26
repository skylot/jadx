.class public Lgenerics/AMutableBoolean;
.super Ljava/lang/Object;

.implements Lgenerics/ZMutable;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Lgenerics/ZMutable<",
        "Ljava/lang/Boolean;",
        ">;"
    }
.end annotation

.field private value:Z

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public getValue()Ljava/lang/Boolean;
    .registers 2
    iget-boolean v0, p0, Lgenerics/AMutableBoolean;->value:Z
    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
    move-result-object v0
    return-object v0
.end method

.method public bridge synthetic getValue()Ljava/lang/Object;
    .registers 2
    invoke-virtual {p0}, Lgenerics/AMutableBoolean;->getValue()Ljava/lang/Boolean;
    move-result-object v0
    return-object v0
.end method

.method public setValue(Ljava/lang/Boolean;)V
    .registers 3
    invoke-virtual {p1}, Ljava/lang/Boolean;->booleanValue()Z
    move-result v0
    iput-boolean v0, p0, Lgenerics/AMutableBoolean;->value:Z
    return-void
.end method

.method public bridge synthetic setValue(Ljava/lang/Object;)V
    .registers 2
    check-cast p1, Ljava/lang/Boolean;
    invoke-virtual {p0, p1}, Lgenerics/AMutableBoolean;->setValue(Ljava/lang/Boolean;)V
    return-void
.end method
