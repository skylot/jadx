.class public Lsample/Outer$Target;
.super Ljava/lang/Object;

.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lsample/Outer;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "Target"
.end annotation

.field public final value:I

.method private constructor <init>(I)V
    .registers 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput p1, p0, Lsample/Outer$Target;->value:I
    return-void
.end method

.method synthetic constructor <init>(ILsample/Outer$Marker;)V
    .registers 3
    invoke-direct {p0, p1}, Lsample/Outer$Target;-><init>(I)V
    return-void
.end method
