.class public Lconditions/TestConditions22;
.super Ljava/lang/Object;

.method public static test(II)I
    .registers 6

    const/4 v0, 0x1
    const/4 v1, 0x2
    const/4 v2, 0x3
    const/4 v3, 0x0

    if-ne p0, v0, :cond_b
    if-ne p1, v1, :cond_9
    goto :goto_17

    :cond_9
    move v2, v3
    goto :goto_17

    :cond_b
    const/4 v0, 0x4
    if-ne p0, v1, :cond_12
    if-ne p1, v2, :cond_9
    move v2, v0
    goto :goto_17

    :cond_12
    if-ne p0, v2, :cond_9
    if-ne p1, v0, :cond_9
    const/4 v2, 0x5

    .line 32
    :goto_17
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance p1, Ljava/lang/StringBuilder;
    const-string v0, "k = "
    invoke-direct {p1, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {p1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    move-result-object p1
    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object p1
    invoke-virtual {p0, p1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return v2
.end method

.method private static verify(III)V
    .registers 3
    invoke-static {p0, p1}, Lconditions/TestConditions22;->test(II)I
    move-result p0
    invoke-static {p0}, Ljadx/tests/api/utils/assertj/JadxAssertions;->assertThat(I)Lorg/assertj/core/api/AbstractIntegerAssert;
    move-result-object p0
    invoke-virtual {p0, p2}, Lorg/assertj/core/api/AbstractIntegerAssert;->isEqualTo(I)Lorg/assertj/core/api/AbstractIntegerAssert;
    return-void
.end method

.method public check()V
    .registers 5
    const/4 v0, 0x1
    const/4 v1, 0x2
    const/4 v2, 0x3
    invoke-static {v0, v1, v2}, Lconditions/TestConditions22;->verify(III)V
    const/4 v3, 0x0
    invoke-static {v0, v0, v3}, Lconditions/TestConditions22;->verify(III)V
    const/4 v0, 0x4
    invoke-static {v1, v2, v0}, Lconditions/TestConditions22;->verify(III)V
    invoke-static {v1, v1, v3}, Lconditions/TestConditions22;->verify(III)V
    const/4 v1, 0x5
    invoke-static {v2, v0, v1}, Lconditions/TestConditions22;->verify(III)V
    invoke-static {v2, v2, v3}, Lconditions/TestConditions22;->verify(III)V
    invoke-static {v0, v0, v3}, Lconditions/TestConditions22;->verify(III)V
    return-void
.end method
