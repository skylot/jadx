.class public Lconditions/TestIfCodeStyle2;
.super Ljava/lang/Object;

.method public execute(Ltest/TestSender;Ljava/lang/String;[Ljava/lang/String;)Z
    .registers 21
    .param p1, "sender"    # Ltest/TestSender;
    .param p2, "currentAlias"    # Ljava/lang/String;
    .param p3, "args"    # [Ljava/lang/String;
    .prologue
    .line 33
    invoke-virtual/range {p0 .. p1}, Lconditions/TestIfCodeStyle2;->testPermission(Ltest/TestSender;)Z
    move-result v4
    if-nez v4, :cond_8
    const/4 v4, 0x1
    .line 165
    :goto_7
    return v4
    .line 35
    :cond_8
    move-object/from16 v0, p3
    array-length v4, v0
    const/4 v5, 0x2
    if-ge v4, v5, :cond_32
    .line 36
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    sget-object v5, Ltest/ChatColor;->RED:Ltest/ChatColor;
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v5, "Usage: "
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    move-object/from16 v0, p0
    iget-object v5, v0, Lconditions/TestIfCodeStyle2;->usageMessage:Ljava/lang/String;
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 37
    const/4 v4, 0x0
    goto :goto_7
    .line 40
    :cond_32
    const/4 v4, 0x0
    aget-object v4, p3, v4
    const-string v5, "give"
    invoke-virtual {v4, v5}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v4
    if-nez v4, :cond_61
    .line 41
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    sget-object v5, Ltest/ChatColor;->RED:Ltest/ChatColor;
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v5, "Usage: "
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    move-object/from16 v0, p0
    iget-object v5, v0, Lconditions/TestIfCodeStyle2;->usageMessage:Ljava/lang/String;
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 42
    const/4 v4, 0x0
    goto :goto_7
    .line 45
    :cond_61
    const/4 v4, 0x1
    aget-object v16, p3, v4
    .line 46
    .local v16, "statisticString":Ljava/lang/String;
    const/4 v2, 0x0
    .line 48
    .local v2, "player":Ltest/entity/Player;
    move-object/from16 v0, p3
    array-length v4, v0
    const/4 v5, 0x2
    if-le v4, v5, :cond_7d
    .line 49
    const/4 v4, 0x1
    aget-object v4, p3, v4
    invoke-static {v4}, Ltest/Bukkit;->getPlayer(Ljava/lang/String;)Ltest/entity/Player;
    move-result-object v2
    .line 54
    :cond_72
    :goto_72
    if-nez v2, :cond_88
    .line 55
    const-string v4, "You must specify which player you wish to perform this action on."
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 56
    const/4 v4, 0x1
    goto :goto_7
    .line 50
    :cond_7d
    move-object/from16 v0, p1
    instance-of v4, v0, Ltest/entity/Player;
    if-eqz v4, :cond_72
    move-object/from16 v2, p1
    .line 51
    check-cast v2, Ltest/entity/Player;
    goto :goto_72
    .line 59
    :cond_88
    const-string v4, "*"
    move-object/from16 v0, v16
    invoke-virtual {v0, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v4
    if-eqz v4, :cond_d7
    .line 60
    invoke-static {}, Ltest/Achievement;->values()[Ltest/Achievement;
    move-result-object v5
    array-length v7, v5
    const/4 v4, 0x0
    :goto_98
    if-ge v4, v7, :cond_bf
    aget-object v13, v5, v4
    .line 61
    .local v13, "achievement":Ltest/Achievement;
    invoke-interface {v2, v13}, Ltest/entity/Player;->hasAchievement(Ltest/Achievement;)Z
    move-result v8
    if-eqz v8, :cond_a5
    .line 60
    :cond_a2
    :goto_a2
    add-int/lit8 v4, v4, 0x1
    goto :goto_98
    .line 64
    :cond_a5
    new-instance v1, Ltest/event/player/PlayerAchievementAwardedEvent;
    invoke-direct {v1, v2, v13}, Ltest/event/player/PlayerAchievementAwardedEvent;-><init>(Ltest/entity/Player;Ltest/Achievement;)V
    .line 65
    .local v1, "event":Ltest/event/player/PlayerAchievementAwardedEvent;
    invoke-static {}, Ltest/Bukkit;->getServer()Ltest/Server;
    move-result-object v8
    invoke-interface {v8}, Ltest/Server;->getPluginManager()Ltest/plugin/PluginManager;
    move-result-object v8
    invoke-interface {v8, v1}, Ltest/plugin/PluginManager;->callEvent(Ltest/event/Event;)V
    .line 66
    invoke-virtual {v1}, Ltest/event/player/PlayerAchievementAwardedEvent;->isCancelled()Z
    move-result v8
    if-nez v8, :cond_a2
    .line 67
    invoke-interface {v2, v13}, Ltest/entity/Player;->awardAchievement(Ltest/Achievement;)V
    goto :goto_a2
    .line 70
    .end local v1    # "event":Ltest/event/player/PlayerAchievementAwardedEvent;
    .end local v13    # "achievement":Ltest/Achievement;
    :cond_bf
    const-string v4, "Successfully given all achievements to %s"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-static {v0, v4}, Ltest/command/Command;->broadcastCommandMessage(Ltest/TestSender;Ljava/lang/String;)V
    .line 71
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 74
    :cond_d7
    invoke-static {}, Ltest/Bukkit;->getUnsafe()Ltest/UnsafeValues;
    move-result-object v4
    move-object/from16 v0, v16
    invoke-interface {v4, v0}, Ltest/UnsafeValues;->getAchievementFromInternalName(Ljava/lang/String;)Ltest/Achievement;
    move-result-object v13
    .line 75
    .restart local v13    # "achievement":Ltest/Achievement;
    invoke-static {}, Ltest/Bukkit;->getUnsafe()Ltest/UnsafeValues;
    move-result-object v4
    move-object/from16 v0, v16
    invoke-interface {v4, v0}, Ltest/UnsafeValues;->getStatisticFromInternalName(Ljava/lang/String;)Ltest/Statistic;
    move-result-object v3
    .line 77
    .local v3, "statistic":Ltest/Statistic;
    if-eqz v13, :cond_15d
    .line 78
    invoke-interface {v2, v13}, Ltest/entity/Player;->hasAchievement(Ltest/Achievement;)Z
    move-result v4
    if-eqz v4, :cond_10e
    .line 79
    const-string v4, "%s already has achievement %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    const/4 v7, 0x1
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 80
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 83
    :cond_10e
    new-instance v1, Ltest/event/player/PlayerAchievementAwardedEvent;
    invoke-direct {v1, v2, v13}, Ltest/event/player/PlayerAchievementAwardedEvent;-><init>(Ltest/entity/Player;Ltest/Achievement;)V
    .line 84
    .restart local v1    # "event":Ltest/event/player/PlayerAchievementAwardedEvent;
    invoke-static {}, Ltest/Bukkit;->getServer()Ltest/Server;
    move-result-object v4
    invoke-interface {v4}, Ltest/Server;->getPluginManager()Ltest/plugin/PluginManager;
    move-result-object v4
    invoke-interface {v4, v1}, Ltest/plugin/PluginManager;->callEvent(Ltest/event/Event;)V
    .line 85
    invoke-virtual {v1}, Ltest/event/player/PlayerAchievementAwardedEvent;->isCancelled()Z
    move-result v4
    if-eqz v4, :cond_13f
    .line 86
    const-string v4, "Unable to award %s the achievement %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    const/4 v7, 0x1
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 87
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 89
    :cond_13f
    invoke-interface {v2, v13}, Ltest/entity/Player;->awardAchievement(Ltest/Achievement;)V
    .line 91
    const-string v4, "Successfully given %s the stat %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    const/4 v7, 0x1
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-static {v0, v4}, Ltest/command/Command;->broadcastCommandMessage(Ltest/TestSender;Ljava/lang/String;)V
    .line 92
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 95
    .end local v1    # "event":Ltest/event/player/PlayerAchievementAwardedEvent;
    :cond_15d
    if-nez v3, :cond_173
    .line 96
    const-string v4, "Unknown achievement or statistic \'%s\'"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 97
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 100
    :cond_173
    invoke-virtual {v3}, Ltest/Statistic;->getType()Ltest/Statistic$Type;
    move-result-object v4
    sget-object v5, Ltest/Statistic$Type;->UNTYPED:Ltest/Statistic$Type;
    if-ne v4, v5, :cond_1d4
    .line 101
    new-instance v1, Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v4
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v5
    add-int/lit8 v5, v5, 0x1
    invoke-direct {v1, v2, v3, v4, v5}, Ltest/event/player/PlayerStatisticIncrementEvent;-><init>(Ltest/entity/Player;Ltest/Statistic;II)V
    .line 102
    .local v1, "event":Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-static {}, Ltest/Bukkit;->getServer()Ltest/Server;
    move-result-object v4
    invoke-interface {v4}, Ltest/Server;->getPluginManager()Ltest/plugin/PluginManager;
    move-result-object v4
    invoke-interface {v4, v1}, Ltest/plugin/PluginManager;->callEvent(Ltest/event/Event;)V
    .line 103
    invoke-virtual {v1}, Ltest/event/player/PlayerStatisticIncrementEvent;->isCancelled()Z
    move-result v4
    if-eqz v4, :cond_1b6
    .line 104
    const-string v4, "Unable to increment %s for %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    const/4 v7, 0x1
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 105
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 107
    :cond_1b6
    invoke-interface {v2, v3}, Ltest/entity/Player;->incrementStatistic(Ltest/Statistic;)V
    .line 108
    const-string v4, "Successfully given %s the stat %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    const/4 v7, 0x1
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-static {v0, v4}, Ltest/command/Command;->broadcastCommandMessage(Ltest/TestSender;Ljava/lang/String;)V
    .line 109
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 112
    .end local v1    # "event":Ltest/event/player/PlayerStatisticIncrementEvent;
    :cond_1d4
    invoke-virtual {v3}, Ltest/Statistic;->getType()Ltest/Statistic$Type;
    move-result-object v4
    sget-object v5, Ltest/Statistic$Type;->ENTITY:Ltest/Statistic$Type;
    if-ne v4, v5, :cond_274
    .line 113
    const-string v4, "."
    move-object/from16 v0, v16
    invoke-virtual {v0, v4}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v4
    add-int/lit8 v4, v4, 0x1
    move-object/from16 v0, v16
    invoke-virtual {v0, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v4
    invoke-static {v4}, Ltest/entity/EntityType;->fromName(Ljava/lang/String;)Ltest/entity/EntityType;
    move-result-object v6
    .line 115
    .local v6, "entityType":Ltest/entity/EntityType;
    if-nez v6, :cond_206
    .line 116
    const-string v4, "Unknown achievement or statistic \'%s\'"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 117
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 120
    :cond_206
    new-instance v1, Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v4
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v5
    add-int/lit8 v5, v5, 0x1
    invoke-direct/range {v1 .. v6}, Ltest/event/player/PlayerStatisticIncrementEvent;-><init>(Ltest/entity/Player;Ltest/Statistic;IILtest/entity/EntityType;)V
    .line 121
    .restart local v1    # "event":Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-static {}, Ltest/Bukkit;->getServer()Ltest/Server;
    move-result-object v4
    invoke-interface {v4}, Ltest/Server;->getPluginManager()Ltest/plugin/PluginManager;
    move-result-object v4
    invoke-interface {v4, v1}, Ltest/plugin/PluginManager;->callEvent(Ltest/event/Event;)V
    .line 122
    invoke-virtual {v1}, Ltest/event/player/PlayerStatisticIncrementEvent;->isCancelled()Z
    move-result v4
    if-eqz v4, :cond_241
    .line 123
    const-string v4, "Unable to increment %s for %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    const/4 v7, 0x1
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 124
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 128
    :cond_241
    :try_start_241
    invoke-interface {v2, v3, v6}, Ltest/entity/Player;->incrementStatistic(Ltest/Statistic;Ltest/entity/EntityType;)V
    :try_end_244
    .catch Ljava/lang/IllegalArgumentException; {:try_start_241 .. :try_end_244} :catch_25f
    .line 164
    .end local v6    # "entityType":Ltest/entity/EntityType;
    :goto_244
    const-string v4, "Successfully given %s the stat %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    const/4 v7, 0x1
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-static {v0, v4}, Ltest/command/Command;->broadcastCommandMessage(Ltest/TestSender;Ljava/lang/String;)V
    .line 165
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 129
    .restart local v6    # "entityType":Ltest/entity/EntityType;
    :catch_25f
    move-exception v14
    .line 130
    .local v14, "e":Ljava/lang/IllegalArgumentException;
    const-string v4, "Unknown achievement or statistic \'%s\'"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 131
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 136
    .end local v1    # "event":Ltest/event/player/PlayerStatisticIncrementEvent;
    .end local v6    # "entityType":Ltest/entity/EntityType;
    .end local v14    # "e":Ljava/lang/IllegalArgumentException;
    :cond_274
    :try_start_274
    const-string v4, "."
    move-object/from16 v0, v16
    invoke-virtual {v0, v4}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I
    move-result v4
    add-int/lit8 v4, v4, 0x1
    move-object/from16 v0, v16
    invoke-virtual {v0, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v9
    const/4 v10, 0x0
    const v11, 0x7fffffff
    const/4 v12, 0x1
    move-object/from16 v7, p0
    move-object/from16 v8, p1
    invoke-virtual/range {v7 .. v12}, Lconditions/TestIfCodeStyle2;->getInteger(Ltest/TestSender;Ljava/lang/String;IIZ)I
    :try_end_290
    .catch Ljava/lang/NumberFormatException; {:try_start_274 .. :try_end_290} :catch_2ab
    move-result v15
    .line 142
    .local v15, "id":I
    invoke-static {v15}, Ltest/Material;->getMaterial(I)Ltest/Material;
    move-result-object v12
    .line 144
    .local v12, "material":Ltest/Material;
    if-nez v12, :cond_2b8
    .line 145
    const-string v4, "Unknown achievement or statistic \'%s\'"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 146
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 137
    .end local v12    # "material":Ltest/Material;
    .end local v15    # "id":I
    :catch_2ab
    move-exception v14
    .line 138
    .local v14, "e":Ljava/lang/NumberFormatException;
    invoke-virtual {v14}, Ljava/lang/NumberFormatException;->getMessage()Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 139
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 149
    .end local v14    # "e":Ljava/lang/NumberFormatException;
    .restart local v12    # "material":Ltest/Material;
    .restart local v15    # "id":I
    :cond_2b8
    new-instance v1, Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v10
    invoke-interface {v2, v3}, Ltest/entity/Player;->getStatistic(Ltest/Statistic;)I
    move-result v4
    add-int/lit8 v11, v4, 0x1
    move-object v7, v1
    move-object v8, v2
    move-object v9, v3
    invoke-direct/range {v7 .. v12}, Ltest/event/player/PlayerStatisticIncrementEvent;-><init>(Ltest/entity/Player;Ltest/Statistic;IILtest/Material;)V
    .line 150
    .restart local v1    # "event":Ltest/event/player/PlayerStatisticIncrementEvent;
    invoke-static {}, Ltest/Bukkit;->getServer()Ltest/Server;
    move-result-object v4
    invoke-interface {v4}, Ltest/Server;->getPluginManager()Ltest/plugin/PluginManager;
    move-result-object v4
    invoke-interface {v4, v1}, Ltest/plugin/PluginManager;->callEvent(Ltest/event/Event;)V
    .line 151
    invoke-virtual {v1}, Ltest/event/player/PlayerStatisticIncrementEvent;->isCancelled()Z
    move-result v4
    if-eqz v4, :cond_2f6
    .line 152
    const-string v4, "Unable to increment %s for %s"
    const/4 v5, 0x2
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    const/4 v7, 0x1
    invoke-interface {v2}, Ltest/entity/Player;->getName()Ljava/lang/String;
    move-result-object v8
    aput-object v8, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 153
    const/4 v4, 0x1
    goto/16 :goto_7
    .line 157
    :cond_2f6
    :try_start_2f6
    invoke-interface {v2, v3, v12}, Ltest/entity/Player;->incrementStatistic(Ltest/Statistic;Ltest/Material;)V
    :try_end_2f9
    .catch Ljava/lang/IllegalArgumentException; {:try_start_2f6 .. :try_end_2f9} :catch_2fb
    goto/16 :goto_244
    .line 158
    :catch_2fb
    move-exception v14
    .line 159
    .local v14, "e":Ljava/lang/IllegalArgumentException;
    const-string v4, "Unknown achievement or statistic \'%s\'"
    const/4 v5, 0x1
    new-array v5, v5, [Ljava/lang/Object;
    const/4 v7, 0x0
    aput-object v16, v5, v7
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v4
    move-object/from16 v0, p1
    invoke-interface {v0, v4}, Ltest/TestSender;->sendMessage(Ljava/lang/String;)V
    .line 160
    const/4 v4, 0x1
    goto/16 :goto_7
.end method
