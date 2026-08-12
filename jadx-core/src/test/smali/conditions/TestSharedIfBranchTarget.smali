.class public Lconditions/TestSharedIfBranchTarget;
.super Ljava/lang/Object;

.method public static test(II)I
    .registers 3

    const/4 v0, 0x0
    if-eqz p0, :outer_complete

    const/4 v1, 0x3
    if-ne p1, v1, :do_stop
    goto :complete

    :outer_complete
    add-int/lit8 v0, v0, 0x1
    goto :complete

    :do_stop
    add-int/lit8 v0, v0, 0x2
    goto :fail

    :complete
    add-int/lit8 v0, v0, 0x4
    goto :after

    :fail
    add-int/lit8 v0, v0, 0x8

    :after
    add-int/lit8 v0, v0, 0x10
    return v0
.end method

.method public check()V
    .registers 4

    const/4 v0, 0x0
    const/4 v1, 0x0
    invoke-static {v0, v1}, Lconditions/TestSharedIfBranchTarget;->test(II)I
    move-result v2
    const/16 v3, 0x15
    if-ne v2, v3, :fail

    const/4 v0, 0x1
    const/4 v1, 0x3
    invoke-static {v0, v1}, Lconditions/TestSharedIfBranchTarget;->test(II)I
    move-result v2
    const/16 v3, 0x14
    if-ne v2, v3, :fail

    const/4 v0, 0x1
    const/4 v1, 0x0
    invoke-static {v0, v1}, Lconditions/TestSharedIfBranchTarget;->test(II)I
    move-result v2
    const/16 v3, 0x1a
    if-ne v2, v3, :fail

    return-void

    :fail
    new-instance v0, Ljava/lang/AssertionError;
    invoke-direct {v0}, Ljava/lang/AssertionError;-><init>()V
    throw v0
.end method
