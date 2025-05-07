.class public Lothers/TestInvalidExceptions2;
.super Ljava/lang/Object;

.method private throwPossibleExceptionType()V
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljadx/UnknownTypeHierarchyException;
        }
    .end annotation

    new-instance v0, Ljadx/UnknownTypeHierarchyException;
    const-string v1, ""
    invoke-direct {v0, v1}, Ljadx/UnknownTypeHierarchyException;-><init>(Ljava/lang/String;)V
    throw v0
.end method
