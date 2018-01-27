.class public LTestArithNot;
.super Ljava/lang/Object;

.method private test1(I)I
    .registers 2
    .param p1, "a"

    not-int v0, p1

    return v0
.end method

.method private test2(J)J
    .registers 4
    .param p1, "b"

    not-long v0, p1

    return v0
.end method
