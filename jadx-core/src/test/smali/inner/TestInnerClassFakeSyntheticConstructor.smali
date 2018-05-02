.class public Ljadx/tests/inner/TestCls;
.super Ljava/lang/Object;

# direct methods
.method public synthetic constructor <init>(Ljava/lang/String;)V
    .registers 3
    .param p1, "a"    # Ljava/lang/String;

    .prologue
    const/4 v0, 0x1

    invoke-direct {p0, p1, v0}, Ljadx/tests/inner/TestCls;-><init>(Ljava/lang/String;Z)V

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;Z)V
    .registers 3
    .param p1, "a"    # Ljava/lang/String;
    .param p2, "b"    # Z

    .prologue
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static build(Ljava/lang/String;)Ljadx/tests/inner/TestCls;
    .registers 2
    .param p0, "str"    # Ljava/lang/String;

    .prologue
    new-instance v0, Ljadx/tests/inner/TestCls;

    invoke-direct {v0, p0}, Ljadx/tests/inner/TestCls;-><init>(Ljava/lang/String;)V

    return-object v0
.end method
