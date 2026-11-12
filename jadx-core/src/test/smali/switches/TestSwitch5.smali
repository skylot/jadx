.class public Lswitches/TestSwitch5;
.super Ljava/lang/Object;

.method public static test(I)V
    .registers 2
    const/4 v0, -0x1
    if-eq p0, v0, :join
    if-eqz p0, :join
    const/4 v0, 0x1
    if-eq p0, v0, :join
    packed-switch p0, :pd1
    packed-switch p0, :pd2
    return-void
    :case1
    goto :join
    :case2
    nop
    :join
    sget-object v0, Ljava/lang/System;->out:Ljava/lang/PrintStream;
    invoke-virtual {v0, p0}, Ljava/io/PrintStream;->println(I)V
    return-void
    :pd1
    .packed-switch 0x1e
        :case1
        :case1
    .end packed-switch
    :pd2
    .packed-switch 0x28
        :case2
        :case2
    .end packed-switch
.end method
