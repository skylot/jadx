.class public Linline/TestOverrideBridgeMerge;
.super Ljava/lang/Object;

.implements Ljava/util/function/Function;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Ljava/util/function/Function",
        "<",
        "Ljava/lang/String;",
        "Ljava/lang/Integer;",
        ">;"
    }
.end annotation

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public bridge synthetic apply(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 3
    check-cast p1, Ljava/lang/String;
    invoke-virtual {p0, p1}, Linline/TestOverrideBridgeMerge;->test(Ljava/lang/String;)Ljava/lang/Integer;
    move-result-object v0
    return-object v0
.end method

.method public test(Ljava/lang/String;)Ljava/lang/Integer;
    .registers 3
    .param p1, "str"    # Ljava/lang/String;
    invoke-virtual {p1}, Ljava/lang/String;->length()I
    move-result v0
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v0
    return-object v0
.end method
