.class public Lconditions/TestInnerAssign3;
.super LTestSuper;
.source "Test.java"

.method public test()V
    .locals 5

    const/4 v0, 0
    const/4 v1, 0
    const/4 v4, 0

    if-eqz v4, :cond_0

    const/4 v2, 0

    invoke-virtual {v2}, LTestClass1;->testMethod()LTestClass2;

    move-result-object v0

    if-eqz v0, :cond_0

    if-eq v1, v0, :cond_0

    iget-object v3, v2, LTestClass1;->testField:LTestClass3;

    if-eqz v3, :cond_0

    :cond_0
    return-void
.end method
