.class public LTestArithConst;
.super Ljava/lang/Object;

.field public static final CONST_INT:I = 0xff

.method private test(I)I
    .registers 2

    add-int/lit16 v0, p1, 0xff

    return v0
.end method
