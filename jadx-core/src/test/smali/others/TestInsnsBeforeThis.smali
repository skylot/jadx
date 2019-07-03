.class public Lothers/TestInsnsBeforeThis;
.super Ljava/lang/Object;

.method public constructor <init>(Ljava/lang/String;)V
    .registers 3

    .prologue
    invoke-static {p1}, Lothers/TestInsnsBeforeThis;->checkNull(Ljava/lang/Object;)V

    invoke-direct {p1}, Ljava/lang/String;->length()I
    move-result v0

    invoke-direct {p0, v0}, Lothers/TestInsnsBeforeThis;-><init>(I)V

    return-void
.end method

.method public constructor <init>(I)V
    .registers 3

    .prologue
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static checkNull(Ljava/lang/Object;)V
    .registers 3

    .prologue
    if-nez p0, :cond_8

    new-instance v0, Ljava/lang/NullPointerException;
    invoke-direct {v0}, Ljava/lang/NullPointerException;-><init>()V
    throw v0

    :cond_8
    return-void
.end method
