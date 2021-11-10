.class final Linline/TestSyntheticInline3$onCreate$1;
.super Lkotlin/jvm/internal/Lambda;

.implements Lkotlin/jvm/functions/Function1;

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x18
    name = null
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lkotlin/jvm/internal/Lambda;",
        "Lkotlin/jvm/functions/Function1<",
        "Ljava/lang/String;",
        "Lkotlin/Unit;",
        ">;"
    }
.end annotation


.field final synthetic this$0:Linline/TestSyntheticInline3;


.method constructor <init>(Linline/TestSyntheticInline3;)V
    .registers 2
    iput-object p1, p0, Linline/TestSyntheticInline3$onCreate$1;->this$0:Linline/TestSyntheticInline3;
    const/4 p1, 0x1
    invoke-direct {p0, p1}, Lkotlin/jvm/internal/Lambda;-><init>(I)V
    return-void
.end method


.method public bridge synthetic invoke(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 2
    check-cast p1, Ljava/lang/String;
    invoke-virtual {p0, p1}, Linline/TestSyntheticInline3$onCreate$1;->invoke(Ljava/lang/String;)V
    sget-object p1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object p1
.end method

.method public final invoke(Ljava/lang/String;)V
    .registers 5
    iget-object v0, p0, Linline/TestSyntheticInline3$onCreate$1;->this$0:Linline/TestSyntheticInline3;
    invoke-static {v0}, Linline/TestSyntheticInline3;->access$getDialog$p(Linline/TestSyntheticInline3;)Landroidx/appcompat/app/AlertDialog;
    move-result-object v0
    if-nez v0, :cond_9
    goto :goto_c
    :cond_9
    invoke-virtual {v0}, Landroidx/appcompat/app/AppCompatDialog;->dismiss()V
    :goto_c
    iget-object v0, p0, Linline/TestSyntheticInline3$onCreate$1;->this$0:Linline/TestSyntheticInline3;
    invoke-virtual {v0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v1
    const-string v2, "intent"
    invoke-static {v1, v2}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-static {v0, v1, p1}, Linline/TestSyntheticInline3;->access$getChooserIntent(Linline/TestSyntheticInline3;Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;
    move-result-object p1
    invoke-virtual {v0, p1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
    iget-object p1, p0, Linline/TestSyntheticInline3$onCreate$1;->this$0:Linline/TestSyntheticInline3;
    invoke-virtual {p1}, Landroid/app/Activity;->finish()V
    return-void
.end method
