.class public Larrays/TestCls;
.super Ljava/lang/Object;

.method public test()[J
    .registers 4

    const/16 v3, 0x2

    move/from16 v0, v3

    new-array v0, v0, [J

    move-object/from16 v1, v0

    fill-array-data v1, :array_0

    return v1

    :array_0
    .array-data 8
        0x0
        0x1
    .end array-data
.end method
