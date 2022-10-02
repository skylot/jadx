.class public Lconditions/TestBooleanToInt2;
.super Ljava/lang/Object;

.method public test()V
    .registers 3
    invoke-direct {p0}, Lconditions/TestBooleanToInt2;->getValue()Z
    move-result v0
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v1
    invoke-direct {p0, v1}, Lconditions/TestBooleanToInt2;->use1(Ljava/lang/Integer;)V
    invoke-direct {p0, v0}, Lconditions/TestBooleanToInt2;->use2(I)V
    return-void
.end method

.method private getValue()Z
    .registers 2
    const/4 v0, 0x0
    return v0
.end method

.method private use1(Ljava/lang/Integer;)V
    .registers 2
    return-void
.end method

.method private use2(I)V
    .registers 2
    return-void
.end method
