.class public Ltypes/TestCls;
.super Ljava/lang/Object;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ljadx/tests/integration/types/TestTypeResolver8;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "TestCls"
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Ltypes/TestCls$B;,
        Ltypes/TestCls$A;
    }
.end annotation


# instance fields
.field private f:Ltypes/TestCls$A;


# direct methods
.method public constructor <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private use(Ltypes/TestCls$B;)V
    .registers 2

    return-void
.end method


# virtual methods
.method public test()V
    .registers 3

    iget-object v1, p0, Ltypes/TestCls;->f:Ltypes/TestCls$A;

    if-eqz v1, :cond_a

    new-instance v0, Ltypes/TestCls$B;

    invoke-direct {v0, v1}, Ltypes/TestCls$B;-><init>(Ltypes/TestCls$A;)V

    move v1, v0

    :cond_a
    invoke-direct {p0, v1}, Ltypes/TestCls;->use(Ltypes/TestCls$B;)V

    return-void
.end method
