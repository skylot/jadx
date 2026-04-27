.class public Lswitches/TestSwitchOverStrings5;
.super Ljava/lang/Object;


.method public enableClockSync()Z
    .registers 2

    .line 175
    iget-object p0, p0, Lswitches/TestSwitchOverStrings5;->modelId:Ljava/lang/String;

    invoke-virtual {p0}, Ljava/lang/String;->hashCode()I

    move-result v0

    sparse-switch v0, :sswitch_data_60

    goto :goto_5d

    :sswitch_a
    const-string v0, "mp3_310"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_13
    const-string v0, "liberty_e5plus_150"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_1c
    const-string v0, "liberty_e5plus_125"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_5d

    goto :goto_5b

    :sswitch_25
    const-string v0, "p197"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_2e
    const-string v0, "beverly_my_2020_350"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_37
    const-string v0, "liberty_e5plus_50"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_40
    const-string v0, "mp3_300_hpe_e5"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_49
    const-string v0, "mp3_500_e5"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :sswitch_52
    const-string v0, "mp3_400_e5"

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_5b

    goto :goto_5d

    :cond_5b
    :goto_5b
    const/4 p0, 0x0

    goto :goto_5e

    :cond_5d
    :goto_5d
    const/4 p0, 0x1

    :goto_5e
    return p0

    nop

    :sswitch_data_60
    .sparse-switch
        -0x7006ac76 -> :sswitch_52
        -0x6e51d3d7 -> :sswitch_49
        -0x670ba373 -> :sswitch_40
        -0x5287fd58 -> :sswitch_37
        -0x377a5db4 -> :sswitch_2e
        0x33a89f -> :sswitch_25
        0x18843c7 -> :sswitch_1c
        0x188441f -> :sswitch_13
        0x4820a3c3 -> :sswitch_a
    .end sparse-switch
.end method

