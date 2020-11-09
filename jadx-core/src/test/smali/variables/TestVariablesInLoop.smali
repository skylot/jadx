.class public abstract Lvariables/TestVariablesInLoop;
.super Ljava/lang/Object;
.source "SourceFile"

.implements Ljava/util/List;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Ljava/util/List<",
        "Ljava/lang/Long;",
        ">;"
    }
.end annotation

.method static test(Ljava/util/List;)I
    .locals 5
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Ljava/lang/Long;",
            ">;)I"
        }
    .end annotation

    invoke-interface {p0}, Ljava/util/List;->size()I

    move-result v0

    const/4 v1, 0x0

    if-nez v0, :cond_0

    return v1

    :cond_0
    instance-of v2, p0, Lvariables/TestVariablesInLoop;

    if-eqz v2, :cond_1

    check-cast p0, Lvariables/TestVariablesInLoop;

    const/4 v2, 0x0

    :goto_0
    if-ge v1, v0, :cond_2

    invoke-virtual {p0, v1}, Lvariables/TestVariablesInLoop;->getLong(I)J

    move-result-wide v3

    invoke-static {v3, v4}, Lvariables/TestVariablesInLoop;->mth(J)I

    move-result v3

    add-int/2addr v2, v3

    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    :cond_1
    const/4 v2, 0x0

    :goto_1
    if-ge v1, v0, :cond_2

    invoke-interface {p0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/Long;

    invoke-virtual {v3}, Ljava/lang/Long;->longValue()J

    move-result-wide v3

    invoke-static {v3, v4}, Lvariables/TestVariablesInLoop;->mth(J)I

    move-result v3

    add-int/2addr v2, v3

    add-int/lit8 v1, v1, 0x1

    goto :goto_1

    :cond_2
    return v2
.end method

.method public final getLong(I)J
    .locals 2
    const/16 v0, 0x0
    return-wide v0
.end method

.method static mth(J)I
    .locals 2
    const/4 v0, 0x0
    return v0
.end method
