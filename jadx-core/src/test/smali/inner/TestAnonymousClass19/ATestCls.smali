.class public Linner/ATestCls;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1

    .prologue
    .line 11
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public test(ZZ)V
    .registers 5
    .param p1, "a"    # Z
    .param p2, "b"    # Z

    .prologue
    .line 14
    if-eqz p1, :cond_e

    if-eqz p2, :cond_e

    const/4 v0, 0x1

    .line 15
    .local v0, "c":Z
    :goto_5
    new-instance v1, Linner/Lambda$TestCls$1;

    invoke-direct {v1, p0, p1, p2, v0}, Linner/Lambda$TestCls$1;-><init>(Linner/ATestCls;ZZZ)V

    invoke-virtual {p0, v1}, Linner/ATestCls;->use(Ljava/lang/Runnable;)V

    .line 21
    return-void

    .line 14
    .end local v0    # "c":Z
    :cond_e
    const/4 v0, 0x0

    goto :goto_5
.end method

.method public use(Ljava/lang/Runnable;)V
    .registers 2
    return-void
.end method
