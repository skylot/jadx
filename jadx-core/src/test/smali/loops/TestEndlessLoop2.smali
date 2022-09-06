.class Lloops/TestEndlessLoop2;
.super Ljava/lang/Object;

.field instanceCount:J

.method test([Ljava/lang/String;)V
    .registers 10

    const/16 p1, 0xb
    invoke-virtual {p0, p1}, Lloops/TestEndlessLoop2;->vMeth(I)V
    const/16 v0, 0xf1
    const-wide/high16 v1, 0x4032000000000000L    # 18.0

    :goto_a
    const-wide/high16 v3, 0x4076000000000000L    # 352.0
    const/4 v5, 0x1
    cmpg-double v6, v1, v3
    if-gez v6, :cond_1c
    const/4 v0, 0x1

    :goto_12
    add-int/2addr v0, v5
    const/16 v3, 0x4b
    if-ge v0, v3, :cond_18
    goto :goto_12

    :cond_18
    const-wide/high16 v3, 0x3ff0000000000000L    # 1.0
    add-double/2addr v1, v3
    goto :goto_a

    :cond_1c
    iget-wide v3, p0, Lloops/TestEndlessLoop2;->instanceCount:J
    long-to-int v4, v3
    const/16 v3, 0xb

    :goto_21
    const/16 v6, 0xf3

    if-ge v5, v6, :cond_41
    rem-int/lit8 v6, v5, 0x9
    add-int/lit8 v6, v6, 0x12
    if-eq v6, p1, :cond_3e
    const/16 v7, 0x15
    if-eq v6, v7, :cond_36
    const/16 v7, 0x16
    if-eq v6, v7, :cond_34
    goto :goto_3b


    :cond_34
    add-int/2addr v4, v0
    goto :goto_3b

    :cond_36
    const v6, 0xeed9
    div-int/2addr v3, v6
    nop

    :goto_3b
    add-int/lit8 v5, v5, 0x1
    goto :goto_21

    :cond_3e
    nop

    :goto_3f
    nop
	# endless loop with empty body
    goto :goto_3f

    :cond_41
    sget-object p1, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-static {v1, v2}, Ljava/lang/Double;->doubleToLongBits(D)J
    move-result-wide v0
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const-string v5, "i21 d2 i22 = "
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v3, ","
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v0, v1}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-virtual {p1, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
