.class public Lgenerics/ParentCollision;
.super Ljava/lang/Object;

.implements Lgenerics/BaseIface;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Lgenerics/BaseIface<",
        "Lgenerics/MBox<",
        "Ljava/lang/String;",
        ">;>;"
    }
.end annotation

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public call()Lgenerics/MBox;
    .registers 2
    const/4 v0, 0x0
    return-object v0
.end method

.method public call()I
    .registers 2
    const/4 v0, 0x1
    return v0
.end method
