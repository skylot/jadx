.class public final Lvariables/TestThisBranchDup;
.super Ljava/lang/Object;

.method public constructor <init>(ZZZLh3/t;ZZILkotlin/jvm/internal/DefaultConstructorMarker;)V
    .registers 10

    and-int/lit8 p8, p7, 0x1
    if-eqz p8, :cond_5
    const/4 p1, 0x0

    :cond_5
    and-int/lit8 p8, p7, 0x2
    const/4 v0, 0x1
    if-eqz p8, :cond_b
    move p2, v0

    :cond_b
    and-int/lit8 p8, p7, 0x4
    if-eqz p8, :cond_10
    move p3, v0

    :cond_10
    and-int/lit8 p8, p7, 0x8
    if-eqz p8, :cond_16
    .line 11
    sget-object p4, Lh3/t;->Inherit:Lh3/t;

    :cond_16
    and-int/lit8 p8, p7, 0x10
    if-eqz p8, :cond_1b
    move p5, v0

    :cond_1b
    and-int/lit8 p7, p7, 0x20
    if-eqz p7, :cond_27
    move p8, v0
    move-object p6, p4
    move p7, p5
    move p4, p2
    move p5, p3
    move-object p2, p0
    move p3, p1
    goto :goto_2e

    :cond_27
    move p8, p6
    move p7, p5
    move p5, p3
    move-object p6, p4
    move p3, p1
    move p4, p2
    move-object p2, p0

    .line 12
    :goto_2e
    invoke-direct/range {p2 .. p8}, Lvariables/TestThisBranchDup;-><init>(ZZZLh3/t;ZZ)V
    return-void
.end method
