.class public Linline/TestInline7;
.super Ljava/lang/Object;

.method public onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V
    .locals 4
    .param p2    # Landroid/os/Bundle;
        .annotation build Landroid/support/annotation/Nullable;
        .end annotation
    .end param

    .line 42
    invoke-super {p0, p1, p2}, Lapp/navigation/fragment/NodeFragment;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V

    .line 43
    sget p2, Lapp/wallet/R$id;->done_button_early_release_failure:I

    invoke-virtual {p1, p2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p2

    new-instance v0, Lapp/common/utils/SafeClickListener;

    invoke-direct {v0, p0}, Lapp/common/utils/SafeClickListener;-><init>(Lapp/common/utils/ISafeClickVerifierListener;)V

    invoke-virtual {p2, v0}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 44
    invoke-virtual {p0}, Linline/TestInline7;->getArguments()Landroid/os/Bundle;

    move-result-object p2

    if-eqz p2, :cond_0

    .line 46
    sget v0, Lapp/wallet/R$id;->summary_content_early_release_failure:I

    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Landroid/widget/TextView;

    sget v0, Lapp/wallet/R$string;->withdraw_id_capture_failure_content:I

    const/4 v1, 0x2

    new-array v1, v1, [Ljava/lang/Object;

    const/4 v2, 0x0

    const-string v3, "withdrawAmount"

    invoke-virtual {p2, v3}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    aput-object v3, v1, v2

    const/4 v2, 0x1

    const-string v3, "withdrawHoldTime"

    invoke-virtual {p2, v3}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p2

    aput-object p2, v1, v2

    invoke-virtual {p0, v0, v1}, Linline/TestInline7;->getString(I[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    :cond_0
    return-void
.end method
