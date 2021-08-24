.class public Ltypes/TestPrimitiveConversion2;
.super Ljava/lang/Object;


.method protected test(Landroid/widget/TextView;Lapp/ItemCurrency;Lapp/ItemCurrency;Lapp/ItemCurrency;Lapp/ItemCurrency;ZLapp/SearchListItem;)Z
    .locals 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/widget/TextView;",
            "Lapp/ItemCurrency;",
            "Lapp/ItemCurrency;",
            "Lapp/ItemCurrency;",
            "Lapp/ItemCurrency;",
            "ZLapp/SearchListItem;)Z"
        }
    .end annotation

    .line 573
    invoke-direct {p0, p2, p3}, Lapp/DefaultItemAdapter;->getConvertedPrice(Lapp/ItemCurrency;Lapp/ItemCurrency;)Lapp/ItemCurrency;

    move-result-object p3

    const/4 v0, 0x0

    if-eqz p3, :cond_3

    .line 577
    iget-object v1, p3, Lapp/ItemCurrency;->code:Ljava/lang/String;

    iget-object p2, p2, Lapp/ItemCurrency;->code:Ljava/lang/String;

    invoke-virtual {v1, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    const/4 v1, 0x1

    xor-int/2addr p2, v1

    or-int/lit8 v2, p2, 0x2

    .line 581
    iget-object v3, p3, Lapp/ItemCurrency;->value:Ljava/lang/String;

    iget-object p3, p3, Lapp/ItemCurrency;->code:Ljava/lang/String;

    invoke-virtual {p0, v3, p3, v2}, Lapp/ItemAdapter;->formatCurrency(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;

    move-result-object p3

    if-eqz p2, :cond_0

    if-eqz p3, :cond_0

    .line 586
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p3, " "

    invoke-virtual {v1, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p3

    const/4 v1, 0x2

    :cond_0
    if-eqz p6, :cond_1

    if-eqz p7, :cond_1

    .line 594
    invoke-direct {p0, p4, p5}, Lapp/DefaultItemAdapter;->getConvertedPrice(Lapp/ItemCurrency;Lapp/ItemCurrency;)Lapp/ItemCurrency;

    move-result-object p5

    if-eqz p5, :cond_1

    .line 598
    iget-object p6, p5, Lapp/ItemCurrency;->code:Ljava/lang/String;

    iget-object p4, p4, Lapp/ItemCurrency;->code:Ljava/lang/String;

    invoke-virtual {p6, p4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p4

    .line 599
    iget-object p5, p5, Lapp/ItemCurrency;->code:Ljava/lang/String;

    invoke-direct {p0, p5, p3, p7, p4}, Lapp/DefaultItemAdapter;->getPrice(Ljava/lang/String;Ljava/lang/String;Lapp/SearchListItem;Z)Landroid/text/Spannable;

    move-result-object v0

    :cond_1
    if-nez v0, :cond_2

    .line 605
    sget-object p4, Landroid/graphics/Typeface;->DEFAULT:Landroid/graphics/Typeface;

    invoke-virtual {p1, p4, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V

    .line 606
    invoke-virtual {p1, p3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    goto :goto_0

    .line 609
    :cond_2
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    goto :goto_0

    .line 612
    :cond_3
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    const/4 p2, 0x0

    :goto_0
    return p2
.end method
