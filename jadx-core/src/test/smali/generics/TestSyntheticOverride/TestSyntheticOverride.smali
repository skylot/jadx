.class final Lgenerics/TestSyntheticOverride;
.super Ljava/lang/Object;

.implements Lkotlin/jvm/functions/Function1;

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lkotlin/jvm/internal/Lambda;",
        "Lkotlin/jvm/functions/Function1<",
        "Ljava/lang/String;",
        "Lkotlin/Unit;",
        ">;"
    }
.end annotation

.field final synthetic this$0:Lgenerics/TestSyntheticOverrideParent;


.method public bridge synthetic invoke(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 2
    check-cast p1, Ljava/lang/String;
    invoke-virtual {p0, p1}, Lgenerics/TestSyntheticOverride;->invoke(Ljava/lang/String;)V
    sget-object p1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object p1
.end method

.method public final invoke(Ljava/lang/String;)V
    .registers 5
    iget-object v0, p0, Lgenerics/TestSyntheticOverride;->this$0:Lgenerics/TestSyntheticOverrideParent;
    invoke-static {v0}, Lgenerics/TestSyntheticOverride;->access$getDialog$p(Lgenerics/TestSyntheticOverride;)Landroidx/appcompat/app/AlertDialog;
    move-result-object v0
    if-nez v0, :cond_9
    goto :goto_c

    :cond_9
    invoke-virtual {v0}, Landroidx/appcompat/app/AppCompatDialog;->dismiss()V

    :goto_c
    iget-object v0, p0, Lgenerics/TestSyntheticOverride;->this$0:Lgenerics/TestSyntheticOverrideParent;
    invoke-virtual {v0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v1
    const-string v2, "intent"
    invoke-static {v1, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-static {v0, v1, p1}, Lgenerics/TestSyntheticOverride;->access$getChooserIntent(Lgenerics/TestSyntheticOverride;Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;
    move-result-object p1
    invoke-virtual {v0, p1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
    iget-object p1, p0, Lgenerics/TestSyntheticOverride;->this$0:Lgenerics/TestSyntheticOverrideParent;
    invoke-virtual {p1}, Landroid/app/Activity;->finish()V
    return-void
.end method
