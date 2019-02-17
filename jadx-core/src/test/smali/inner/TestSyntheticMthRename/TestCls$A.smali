.class public final Linner/TestCls$A;
.super Ljava/lang/Object;
.source "TestCls.java"

# interfaces
.implements Linner/TestCls$I;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Linner/TestCls;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x19
    name = "A"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Linner/TestCls$I",
        "<",
        "Ljava/lang/String;",
        "Ljava/lang/Runnable;",
        ">;"
    }
.end annotation


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 9
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private varargs renamedCall([Ljava/lang/Runnable;)Ljava/lang/String;
    .registers 3
    .param p1, "p"    # [Ljava/lang/Runnable;

    .prologue
    .line 12
    const-string v0, "str"

    return-object v0
.end method

# virtual methods
.method public synthetic call([Ljava/lang/Object;)Ljava/lang/Object;
    .registers 3

    .prologue
    .line 9
    check-cast p1, [Ljava/lang/Runnable;

    invoke-virtual {p0, p1}, Linner/TestCls$A;->renamedCall([Ljava/lang/Runnable;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method
