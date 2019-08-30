.class public Lothers/TestAllNops;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1

    .line 55
    nop

    nop

    nop

    nop
.end method

.method private test()Z
    .registers 11

    .line 1480
    nop

    nop

    .line 1481
    nop

    nop

    nop

    nop

    nop

    nop

    .line 1485
    nop

    nop

    .line 1486
    nop

    nop

    .line 1487
    nop

.end method

.method private testWithTryCatch()Z
    .registers 11

    .line 1480
    :try_start_0
    nop
    nop

    .line 1481
    nop

    nop

    nop

    nop

    nop

    nop

    .line 1485
    nop

    nop

    :try_end_35
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_0 .. :try_end_35} :catch_36

    nop

    .line 1547
    :catch_36

    nop

    .line 1487
    nop

.end method
