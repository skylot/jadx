.class public final Linline/TestSyntheticInline3;
.super Landroid/app/Activity;

.field private dialog:Landroidx/appcompat/app/AlertDialog;

.method public static final synthetic access$getChooserIntent(Linline/TestSyntheticInline3;Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;
    .registers 3
    invoke-direct {p0, p1, p2}, Linline/TestSyntheticInline3;->getChooserIntent(Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;
    move-result-object p0
    return-object p0
.end method

.method public static final synthetic access$getDialog$p(Linline/TestSyntheticInline3;)Landroidx/appcompat/app/AlertDialog;
    .registers 1
    iget-object p0, p0, Linline/TestSyntheticInline3;->dialog:Landroidx/appcompat/app/AlertDialog;
    return-object p0
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 3
    invoke-super {p0, p1}, Landroidx/fragment/app/FragmentActivity;->onCreate(Landroid/os/Bundle;)V
    new-instance v0, Linline/TestSyntheticInline3$onCreate$1;
    invoke-direct {v0, p0}, Linline/TestSyntheticInline3$onCreate$1;-><init>(Linline/TestSyntheticInline3;)V
    return-void
.end method


.method private final getChooserIntent(Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;
    .registers 5
    new-instance v0, Landroid/content/Intent;
    invoke-direct {v0, p1}, Landroid/content/Intent;-><init>(Landroid/content/Intent;)V
    return-object v0
.end method
