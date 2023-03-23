.class public Linline/Lambda$1;
.super Ljava/lang/Object;

.implements Ljava/util/function/Function;


.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Ljava/util/function/Function",
        "<TT;TT;>;"
    }
.end annotation

.field public static final INSTANCE:Linline/Lambda$1;

.method static constructor <clinit>()V
    .registers 1
    new-instance v0, Linline/Lambda$1;
    invoke-direct {v0}, Linline/Lambda$1;-><init>()V
    sput-object v0, Linline/Lambda$1;->INSTANCE:Linline/Lambda$1;
    return-void
.end method

.method private constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public final apply(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TT;)TT;"
        }
    .end annotation

    return-object p1
.end method
