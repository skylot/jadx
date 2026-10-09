.class public Lswitches/TestSwitchSharedCaseTargets;
.super Ljava/lang/Object;

.method public static test(II)V
    .registers 4

    packed-switch p0, :pswitch_data
    goto :end

    :case_cond
    const/4 v0, 0x3
    if-ne p1, v0, :do_stop
    goto :complete

    :do_stop
    invoke-static {}, Lswitches/TestSwitchSharedCaseTargets;->stop()V
    goto :fail

    :complete
    invoke-static {}, Lswitches/TestSwitchSharedCaseTargets;->complete()V
    goto :end

    :fail
    invoke-static {}, Lswitches/TestSwitchSharedCaseTargets;->fail()V

    :end
    return-void

    :pswitch_data
    .packed-switch 0x1
        :fail
        :complete
        :case_cond
    .end packed-switch
.end method

.method private static stop()V
    .registers 0
    return-void
.end method

.method private static fail()V
    .registers 0
    return-void
.end method

.method private static complete()V
    .registers 0
    return-void
.end method
