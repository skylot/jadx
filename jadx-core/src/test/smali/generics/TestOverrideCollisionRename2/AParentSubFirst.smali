.class public Lgenerics/AParentSubFirst;
.super Lgenerics/ParentCollision;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Lgenerics/ParentCollision;-><init>()V
    return-void
.end method

.method public call()I
    .registers 2
    const/4 v0, 0x2
    return v0
.end method
