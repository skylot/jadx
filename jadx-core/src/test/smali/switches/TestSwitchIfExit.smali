.class public Lswitches/TestSwitchIfExit;
.super Ljava/lang/Object;

.method public static test(II)I
    .registers 4

    const/4 v0, 0x0
    packed-switch p0, :pswitch_data_0
    goto :exit

    :pswitch_0
    const/4 v0, 0x1
    if-lez p1, :exit
    const/4 v0, 0x2
    goto :exit

    :pswitch_1
    const/4 v0, 0x3

    :exit
    add-int/2addr v0, p1
    return v0

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
        :pswitch_1
    .end packed-switch
.end method
