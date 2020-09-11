.class public Larith/TestXor;
.super Ljava/lang/Object;


.method public test()Z
    .locals 1

    .line 20
    const/4 v0, 0x1

    return v0
.end method

.method public test1()Z
    .locals 1

    .line 12
    invoke-virtual {p0}, Larith/TestXor;->test()Z

    move-result v0

    xor-int/lit8 v0, v0, 0x1

    return v0
.end method

.method public test2()Z
    .locals 1

    .line 16
    invoke-virtual {p0}, Larith/TestXor;->test()Z

    move-result v0

    xor-int/lit8 v0, v0, 0x0

    return v0
.end method
