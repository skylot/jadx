.class public Ltypes/TestTypeResolver14;
.super Ljava/lang/Object;
.source "SourceFile"

.method public test()Ljava/util/Date;
    .registers 5
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 472
    const/4 v2, 0x0
    const/4 v3, 0x0

    invoke-static {v3, v2}, Landroidx/room/util/DBUtil;->query(ZLandroid/os/CancellationSignal;)Landroid/database/Cursor;
    move-result-object v0

    .line 475
    :try_start_e
    invoke-interface {v0}, Landroid/database/Cursor;->moveToFirst()Z
    move-result v1

    if-eqz v1, :cond_2d

    .line 477
    invoke-interface {v0, v3}, Landroid/database/Cursor;->isNull(I)Z
    move-result v1

    if-eqz v1, :cond_1b

    goto :goto_23

    .line 480
    :cond_1b
    invoke-interface {v0, v3}, Landroid/database/Cursor;->getLong(I)J
    move-result-wide v1

    invoke-static {v1, v2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v2

    .line 482
    :goto_23
    iget-object v1, p0, Ltypes/TestTypeResolver14$8;->this$0:Ltypes/TestTypeResolver14;

    invoke-virtual {v1, v2}, Ltypeconverters/DateTypeConverter;->toDate(Ljava/lang/Long;)Ljava/util/Date;
    move-result-object v2

    :try_end_2d
    .catchall {:try_start_e .. :try_end_2d} :catchall_31

    .line 488
    :cond_2d
    invoke-interface {v0}, Landroid/database/Cursor;->close()V
    return-object v2

    :catchall_31
    move-exception v1
    invoke-interface {v0}, Landroid/database/Cursor;->close()V
    .line 489
    throw v1
.end method
