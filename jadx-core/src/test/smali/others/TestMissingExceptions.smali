.class public Lothers/TestMissingExceptions;
.super Ljava/lang/Object;

# direct methods
.method public constructor <init>()V
    .registers 1

    .line 6
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private exceptionSource()V
    .registers 3

    .line 8
    new-instance v0, Ljava/io/FileNotFoundException;

    const-string v1, ""

    invoke-direct {v0, v1}, Ljava/io/FileNotFoundException;-><init>(Ljava/lang/String;)V

    throw v0
.end method

# virtual methods
.method public doSomething1(I)V
    .registers 3
    .param p1, "i"    # I

    .line 20
    const/4 v0, 0x1

    if-ne p1, v0, :cond_7

    .line 21
    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething2(I)V

    goto :goto_a

    .line 23
    :cond_7
    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething1(I)V

    .line 25
    :goto_a
    return-void
.end method

.method public doSomething2(I)V
    .registers 3
    .param p1, "i"    # I

    .line 28
    const/4 v0, 0x1

    if-ne p1, v0, :cond_7

    .line 29
    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V

    goto :goto_a

    .line 31
    :cond_7
    invoke-virtual {p0, p1}, Lothers/TestMissingExceptions;->doSomething1(I)V

    .line 33
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

    .line 12
    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V

    .line 13
    return-void
.end method

.method public missingThrowsAnnotation()V
    .registers 1

    .line 16
    invoke-direct {p0}, Lothers/TestMissingExceptions;->exceptionSource()V

    .line 17
    return-void
.end method
