.class public final Lconditions/TestConditions21;
.super Ljava/lang/Object;

.method public check(Ljava/lang/Object;)Z
    .locals 2

    if-eq p0, p1, :ret_true

    instance-of v0, p1, Ljava/util/List;
    if-eqz v0, :ret_false

    check-cast p1, Ljava/util/List;

    invoke-interface {p1}, Ljava/util/List;->isEmpty()Z
    move-result v0

    if-nez v0, :ret_false

    invoke-interface {p1, p0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z
    move-result v0

    if-eqz v0, :ret_false

    goto :ret_true

    :ret_false
    const/4 p1, 0x0
    return p1

    :ret_true
    const/4 p1, 0x1
    return p1
.end method
