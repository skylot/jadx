.class public final Lconditions/TestComplexIf3;
.super Ljava/lang/Object;

.method public test(Landroid/os/Message;)V
    .registers 10

    .prologue
    const/16 v7, 0xf

    const/4 v6, 0x4

    const/4 v0, 0x0

    const/4 v1, 0x1

    const/4 v2, 0x0

    .line 3307
    const-string/jumbo v3, "Service"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string/jumbo v5, "handle: "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    iget v5, p1, Landroid/os/Message;->what:I

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v3, v4}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 3308
    iget v3, p1, Landroid/os/Message;->what:I

    sparse-switch v3, :sswitch_data_2d0

    .line 3516
    :cond_27
    :goto_27
    return-void

    .line 3312
    :sswitch_28
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3313
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap27(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    goto :goto_27

    .line 3318
    :sswitch_32
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap26(Lconditions/TestComplexIf3;)V

    goto :goto_27

    .line 3323
    :sswitch_38
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get18(Lconditions/TestComplexIf3;)Z

    move-result v0

    if-eqz v0, :cond_45

    .line 3324
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0, v2}, Lconditions/TestComplexIf3;->-wrap33(Lconditions/TestComplexIf3;Z)V

    .line 3326
    :cond_45
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap9(Lconditions/TestComplexIf3;)Z

    .line 3327
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap0(Lconditions/TestComplexIf3;)Z

    .line 3329
    new-instance v0, Landroid/os/Bundle;

    invoke-direct {v0, v1}, Landroid/os/Bundle;-><init>(I)V

    .line 3330
    const-string/jumbo v1, "flag"

    const/16 v2, 0xb

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3331
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap28(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    .line 3333
    invoke-static {}, Lconditions/TestComplexIf3;->-get28()Lconditions/TestComplexIf3$OnExitListener;

    move-result-object v0

    if-eqz v0, :cond_27

    .line 3334
    invoke-static {}, Lconditions/TestComplexIf3;->-get28()Lconditions/TestComplexIf3$OnExitListener;

    move-result-object v0

    invoke-interface {v0}, Lconditions/TestComplexIf3$OnExitListener;->onExit()V

    goto :goto_27

    .line 3340
    :sswitch_6f
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3341
    const-string/jumbo v3, "value"

    invoke-virtual {v0, v3}, Landroid/os/Bundle;->getInt(Ljava/lang/String;)I

    move-result v3

    .line 3347
    if-nez v3, :cond_8e

    .line 3349
    const-string/jumbo v2, "flag"

    invoke-virtual {v0, v2, v6}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3351
    const-string/jumbo v2, "key"

    invoke-virtual {v0, v2, v1}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    .line 3352
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap28(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    goto :goto_27

    .line 3357
    :cond_8e
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1}, Lconditions/TestComplexIf3;->-get22(Lconditions/TestComplexIf3;)I

    move-result v1

    const/4 v3, 0x7

    if-eq v1, v3, :cond_27

    .line 3358
    const-string/jumbo v1, "flag"

    invoke-virtual {v0, v1, v6}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3360
    const-string/jumbo v1, "key"

    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    .line 3361
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap28(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    goto/16 :goto_27

    .line 3368
    :sswitch_aa
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3369
    const-string/jumbo v1, "f"

    invoke-virtual {v0, v1}, Landroid/os/Bundle;->getFloat(Ljava/lang/String;)F

    move-result v0

    .line 3370
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap12(Lconditions/TestComplexIf3;F)Z

    move-result v1

    .line 3372
    if-nez v1, :cond_c7

    .line 3373
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v0

    invoke-static {v0}, Ltest/Utils;->computeFrequency(I)F

    move-result v0

    .line 3375
    :cond_c7
    new-instance v2, Landroid/os/Bundle;

    const/4 v3, 0x3

    invoke-direct {v2, v3}, Landroid/os/Bundle;-><init>(I)V

    .line 3376
    const-string/jumbo v3, "flag"

    invoke-virtual {v2, v3, v7}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3378
    const-string/jumbo v3, "key"

    invoke-virtual {v2, v3, v1}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    .line 3379
    const-string/jumbo v1, "key"

    invoke-virtual {v2, v1, v0}, Landroid/os/Bundle;->putFloat(Ljava/lang/String;F)V

    .line 3380
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0, v2}, Lconditions/TestComplexIf3;->-wrap28(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    goto/16 :goto_27

    .line 3385
    :sswitch_e6
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3386
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v3, v1}, Lconditions/TestComplexIf3;->-set5(Lconditions/TestComplexIf3;Z)Z

    .line 3387
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    const-string/jumbo v3, "f"

    invoke-virtual {v0, v3}, Landroid/os/Bundle;->getFloat(Ljava/lang/String;)F

    move-result v3

    .line 3388
    const-string/jumbo v4, "o"

    invoke-virtual {v0, v4}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v0

    .line 3387
    invoke-static {v1, v3, v0}, Lconditions/TestComplexIf3;->-wrap13(Lconditions/TestComplexIf3;FZ)F

    move-result v0

    .line 3390
    invoke-static {v0}, Ltest/Utils;->computeStation(F)I

    move-result v1

    .line 3391
    invoke-static {v1}, Ltest/Utils;->isValidStation(I)Z

    move-result v1

    if-eqz v1, :cond_2cd

    .line 3392
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap12(Lconditions/TestComplexIf3;F)Z

    move-result v1

    .line 3395
    :goto_113
    if-nez v1, :cond_11f

    .line 3396
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v0

    invoke-static {v0}, Ltest/Utils;->computeFrequency(I)F

    move-result v0

    .line 3398
    :cond_11f
    new-instance v3, Landroid/os/Bundle;

    const/4 v4, 0x2

    invoke-direct {v3, v4}, Landroid/os/Bundle;-><init>(I)V

    .line 3399
    const-string/jumbo v4, "flag"

    invoke-virtual {v3, v4, v7}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3401
    const-string/jumbo v4, "key_is_tune"

    invoke-virtual {v3, v4, v1}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    .line 3402
    const-string/jumbo v1, "key_tune_to_station"

    invoke-virtual {v3, v1, v0}, Landroid/os/Bundle;->putFloat(Ljava/lang/String;F)V

    .line 3403
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0, v3}, Lconditions/TestComplexIf3;->-wrap28(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    .line 3404
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0, v2}, Lconditions/TestComplexIf3;->-set5(Lconditions/TestComplexIf3;Z)Z

    goto/16 :goto_27

    .line 3414
    :sswitch_143
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v3, v1}, Lconditions/TestComplexIf3;->-set4(Lconditions/TestComplexIf3;Z)Z

    .line 3418
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v4}, Lconditions/TestComplexIf3;->-get6(Lconditions/TestComplexIf3;)Landroid/content/Context;

    move-result-object v4

    invoke-static {v4}, Ltest/Station;->getCurrentStation(Landroid/content/Context;)I

    move-result v4

    invoke-static {v3, v4}, Lconditions/TestComplexIf3;->-set0(Lconditions/TestComplexIf3;I)I

    .line 3419
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v3}, Lconditions/TestComplexIf3;->-get19(Lconditions/TestComplexIf3;)I

    move-result v3

    sget v4, Lconditions/TestComplexIf3;->POWER_UP:I

    if-eq v3, v4, :cond_185

    .line 3420
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v4}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v4

    invoke-static {v4}, Ltest/Utils;->computeFrequency(I)F

    move-result v4

    invoke-static {v3, v4}, Lconditions/TestComplexIf3;->-wrap10(Lconditions/TestComplexIf3;F)Z

    move-result v3

    if-eqz v3, :cond_21f

    .line 3421
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v4}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v4

    invoke-static {v4}, Ltest/Utils;->computeFrequency(I)F

    move-result v4

    invoke-static {v3, v4}, Lconditions/TestComplexIf3;->-wrap8(Lconditions/TestComplexIf3;F)Z

    move-result v3

    .line 3419
    if-eqz v3, :cond_2c9

    .line 3422
    :cond_185
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    if-eqz v0, :cond_222

    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/PowerManager$WakeLock;->isHeld()Z

    move-result v0

    xor-int/lit8 v0, v0, 0x1

    if-eqz v0, :cond_2c6

    .line 3424
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/PowerManager$WakeLock;->acquire()V

    move v0, v1

    .line 3426
    :goto_1a5
    const-string/jumbo v3, "Service"

    const-string/jumbo v4, "handle: start"

    invoke-static {v3, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 3427
    iget-object v3, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v3}, Lconditions/TestComplexIf3;->-wrap14(Lconditions/TestComplexIf3;)[I

    move-result-object v3

    .line 3429
    :goto_1b4
    const-string/jumbo v4, "Service"

    const-string/jumbo v5, "handle: end"

    invoke-static {v4, v5}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 3431
    if-eqz v3, :cond_224

    aget v4, v3, v2

    const/16 v5, -0x64

    if-ne v4, v5, :cond_224

    .line 3434
    const/4 v3, -0x1

    .line 3433
    filled-new-array {v3, v2}, [I

    move-result-object v3

    move-object v4, v3

    move v3, v2

    .line 3448
    :goto_1cc
    iget-object v5, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v5}, Lconditions/TestComplexIf3;->-get9(Lconditions/TestComplexIf3;)Z

    move-result v5

    if-eqz v5, :cond_1d9

    .line 3449
    iget-object v5, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-virtual {v5, v2}, Lconditions/TestComplexIf3;->setMute(Z)I

    .line 3452
    :cond_1d9
    if-eqz v0, :cond_1f8

    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    if-eqz v0, :cond_1f8

    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/PowerManager$WakeLock;->isHeld()Z

    move-result v0

    if-eqz v0, :cond_1f8

    .line 3453
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-get27(Lconditions/TestComplexIf3;)Landroid/os/PowerManager$WakeLock;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/PowerManager$WakeLock;->release()V

    .line 3456
    :cond_1f8
    new-instance v0, Landroid/os/Bundle;

    invoke-direct {v0, v6}, Landroid/os/Bundle;-><init>(I)V

    .line 3457
    const-string/jumbo v5, "callback_flag"

    .line 3458
    const/16 v6, 0xd

    .line 3457
    invoke-virtual {v0, v5, v6}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3460
    const-string/jumbo v5, "key_station_num"

    aget v1, v4, v1

    invoke-virtual {v0, v5, v1}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    .line 3461
    const-string/jumbo v1, "key_is_scan"

    invoke-virtual {v0, v1, v3}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V

    .line 3463
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v2}, Lconditions/TestComplexIf3;->-set4(Lconditions/TestComplexIf3;Z)Z

    .line 3465
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap29(Lconditions/TestComplexIf3;Landroid/os/Bundle;)V

    goto/16 :goto_27

    :cond_21f
    move-object v3, v0

    move v0, v2

    .line 3421
    goto :goto_1b4

    :cond_222
    move v0, v2

    .line 3422
    goto :goto_1a5

    .line 3437
    :cond_224
    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v4, v3}, Lconditions/TestComplexIf3;->-wrap15(Lconditions/TestComplexIf3;[I)[I

    move-result-object v3

    .line 3438
    aget v4, v3, v2

    .line 3439
    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    iget-object v5, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v5}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v5

    invoke-static {v5}, Ltest/Utils;->computeFrequency(I)F

    move-result v5

    invoke-static {v4, v5}, Lconditions/TestComplexIf3;->-wrap12(Lconditions/TestComplexIf3;F)Z

    move-result v4

    if-eqz v4, :cond_2c2

    .line 3440
    iget-object v4, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    iget-object v5, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v5}, Lconditions/TestComplexIf3;->-get7(Lconditions/TestComplexIf3;)I

    move-result v5

    invoke-virtual {v4, v5}, Lconditions/TestComplexIf3;->initService(I)V

    move-object v4, v3

    move v3, v1

    goto :goto_1cc

    .line 3470
    :sswitch_24c
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3471
    const-string/jumbo v1, "key_audiofocus_changed"

    invoke-virtual {v0, v1}, Landroid/os/Bundle;->getInt(Ljava/lang/String;)I

    move-result v0

    .line 3472
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap43(Lconditions/TestComplexIf3;I)V

    goto/16 :goto_27

    .line 3476
    :sswitch_25e
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3477
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    const-string/jumbo v2, "option"

    invoke-virtual {v0, v2}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v0

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap19(Lconditions/TestComplexIf3;Z)I

    goto/16 :goto_27

    .line 3481
    :sswitch_270
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3482
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    const-string/jumbo v2, "option"

    invoke-virtual {v0, v2}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v0

    invoke-virtual {v1, v0}, Lconditions/TestComplexIf3;->setMute(Z)I

    goto/16 :goto_27

    .line 3486
    :sswitch_282
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap16(Lconditions/TestComplexIf3;)I

    goto/16 :goto_27

    .line 3491
    :sswitch_289
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap38(Lconditions/TestComplexIf3;)V

    goto/16 :goto_27

    .line 3495
    :sswitch_290
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap11(Lconditions/TestComplexIf3;)Z

    goto/16 :goto_27

    .line 3499
    :sswitch_297
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3500
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    const-string/jumbo v2, "option"

    invoke-virtual {v0, v2}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v0

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap36(Lconditions/TestComplexIf3;Z)V

    goto/16 :goto_27

    .line 3504
    :sswitch_2a9
    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object v0

    .line 3505
    iget-object v1, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    const-string/jumbo v2, "name"

    invoke-virtual {v0, v2}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lconditions/TestComplexIf3;->-wrap32(Lconditions/TestComplexIf3;Ljava/lang/String;)V

    goto/16 :goto_27

    .line 3510
    :sswitch_2bb
    iget-object v0, p0, Lconditions/TestComplexIf3$Handler;->this$0:Lconditions/TestComplexIf3;

    invoke-static {v0}, Lconditions/TestComplexIf3;->-wrap37(Lconditions/TestComplexIf3;)V

    goto/16 :goto_27

    :cond_2c2
    move-object v4, v3

    move v3, v1

    goto/16 :goto_1cc

    :cond_2c6
    move v0, v2

    goto/16 :goto_1a5

    :cond_2c9
    move-object v3, v0

    move v0, v2

    goto/16 :goto_1b4

    :cond_2cd
    move v1, v2

    goto/16 :goto_113

    .line 3308
    :sswitch_data_2d0
    .sparse-switch
        0x4 -> :sswitch_6f
        0x5 -> :sswitch_25e
        0x7 -> :sswitch_270
        0x9 -> :sswitch_28
        0xa -> :sswitch_32
        0xb -> :sswitch_38
        0xd -> :sswitch_143
        0xf -> :sswitch_aa
        0x10 -> :sswitch_e6
        0x12 -> :sswitch_282
        0x15 -> :sswitch_297
        0x16 -> :sswitch_289
        0x17 -> :sswitch_290
        0x1a -> :sswitch_2a9
        0x1e -> :sswitch_24c
        0x66 -> :sswitch_2bb
    .end sparse-switch
.end method
