.class public Lfirst/A;
.super Ljava/lang/Object;

.field public first:Lfirst/A;
.field public second:Lsecond/A;

.method public test()Ljava/lang/String;
    .registers 2

    invoke-static {}, Lsecond/A;->call()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method
