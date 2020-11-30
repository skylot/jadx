.class public Ltrycatch/TestTryWithEmptyCatchTriple;
.super Ljava/lang/Object;

.field static field:[I

.method static test()V
    .registers 3
    const/4 v0, 0x1

    :try_start_1
    sget-object v1, Ltrycatch/TestTryWithEmptyCatchTriple;->field:[I

    const/4 v2, 0x0

    aput v0, v1, v2

    :try_end_1
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_1

    :catch_1

    sget-object v1, Ltrycatch/TestTryWithEmptyCatchTriple;->field:[I

    array-length v1, v1

    new-array v1, v1, [I

    sput-object v1, Ltrycatch/TestTryWithEmptyCatchTriple;->field:[I

    :try_start_2
    sget-object v1, Ltrycatch/TestTryWithEmptyCatchTriple;->field:[I

    const/4 v2, 0x0

    aput v0, v1, v2
    :try_end_2
    .catch Ljava/lang/Error; {:try_start_2 .. :try_end_2} :catch_2

    :catch_2
    :try_start_3
    sget-object v0, Ltrycatch/TestTryWithEmptyCatchTriple;->field:[I

    const/4 v1, 0x0

    const/4 v2, 0x2

    aput v2, v0, v1
    :try_end_3
    .catch Ljava/lang/Error; {:try_start_3 .. :try_end_3} :catch_3

    :catch_3
    return-void
.end method
