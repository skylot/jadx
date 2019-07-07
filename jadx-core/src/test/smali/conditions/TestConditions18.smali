.class public final Lconditions/TestConditions18;
.super Ljava/lang/Object;

.field private map:Ljava/util/Map;

.method public test(Ljava/lang/Object;)Z
    .locals 1

    if-eq p0, p1, :cond_1

    instance-of v0, p1, Lconditions/TestConditions18;

    if-eqz v0, :cond_0

    check-cast p1, Lconditions/TestConditions18;

    iget-object v0, p0, Lconditions/TestConditions18;->map:Ljava/util/Map;

    iget-object p1, p1, Lconditions/TestConditions18;->map:Ljava/util/Map;

    invoke-static {v0, p1}, Lconditions/TestConditions18;->st(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_0

    goto :goto_0

    :cond_0
    const/4 p1, 0x0

    return p1

    :cond_1
    :goto_0
    const/4 p1, 0x1

    return p1
.end method

.method private static st(Ljava/lang/Object;Ljava/lang/Object;)Z
    .locals 1
    const/4 v0, 0x0
    return v0
.end method
