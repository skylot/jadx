.class LParamsTest;
.super Ljava/lang/Object;

.method public test(Landroid/widget/AdapterView;Landroid/view/View;IJ)V
    .registers 10
    .param p2, "arg1"    # Landroid/view/View;
    .param p3, "arg2"    # I
    .param p4, "arg3"    # J
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/widget/AdapterView",
            "<*>;",
            "Landroid/view/View;",
            "IJ)V"
        }
    .end annotation

    .prologue
    .line 69
    .local p1, "arg0":Landroid/widget/AdapterView;, "Landroid/widget/AdapterView<*>;"
    iget-object v2, p0, LParamsTest;->this$0:Ltest/ColorListActivity;

    .line 72
    iget-object v2, v2, Ltest/ColorListActivity;->mSortedColorList:[Ljava/lang/String;

    .line 75
    aget-object v0, v2, p3

    .line 80
    .local v0, "colorString":Ljava/lang/String;
    new-instance v1, Landroid/content/Intent;

    .line 83
    iget-object v2, p0, LParamsTest;->this$0:Ltest/ColorListActivity;

    .line 86
    const-class v3, Ltest/ColorItemActivity;

    .line 89
    invoke-direct {v1, v2, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    .line 94
    .local v1, "intent":Landroid/content/Intent;
    const-string v2, "colorString"

    .line 97
    invoke-virtual {v1, v2, v0}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 101
    iget-object v2, p0, LParamsTest;->this$0:Ltest/ColorListActivity;

    .line 104
    invoke-virtual {v2, v1}, Ltest/ColorListActivity;->startActivity(Landroid/content/Intent;)V

    .line 108
    return-void
.end method
