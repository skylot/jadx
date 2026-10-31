.class public Lsample/Outer;
.super Ljava/lang/Object;

.method public static create()Lsample/Outer$Target;
    .registers 3
    new-instance v0, Lsample/Outer$Target;
    const/4 v1, 0x7
    const/4 v2, 0x0
    invoke-direct {v0, v1, v2}, Lsample/Outer$Target;-><init>(ILsample/Outer$Marker;)V
    return-object v0
.end method
