.class public Ltrycatch/TestNestedTryCatch5;
.super Ljava/lang/Object;

.method public test(Landroid/database/sqlite/SQLiteDatabase;)V
    .registers 13

    const/4 v0, 0x0
    invoke-static {p1, v0}, LX/7Yz;->A0I(Ljava/lang/Object;I)V
    iget-boolean v0, p0, LX/00y;->A00:Z
    if-nez v0, :cond_9e

    :try_start_8
    iget-object v8, p0, LX/00y;->A03:LX/0Ux;
    iget-object v0, p0, LX/00y;->A04:LX/0Km;
    invoke-static {p1, v0}, LX/00y;->A01(Landroid/database/sqlite/SQLiteDatabase;LX/0Km;)LX/0g9;
    move-result-object v10

    check-cast v8, LX/0AB;
    invoke-virtual {v8, v10}, LX/0AB;->A05(LX/0wU;)V
    iget-object v0, v8, LX/0AB;->A01:LX/0Z4;
    iget-object v9, v0, LX/0Z4;->A00:Landroidx/work/impl/WorkDatabase_Impl;
    iput-object v10, v9, LX/0Rt;->A0B:LX/0wU;
    const-string v0, "PRAGMA foreign_keys = ON"
    invoke-interface {v10, v0}, LX/0wU;->Auv(Ljava/lang/String;)V
    iget-object v1, v9, LX/0Rt;->A06:LX/0Uj;
    iget-object v2, v1, LX/0Uj;->A05:Ljava/lang/Object;

    monitor-enter v2
    :try_end_25
    .catchall {:try_start_8 .. :try_end_25} :catchall_95

    :try_start_25
    iget-boolean v0, v1, LX/0Uj;->A0D:Z

    if-eqz v0, :cond_31
    const-string v1, "ROOM"
    const-string v0, "Invalidation tracker is initialized twice :/."
    invoke-static {v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    goto :goto_4e

    :cond_31
    const-string v0, "PRAGMA temp_store = MEMORY;"
    invoke-interface {v10, v0}, LX/0wU;->Auv(Ljava/lang/String;)V
    const-string v0, "PRAGMA recursive_triggers=\'ON\';"
    invoke-interface {v10, v0}, LX/0wU;->Auv(Ljava/lang/String;)V
    const-string v0, "CREATE TEMP TABLE room_table_modification_log (table_id INTEGER PRIMARY KEY, invalidated INTEGER NOT NULL DEFAULT 0)"
    invoke-interface {v10, v0}, LX/0wU;->Auv(Ljava/lang/String;)V
    invoke-virtual {v1, v10}, LX/0Uj;->A00(LX/0wU;)V
    const-string v0, "UPDATE room_table_modification_log SET invalidated = 0 WHERE invalidated = 1"
    invoke-interface {v10, v0}, LX/0wU;->ArR(Ljava/lang/String;)LX/0wJ;
    move-result-object v0

    iput-object v0, v1, LX/0Uj;->A0C:LX/0wJ;
    const/4 v0, 0x1
    iput-boolean v0, v1, LX/0Uj;->A0D:Z
    :try_end_4e
    .catchall {:try_start_25 .. :try_end_4e} :catchall_92

    :goto_4e
    :try_start_4e
    monitor-exit v2

    iget-object v0, v9, LX/0Rt;->A01:Ljava/util/List;
    if-eqz v0, :cond_8e
    invoke-interface {v0}, Ljava/util/List;->size()I
    move-result v7

    const/4 v6, 0x0

    :goto_58
    if-ge v6, v7, :cond_8e

    iget-object v0, v9, LX/0Rt;->A01:Ljava/util/List;
    invoke-interface {v0, v6}, Ljava/util/List;->get(I)Ljava/lang/Object;
    iget-object v5, v10, LX/0g9;->A00:Landroid/database/sqlite/SQLiteDatabase;
    invoke-virtual {v5}, Landroid/database/sqlite/SQLiteDatabase;->beginTransaction()V
    :try_end_64
    .catchall {:try_start_4e .. :try_end_64} :catchall_95

    :try_start_64
    invoke-static {}, LX/001;->A0r()Ljava/lang/StringBuilder;
    move-result-object v4

    const-string v0, "DELETE FROM workspec WHERE state IN (2, 3, 5) AND (last_enqueue_time + minimum_retention_duration) < "
    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v2

    sget-wide v0, LX/0Jm;->A00:J
    sub-long/2addr v2, v0
    invoke-virtual {v4, v2, v3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;
    const-string v0, " AND (SELECT COUNT(*)=0 FROM dependency WHERE     prerequisite_id=id AND     work_spec_id NOT IN         (SELECT id FROM workspec WHERE state IN (2, 3, 5)))"
    invoke-static {v0, v4}, LX/000;->A0X(Ljava/lang/String;Ljava/lang/StringBuilder;)Ljava/lang/String;
    move-result-object v0

    invoke-interface {v10, v0}, LX/0wU;->Auv(Ljava/lang/String;)V
    invoke-virtual {v5}, Landroid/database/sqlite/SQLiteDatabase;->setTransactionSuccessful()V
    :try_end_83
    .catchall {:try_start_64 .. :try_end_83} :catchall_89

    :try_start_83
    invoke-virtual {v5}, Landroid/database/sqlite/SQLiteDatabase;->endTransaction()V
    add-int/lit8 v6, v6, 0x1
    goto :goto_58

    :catchall_89
    move-exception v0
    invoke-virtual {v5}, Landroid/database/sqlite/SQLiteDatabase;->endTransaction()V
    goto :goto_94

    :cond_8e
    const/4 v0, 0x0
    iput-object v0, v8, LX/0AB;->A00:LX/0N8;
    goto :goto_9e

    :catchall_92
    move-exception v0
    monitor-exit v2

    :goto_94
    throw v0
    :try_end_95
    .catchall {:try_start_83 .. :try_end_95} :catchall_95

    :catchall_95
    move-exception v2
    sget-object v1, LX/0Fu;->A04:LX/0Fu;
    new-instance v0, LX/0oe;
    invoke-direct {v0, v1, v2}, LX/0oe;-><init>(LX/0Fu;Ljava/lang/Throwable;)V
    throw v0

    :cond_9e
    :goto_9e
    const/4 v0, 0x1
    iput-boolean v0, p0, LX/00y;->A01:Z
    return-void
.end method
