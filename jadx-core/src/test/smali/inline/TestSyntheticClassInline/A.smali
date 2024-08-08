.class Linline/A;
.super Ljava/lang/Object;
.source "TestJavaClass.java"

.method constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method static synthetic lambda$test$0(JJ)Ljava/lang/Long;
    .registers 8
    .param p0, "x1"    # J
    .param p2, "x2"    # J

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v0
    .local v0, "y1":J
    add-long v2, p0, v0
    add-long/2addr v2, p2
    invoke-static {v2, v3}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v2
    return-object v2
.end method

.method static test(JJ)Ljava/util/function/Supplier;
    .registers 5
    .param p0, "x1"    # J
    .param p2, "x2"    # J
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(JJ)",
            "Ljava/util/function/Supplier<",
            "Ljava/lang/Long;",
            ">;"
        }
    .end annotation

    new-instance v0, Linline/A$$ExternalSyntheticLambda0;
    invoke-direct {v0, p0, p1, p2, p3}, Linline/A$$ExternalSyntheticLambda0;-><init>(JJ)V
    return-object v0
.end method
