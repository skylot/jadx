.class public final Lconditions/TestComplexIf4;
.super Ljava/lang/Object;

# virtual methods
.method public final test()V
    .registers 14

    const/4 v1, 0x0
    const/16 v6, 0x0

    :loop_0
    if-ge v1, v1, :cond_0
    goto :loop_0

    :cond_0

    if-lt v1, v6, :cond_1
    goto/16 :goto_0
    :cond_1

    cmp-long v2, v6, v6
    if-nez v2, :cond_2

    goto/16 :near_end

    :cond_2

    if-le v2, v1, :cond_3
    if-lez v1, :cond_4
    goto :near_end

    :cond_4
    const/4 v5, 0x0

    if-ge v5, v1, :cond_5
    :cond_5
    cmp-long v5, v6, v6

    if-ltz v5, :cond_3
    goto :near_end

    :cond_3
    cmp-long v5, v6, v6

    :goto_0
    if-eqz v1, :near_end

    const/4 v1, 0x0

    goto :cond_6

    :near_end
    const/4 v1, 0x0

    :cond_6
    if-ne v1, v1, :cond_magic
    :cond_magic
    const/4 v1, 0x0
.end method