.class public Lhusk/Caller;
.super Ljava/lang/Object;

.method public static check(Ljava/lang/StringBuilder;)Z
    .registers 2
    invoke-static {p0}, Lhusk/Trampoline;->a(Ljava/lang/StringBuilder;)I
    move-result v0
    const/4 v1, 0x5
    if-lt v0, v1, :cond_0
    const/4 v0, 0x1
    return v0
    :cond_0
    const/4 v0, 0x0
    return v0
.end method
