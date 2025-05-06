.class public Lothers/TestInvalidExceptions2;
.super Ljava/lang/Object;

# direct methods
.method public constructor <init>()V
    .registers 1

    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private throwPossibleExceptionType()V
    .registers 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljadx/UnknownTypeHierarchyException;
        }
    .end annotation

    .line 36
    new-instance v0, Ljadx/UnknownTypeHierarchyException;

    const-string v1, ""

    invoke-direct {v0, v1}, Ljadx/UnknownTypeHierarchyException;-><init>(Ljava/lang/String;)V

    throw v0
.end method
