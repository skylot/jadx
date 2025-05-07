.class public Lothers/TestMissingExceptions;
.super Ljava/lang/Object;

.method private exceptionSource()V
    .registers 3

    new-instance v0, Ljava/io/FileNotFoundException;
    const-string v1, ""
    invoke-direct {v0, v1}, Ljava/io/FileNotFoundException;-><init>(Ljava/lang/String;)V
    throw v0
.end method

.method public doSomething1(I)V
    .registers 3
    .param p1, "i"    # I

    const/4 v0, 0x1

    if-ne p1, v0, :cond_7

    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething2(I)V
    goto :goto_a

    :cond_7
    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething1(I)V

    :goto_a
    return-void
.end method

.method public doSomething2(I)V
    .registers 3
    .param p1, "i"    # I

    const/4 v0, 0x1

    if-ne p1, v0, :cond_7

    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V

    goto :goto_a

    :cond_7
    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething1(I)V

    :goto_a
    return-void
.end method

.method public mergeThrownExcetions()V
    .registers 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V
    return-void
.end method

.method public missingThrowsAnnotation()V
    .registers 1

    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V
    return-void
.end method
