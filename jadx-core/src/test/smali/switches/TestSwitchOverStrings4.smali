.class public Lswitches/TestSwitchOverStrings4;
.super Landroid/support/v7/app/AppCompatActivity;

.method private test()V
    .registers 7

    .line 220
    invoke-virtual {p0}, Lswitches/TestSwitchOverStrings4;->getSupportFragmentManager()Landroid/support/v4/app/FragmentManager;

    move-result-object v0

    const v1, 0x7f090070

    invoke-virtual {v0, v1}, Landroid/support/v4/app/FragmentManager;->findFragmentById(I)Landroid/support/v4/app/Fragment;

    move-result-object v0

    .line 221
    invoke-virtual {v0}, Landroid/support/v4/app/Fragment;->getTag()Ljava/lang/String;

    move-result-object v0

    const/4 v1, 0x0

    if-nez v0, :cond_16

    .line 225
    invoke-direct {p0, v1}, Lswitches/TestSwitchOverStrings4;->setHomeIsActionBack(Z)V

    return-void

    :cond_16
    const-string v2, "CONTAINER_PROP"

    .line 230
    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    const/4 v3, 0x1

    if-nez v2, :cond_2c

    const-string v2, "CHOOSE_FILE"

    .line 231
    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_28

    goto :goto_2c

    .line 236
    :cond_28
    invoke-direct {p0, v1}, Lswitches/TestSwitchOverStrings4;->setHomeIsActionBack(Z)V

    goto :goto_2f

    .line 233
    :cond_2c
    :goto_2c
    invoke-direct {p0, v3}, Lswitches/TestSwitchOverStrings4;->setHomeIsActionBack(Z)V

    :goto_2f
    const/4 v2, -0x1

    .line 240
    invoke-virtual {v0}, Ljava/lang/String;->hashCode()I

    move-result v4

    const v5, -0x7864e404

    if-eq v4, v5, :cond_67

    const v1, -0x3f1aacc4

    if-eq v4, v1, :cond_5d

    const v1, -0x1d0b6cd4

    if-eq v4, v1, :cond_53

    const v1, 0x2ad8bc

    if-eq v4, v1, :cond_49

    goto :goto_70

    :cond_49
    const-string v1, "INSTALL_NEW"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_70

    const/4 v1, 0x2

    goto :goto_71

    :cond_53
    const-string v1, "MANAGE_CONTAINERS"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_70

    const/4 v1, 0x3

    goto :goto_71

    :cond_5d
    const-string v1, "START_MENU"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_70

    move v1, v3

    goto :goto_71

    :cond_67
    const-string v3, "DESKTOP"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_70

    goto :goto_71

    :cond_70
    :goto_70
    move v1, v2

    :goto_71
    packed-switch v1, :pswitch_data_9a

    goto :goto_98

    .line 252
    :pswitch_75
    iget-object v0, p0, Lswitches/TestSwitchOverStrings4;->mNavigationView:Landroid/support/design/widget/NavigationView;

    const v1, 0x7f090074

    invoke-virtual {v0, v1}, Landroid/support/design/widget/NavigationView;->setCheckedItem(I)V

    goto :goto_98

    .line 249
    :pswitch_7e
    iget-object v0, p0, Lswitches/TestSwitchOverStrings4;->mNavigationView:Landroid/support/design/widget/NavigationView;

    const v1, 0x7f090073

    invoke-virtual {v0, v1}, Landroid/support/design/widget/NavigationView;->setCheckedItem(I)V

    goto :goto_98

    .line 246
    :pswitch_87
    iget-object v0, p0, Lswitches/TestSwitchOverStrings4;->mNavigationView:Landroid/support/design/widget/NavigationView;

    const v1, 0x7f090075

    invoke-virtual {v0, v1}, Landroid/support/design/widget/NavigationView;->setCheckedItem(I)V

    goto :goto_98

    .line 243
    :pswitch_90
    iget-object v0, p0, Lswitches/TestSwitchOverStrings4;->mNavigationView:Landroid/support/design/widget/NavigationView;

    const v1, 0x7f090071

    invoke-virtual {v0, v1}, Landroid/support/design/widget/NavigationView;->setCheckedItem(I)V

    :goto_98
    return-void

    nop

    :pswitch_data_9a
    .packed-switch 0x0
        :pswitch_90
        :pswitch_87
        :pswitch_7e
        :pswitch_75
    .end packed-switch
.end method
