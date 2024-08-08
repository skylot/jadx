.class public final synthetic Linline/A$$ExternalSyntheticLambda0;
.super Ljava/lang/Object;
.source "D8$$SyntheticClass"

.implements Ljava/util/function/Supplier;

.field public final synthetic f$0:J
.field public final synthetic f$1:J

.method public synthetic constructor <init>(JJ)V
    .registers 5
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-wide p1, p0, Linline/A$$ExternalSyntheticLambda0;->f$0:J
    iput-wide p3, p0, Linline/A$$ExternalSyntheticLambda0;->f$1:J
    return-void
.end method

.method public final get()Ljava/lang/Object;
    .registers 5
    iget-wide v0, p0, Linline/A$$ExternalSyntheticLambda0;->f$0:J
    iget-wide v2, p0, Linline/A$$ExternalSyntheticLambda0;->f$1:J
    invoke-static {v0, v1, v2, v3}, Linline/A;->lambda$test$0(JJ)Ljava/lang/Long;
    move-result-object v0
    return-object v0
.end method
