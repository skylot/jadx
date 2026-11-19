.class public Lswitches/TestSwitchOverStrings6;
.super Ljava/lang/Object;

.method public test(Ljava/lang/String;)I
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p1}, Ljava/lang/String;->hashCode()I
    move-result v1

    sparse-switch v1, :sswitch_data_0
    const/4 v0, -0x1
    goto :goto_part2

    :sswitch_beta
    const-string v1, "beta"
    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :cond_beta_fail
    const/4 v0, 0x1
    goto :goto_part2

    :cond_beta_fail
    const/4 v0, -0x1
    goto :goto_part2

    :sswitch_alpha
    const-string v1, "alpha"
    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :cond_alpha_fail
    # v0 is already 0 from entry block (iDom) before hashCode()
    goto :goto_part2

    :cond_alpha_fail
    const/4 v0, -0x1

    :goto_part2
    packed-switch v0, :pswitch_data_0

    const/16 v0, 0x1e
    return v0

    :pswitch_0
    const/16 v0, 0xa
    return v0

    :pswitch_1
    const/16 v0, 0x14
    return v0

    :sswitch_data_0
    .sparse-switch
        0x2e15f0 -> :sswitch_beta
        0x589b15e -> :sswitch_alpha
    .end sparse-switch

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
        :pswitch_1
    .end packed-switch
.end method
