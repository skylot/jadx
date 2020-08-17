.class public Lothers/TestIncorrectMethodSignature;
.super Ljava/lang/RuntimeException;
.source "TestIncorrectMethodSignature.java"

.method public constructor <init>(Ljava/lang/String;)V
    .registers 2

    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(J)V"
        }
    .end annotation

    invoke-direct {p0, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    return-void
.end method
