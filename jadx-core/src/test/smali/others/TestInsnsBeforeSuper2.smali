.class public Lothers/TestInsnsBeforeSuper2;
.super Ljava/lang/Exception;
.source "MyException.java"

# instance fields
.field private mErrorType:I


# direct methods
.method public constructor <init>(Ljava/lang/String;I)V
    .locals 8

    .prologue
    move-object v0, p0

    .local v0, "this":Lothers/TestInsnsBeforeSuper2;
    move-object v1, p1

    .local v1, "message":Ljava/lang/String;
    move v2, p2

    .line 39
    .local v2, "errorType":I
    move-object v3, v0

    .local v3, "this":Lothers/TestInsnsBeforeSuper2;
    move-object v4, v1

    .local v4, "message":Ljava/lang/String;
    move v5, v2

    .line 51
    .end local v0    # "this":Lothers/TestInsnsBeforeSuper2;
    .end local v1    # "message":Ljava/lang/String;
    .end local v2    # "errorType":I
    .local v5, "errorType":I
    move-object v6, v1

    invoke-direct {v0, v6}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V

    .line 39
    const/4 v7, 0x0

    iput v7, v0, Lothers/TestInsnsBeforeSuper2;->mErrorType:I

    .line 52
    iput v2, v0, Lothers/TestInsnsBeforeSuper2;->mErrorType:I

    .line 53
    return-void
.end method
