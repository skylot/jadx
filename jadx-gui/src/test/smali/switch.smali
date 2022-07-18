.class public final LSwitchTest;
.super Ljava/lang/Object;

.field public static final synthetic a:LSwitchTest;
.field public static final synthetic b:LSwitchTest;

.field private final synthetic c:I

.method public final test(Ljava/lang/Runnable;)Ljava/lang/Thread;
    .registers 4
    const v0, 0xa
    const v1, 0xa
    add-int v0, v0, v1
    rem-int v0, v0, v1
    if-gtz v0, :cond_f
    goto/32 :goto_d2

    :cond_f
    :goto_f
    goto/32 :goto_bf
    :goto_18
    const-string v1, "A"
    goto/32 :goto_68
    :goto_26
    const-string v1, "B"
    goto/32 :goto_7c
    :goto_33
    return-object v0

    :pswitch_38
    goto/32 :goto_4e
    :goto_41
    new-instance v0, Labo;
    goto/32 :goto_84
    :goto_4e
    new-instance v0, Lwf;
    goto/32 :goto_ab
    :goto_5b
    new-instance v0, Ljava/lang/Thread;
    goto/32 :goto_18
    :goto_68
    invoke-direct {v0, p1, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;Ljava/lang/String;)V
    goto/32 :goto_71
    :goto_71
    return-object v0
    :pswitch_75
    goto/32 :goto_b3
    :goto_7c
    invoke-direct {v0, p1, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;Ljava/lang/String;)V
    goto/32 :goto_33
    :goto_84
    invoke-direct {v0, p1}, Labo;-><init>(Ljava/lang/Runnable;)V
    goto/32 :goto_90
    :goto_90
    return-object v0

    :pswitch_data_96
    .packed-switch 0x0
        :pswitch_a3
        :pswitch_38
        :pswitch_75
    .end packed-switch

    :goto_a0
    return-object v0
    :pswitch_a3
    goto/32 :goto_41
    :goto_ab
    invoke-direct {v0, p1}, Lwf;-><init>(Ljava/lang/Runnable;)V
    goto/32 :goto_a0
    :goto_b3
    new-instance v0, Ljava/lang/Thread;
    goto/32 :goto_26
    :goto_bf
    iget v0, p0, LSwitchTest;->c:I

    packed-switch v0, :pswitch_data_96
    goto/32 :goto_5b
    :goto_d2
    goto/32 :goto_f
.end method
