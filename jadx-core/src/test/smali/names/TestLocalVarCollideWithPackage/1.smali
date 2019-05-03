.class public Lfirst/A;
.super Ljava/lang/Object;

.method public test()Ljava/lang/String;
    .registers 2

    new-instance v1, Lpkg/Second;

    invoke-direct {v1}, Lpkg/Second;-><init>()V

    .local v1, "second":Lpkg/Second;

    invoke-static {}, Lsecond/A;->call()Ljava/lang/String;

    iget-object v0, v1, Lpkg/Second;->str:Ljava/lang/String;

    return-object v0
.end method
