.class public LTestSwitchOverStrings4;
.super Ljava/lang/Object;

.method public static test4(Ljava/lang/String;)I
    .registers 10

    const/16 v3, 0x0

    const/16 v4, -0x1

    if-nez p0, :cond_26

    return v4

    :cond_26
    .line 202

    invoke-virtual {p0}, Ljava/lang/String;->hashCode()I

    move-result v2

    sparse-switch v2, :sswitch_data_222

    const/4 v0, -0x1

    const/16 v2, 0x13

    goto/16 :goto_20a

    :sswitch_3b
    const/16 v2, 0x13

    const-string/jumbo v1, "video/x-matroska"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_48

    goto/16 :goto_207

    :cond_48
    const/16 v0, 0x1

    goto/16 :goto_20a

    :sswitch_1fd
    const/16 v2, 0x13

    const-string v1, "audio/eac3-joc"

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_209

    :goto_207
    const/4 v0, -0x1

    goto :goto_20a

    :cond_209
    const/4 v0, 0x0

    :goto_20a
    packed-switch v0, :pswitch_data_29c

    return v4

    :pswitch_216
    return v2

    :pswitch_221
    return v3

    :sswitch_data_222
    .sparse-switch
        0xb269699 -> :sswitch_1fd
        0x79909c15 -> :sswitch_3b
    .end sparse-switch

    :pswitch_data_29c
    .packed-switch 0x0
        :pswitch_221
        :pswitch_216
    .end packed-switch
.end method
