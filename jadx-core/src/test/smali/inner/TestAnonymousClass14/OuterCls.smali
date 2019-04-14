.class public Linner/OuterCls;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Linner/OuterCls$TestCls;
    }
.end annotation


# direct methods
.method static constructor <clinit>()V
    .locals 0

    return-void
.end method

.method public constructor <init>()V
    .locals 1

    return-void
.end method

.method public makeTestCls()V
    .locals 2

    new-instance v1, Linner/OuterCls$TestCls;

    const/4 v0, 0x0

    invoke-direct {v1, p0, v0}, Linner/OuterCls$TestCls;-><init>(Linner/OuterCls;Linner/OuterCls$1;)V

    return-void
.end method

.method public makeAnonymousCls()V
    .locals 2

    new-instance v1, Linner/OuterCls$1;

    invoke-direct {v1, p0, p0}, Linner/OuterCls$1;-><init>(Linner/OuterCls;Ljava/lang/Runnable;)V

    invoke-direct {p0, v1}, Linner/OuterCls;->use(Ljava/lang/Thread;)V

    return-void
.end method

.method public run()V
    .locals 2

    return-void
.end method

.method public use(Ljava/lang/Thread;)V
    .locals 2

    return-void
.end method

