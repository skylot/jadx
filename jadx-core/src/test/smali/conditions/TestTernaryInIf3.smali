.class public Lconditions/TestTernaryInIf3;
.super Ljava/lang/Object;

.method public static final A01(LX/73h;FFZ)Landroid/util/Pair;
    .registers 7

    .line 0
    const/4 v1, 0x0

    .line 1
    cmpl-float v0, p1, v1

    .line 2
    .line 3
    if-eqz v0, :cond_33

    .line 4
    .line 5
    cmpl-float v0, p2, v1

    .line 6
    .line 7
    if-eqz v0, :cond_33

    .line 8
    .line 9
    if-eqz p3, :cond_24

    .line 10
    .line 11
    cmpg-float v0, p2, v1

    .line 12
    .line 13
    if-ltz v0, :cond_26

    .line 14
    .line 15
    :cond_f
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 16
    .line 17
    const-string v0, "translationXCurveDownwards"

    .line 18
    .line 19
    invoke-virtual {v1, v0}, LX/5Yj;->A03(Ljava/lang/String;)LX/5Wd;

    .line 20
    .line 21
    .line 22
    move-result-object v2

    .line 23
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 24
    .line 25
    const-string v0, "translationYCurveDownwards"

    .line 26
    .line 27
    :goto_1b
    invoke-virtual {v1, v0}, LX/5Yj;->A03(Ljava/lang/String;)LX/5Wd;

    .line 28
    .line 29
    .line 30
    move-result-object v0

    .line 31
    invoke-static {v2, v0}, LX/0xz;->A0F(Ljava/lang/Object;Ljava/lang/Object;)Landroid/util/Pair;

    .line 32
    .line 33
    .line 34
    move-result-object v0

    .line 35
    return-object v0

    .line 36
    :cond_24
    if-lez v0, :cond_f

    .line 37
    .line 38
    :cond_26
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 39
    .line 40
    const-string v0, "translationXCurveUpwards"

    .line 41
    .line 42
    invoke-virtual {v1, v0}, LX/5Yj;->A03(Ljava/lang/String;)LX/5Wd;

    .line 43
    .line 44
    .line 45
    move-result-object v2

    .line 46
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 47
    .line 48
    const-string v0, "translationYCurveUpwards"

    .line 49
    .line 50
    goto :goto_1b

    .line 51
    :cond_33
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 52
    .line 53
    const-string v0, "translationXLinear"

    .line 54
    .line 55
    invoke-virtual {v1, v0}, LX/5Yj;->A03(Ljava/lang/String;)LX/5Wd;

    .line 56
    .line 57
    .line 58
    move-result-object v2

    .line 59
    iget-object v1, p0, LX/73h;->A00:LX/5Yj;

    .line 60
    .line 61
    const-string v0, "translationYLinear"

    .line 62
    .line 63
    goto :goto_1b
    .line 64
    .line 65
    .line 66
    .line 67
    .line 68
    .line 69
    .line 70
    .line 71
    .line 72
    .line 73
    .line 74
    .line 75
    .line 76
    .line 77
.end method
