.class public Ltypes/TestTypeResolver21;
.super Ljava/lang/Object;
.source "TestTypeResolver21.java"


.method public test(Ljava/lang/Object;)Ljava/lang/Number;
    .registers 4
    .param p1, "objectArray"    # Ljava/lang/Object;

    .prologue
    .line 16
    check-cast p1, [Ljava/lang/Object;
    .end local p1    # "objectArray":Ljava/lang/Object;
    move-object v0, p1
    check-cast v0, [Ljava/lang/Object;

    .line 17
    .local v0, "arr":[Ljava/lang/Object;
    const/4 v1, 0x0
    aget-object v1, v0, v1
    check-cast v1, Ljava/lang/Number;
    return-object v1
.end method
