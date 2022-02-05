.class public final Ldeobf/TestKotlinMetadata;
.super Ljava/lang/Object;
.source "TestMetaData.kt"


# annotations
.annotation runtime Lkotlin/Metadata;
    bv = {
        0x1,
        0x0,
        0x3
    }
    d1 = {
        "\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0002\n\u0002\u0010\u0008\n\u0002\u0008\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0015\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0004H\u0007\u00a2\u0006\u0002\u0008\u0007R\u0010\u0010\u0003\u001a\u00020\u00048\u0006X\u0087D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0008"
    }
    d2 = {
        "Ljadx/TestMetaData;",
        "",
        "()V",
        "id",
        "",
        "double",
        "x",
        "makeTwo",
        "test"
    }
    k = 0x1
    mv = {
        0x1,
        0x4,
        0x0
    }
.end annotation


# instance fields
.field public final id:I
    .annotation build Lkotlin/jvm/JvmField;
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .registers 2

    .prologue
    .line 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 7
    const/4 v0, 0x1

    iput v0, p0, Ldeobf/TestKotlinMetadata;->id:I

    return-void
.end method


# virtual methods
.method public final makeTwo(I)I
    .registers 3
    .param p1, "x"    # I
    .annotation build Lkotlin/jvm/JvmName;
        name = "makeTwo"
    .end annotation

    .prologue
    .line 11
    mul-int/lit8 v0, p1, 0x2

    return v0
.end method
