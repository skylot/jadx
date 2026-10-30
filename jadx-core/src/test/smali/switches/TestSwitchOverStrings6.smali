.class public Lswitches/TestSwitchOverStrings6;
.super Ljava/lang/Object;

.method public test(Ljava/lang/String;)V
    .registers 3

    invoke-virtual {p1}, Ljava/lang/String;->hashCode()I
    move-result v0
    sparse-switch v0, :sswitch_data_20
    return-void

    :sswitch_a
    const-string v0, "a"
    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result p1
    if-nez p1, :cond_15
    return-void

    :cond_15
    invoke-static {}, Lswitches/TestSwitchOverStrings6;->callA()V
    return-void

    :sswitch_16
    const-string v0, "b"
    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result p1
    if-nez p1, :cond_1f
    return-void

    :cond_1f
    invoke-static {}, Lswitches/TestSwitchOverStrings6;->callB()V
    return-void

    :sswitch_data_20
    .sparse-switch
        0x61 -> :sswitch_a
        0x62 -> :sswitch_16
    .end sparse-switch
.end method

.method public static callA()V
    .registers 0
    return-void
.end method

.method public static callB()V
    .registers 0
    return-void
.end method
