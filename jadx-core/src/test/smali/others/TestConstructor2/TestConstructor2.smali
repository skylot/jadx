.class public Lothers/TestConstructor2;
.super Ljava/lang/Object;

.field public a:Ljava/util/HashMap;

.method public final a(III)V
    .registers 6

    .line 1
    .line 2
    new-instance v0, Lothers/TestConstructor2$A;

    .line 3
    .line 4
    .line 5
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 6
    .line 7
    if-gt p2, p3, :cond_1c

    .line 8
    .line 9
    new-instance p2, Ljava/util/ArrayDeque;

    .line 10
    .line 11
    iget v1, v0, Lothers/TestConstructor2$A;->a:I

    .line 12
    .line 13
    .line 14
    invoke-direct {p2, v1}, Ljava/util/ArrayDeque;-><init>(I)V

    .line 15
    .line 16
    iput-object p2, v0, Lothers/TestConstructor2$A;->b:Ljava/util/ArrayDeque;

    .line 17
    .line 18
    iput p3, v0, Lothers/TestConstructor2$A;->a:I

    .line 19
    .line 20
    iget-object p2, p0, Lothers/TestConstructor2;->a:Ljava/util/HashMap;

    .line 21
    .line 22
    .line 23
    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 24
    move-result-object p1

    .line 25
    .line 26
    .line 27
    invoke-virtual {p2, p1, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 28
    return-void

    .line 29
    .line 30
    :cond_1c
    new-instance p1, Ljava/lang/IllegalArgumentException;

    .line 31
    .line 32
    const-string/jumbo p2, "error"

    .line 33
    .line 34
    .line 35
    invoke-direct {p1, p2}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    .line 36
    throw p1
.end method
