.class public LTestXor;
.super Ljava/lang/Object;


# direct methods
.method public constructor <init>()V
    .locals 0

    .line 9
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public test()Z
    .locals 1

    .line 20
    const/4 v0, 0x1

    return v0
.end method

.method public test1()Z
    .locals 1

    .line 12
    invoke-virtual {p0}, Lcom/example/myapplication/MainActivity;->test()Z

    move-result v0

    xor-int/lit8 v0, v0, 0x1

    return v0
.end method

.method public test2()Z
    .locals 1

    .line 16
    invoke-virtual {p0}, Lcom/example/myapplication/MainActivity;->test()Z

    move-result v0

    xor-int/lit8 v0, v0, 0x0

    return v0
.end method
