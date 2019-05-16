.class public Linner/TestCls;
.super Ljava/lang/Object;

# direct methods
.method public constructor <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public test()V
    .registers 2

    new-instance v0, Linner/TestCls$1;

    invoke-direct {v0}, Linner/TestCls$1;-><init>()V

    return-void
.end method
