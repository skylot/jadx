.class public Ltrycatch/TestTryCatch10;
.super Ljava/lang/Object;

.field public static VERSION:I

.method public static test(I)Z
    .registers 5

    sget v0, Ltrycatch/TestTryCatch10;->VERSION:I
    const/16 v1, 0x1d
    const/4 v2, 0x0
    if-lt v0, v1, :cond_1b
    const-string v0, "custom"
    invoke-static {v0}, Ltrycatch/TestTryCatch10;->check(Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :cond_10
    goto :goto_1b

    :cond_10
    :try_start_10
    invoke-static {p0}, Ltrycatch/TestTryCatch10;->getVar(I)I
    move-result p0
    :try_end_18
    .catch Ljava/lang/Exception; {:try_start_10 .. :try_end_18} :catch_1b

    if-eqz p0, :cond_1b
    const/4 v2, 0x1

    :catch_1b
    :cond_1b
    :goto_1b
    return v2
.end method

.method public static getVar(I)I
    .locals 0
    return p0
.end method

.method public static check(Ljava/lang/String;)Z
    .locals 1
    const/4 v0, 0x0
    return v0
.end method
