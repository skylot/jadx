.class public Lothers/B;
.super Lothers/A;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .registers 3

    .prologue
    invoke-static {p1}, Lothers/B;->checkNull(Ljava/lang/Object;)V

    invoke-direct {p0, p1}, Lothers/A;-><init>(Ljava/lang/String;)V

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
