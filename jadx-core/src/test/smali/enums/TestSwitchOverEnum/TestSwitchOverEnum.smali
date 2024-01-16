.class public Lenums/TestSwitchOverEnum;
.super Ljava/lang/Object;

.method public test(Lenums/TestSwitchOverEnum$Count;)I
    .registers 3
    .param p1, "v"

    invoke-virtual {p1}, Lenums/TestSwitchOverEnum$Count;->ordinal()I
    move-result v0

    packed-switch v0, :pswitch_data
    const/4 v0, 0x0

    :goto_8
    return v0

    :pswitch_9
    const/4 v0, 0x1
    goto :goto_8

    :pswitch_b
    const/4 v0, 0x2
    goto :goto_8

    :pswitch_data
    .packed-switch 0x0
        :pswitch_9
        :pswitch_b
    .end packed-switch
.end method
