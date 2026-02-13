.class public LTestSwitchOverStrings3;
.super Ljava/lang/Object;

.method public test3(Ljava/lang/String;)I
    .registers 5

    .line 87
    invoke-virtual {p1}, Ljava/lang/String;->hashCode()I

    move-result v0

    const/4 v1, 0x0

    const/4 v2, 0x1

    packed-switch v0, :pswitch_data_38

    :cond_9
    goto :goto_32

    :pswitch_a
    const-string v0, "branch4"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_9

    const/4 p1, 0x3

    goto :goto_33

    :pswitch_14
    const-string v0, "branch3"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_9

    const/4 p1, 0x2

    goto :goto_33

    :pswitch_1e
    const-string v0, "branch2"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_9

    const/4 p1, 0x1

    goto :goto_33

    :pswitch_28
    const-string v0, "branch1"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_9

    const/4 p1, 0x0

    goto :goto_33

    :goto_32
    const/4 p1, -0x1

    :goto_33
    packed-switch p1, :pswitch_data_44

    .line 94
    return v1

    .line 90
    :pswitch_37
    return v2

    :pswitch_data_38
    .packed-switch 0x8358ecf
        :pswitch_28
        :pswitch_1e
        :pswitch_14
        :pswitch_a
    .end packed-switch

    :pswitch_data_44
    .packed-switch 0x0
        :pswitch_37
        :pswitch_37
    .end packed-switch
.end method
