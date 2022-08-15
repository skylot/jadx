.class public Ltrycatch/TestNestedTryCatch4;
.super Landroid/app/NativeActivity;

.method private test(Landroid/content/Intent;)V
    .registers 11
    .annotation system Ldalvik/annotation/MethodParameters;
        accessFlags = {
            0x0
        }
        names = {
            "intent"
        }
    .end annotation

    const-string v0, "IOException while closing input stream\n"

    if-nez p1, :cond_5

    return-void

    :cond_5
    const-string v1, "intent_cmd"

    .line 1740
    invoke-virtual {p1, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "MCPE"

    if-eqz v1, :cond_80

    .line 1741
    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v3

    if-lez v3, :cond_80

    .line 1743
    :try_start_15
    new-instance p1, Lorg/json/JSONObject;

    invoke-direct {p1, v1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

    const-string v0, "Command"

    .line 1744
    invoke-virtual {p1, v0}, Lorg/json/JSONObject;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    const-string v1, "keyboardResult"

    .line 1745
    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_33

    const-string v0, "Text"

    .line 1746
    invoke-virtual {p1, v0}, Lorg/json/JSONObject;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p1}, Ltrycatch/TestNestedTryCatch4;->nativeSetTextboxText(Ljava/lang/String;)V

    goto/16 :goto_208

    :cond_33
    const-string v1, "fileDialogResult"

    .line 1748
    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_208

    iget-wide v0, p0, Ltrycatch/TestNestedTryCatch4;->mFileDialogCallback:J

    const-wide/16 v3, 0x0

    cmp-long v5, v0, v3

    if-eqz v5, :cond_208

    const-string v0, "Result"

    .line 1749
    invoke-virtual {p1, v0}, Lorg/json/JSONObject;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    const-string v1, "Ok"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_5d

    .line 1750
    iget-wide v0, p0, Ltrycatch/TestNestedTryCatch4;->mFileDialogCallback:J

    const-string v5, "Path"

    invoke-virtual {p1, v5}, Lorg/json/JSONObject;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, v0, v1, p1}, Ltrycatch/TestNestedTryCatch4;->nativeOnPickImageSuccess(JLjava/lang/String;)V

    goto :goto_62

    .line 1753
    :cond_5d
    iget-wide v0, p0, Ltrycatch/TestNestedTryCatch4;->mFileDialogCallback:J

    invoke-virtual {p0, v0, v1}, Ltrycatch/TestNestedTryCatch4;->nativeOnPickImageCanceled(J)V

    .line 1755
    :goto_62
    iput-wide v3, p0, Ltrycatch/TestNestedTryCatch4;->mFileDialogCallback:J
    :try_end_64
    .catch Lorg/json/JSONException; {:try_start_15 .. :try_end_64} :catch_66

    goto/16 :goto_208

    :catch_66
    move-exception p1

    .line 1759
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "JSONObject exception:"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Lorg/json/JSONException;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    return-void

    .line 1765
    :cond_80
    invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v1

    .line 1766
    invoke-virtual {p1}, Landroid/content/Intent;->getType()Ljava/lang/String;

    const-string/jumbo v3, "xbox_live_game_invite"

    .line 1768
    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_b0

    const-string/jumbo v0, "xbl"

    .line 1771
    invoke-virtual {p1, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    .line 1772
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "[XboxLive] Received Invite "

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 1776
    invoke-virtual {p0, v1, p1}, Ltrycatch/TestNestedTryCatch4;->nativeProcessIntentUriQuery(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_208

    :cond_b0
    const-string v3, "android.intent.action.VIEW"

    .line 1779
    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_c0

    const-string v3, "org.chromium.arc.intent.action.VIEW"

    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_208

    .line 1780
    :cond_c0
    invoke-virtual {p1}, Landroid/content/Intent;->getScheme()Ljava/lang/String;

    move-result-object v1

    .line 1781
    invoke-virtual {p1}, Landroid/content/Intent;->getData()Landroid/net/Uri;

    move-result-object p1

    if-nez p1, :cond_cb

    return-void

    :cond_cb
    const-string v3, "minecraft"

    .line 1787
    invoke-virtual {v3, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_1f9

    const-string v3, "minecraftedu"

    invoke-virtual {v3, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v3

    if-eqz v3, :cond_dd

    goto/16 :goto_1f9

    :cond_dd
    const-string v3, "file"

    .line 1795
    invoke-virtual {v3, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v3

    const-string v4, "&"

    if-eqz v3, :cond_108

    .line 1798
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Landroid/net/Uri;->getPath()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Landroid/net/Uri;->getPath()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "fileIntent"

    invoke-virtual {p0, v0, p1}, Ltrycatch/TestNestedTryCatch4;->nativeProcessIntentUriQuery(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_208

    :cond_108
    const-string v3, "content"

    .line 1800
    invoke-virtual {v3, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_208

    .line 1803
    new-instance v1, Ljava/io/File;

    invoke-virtual {p1}, Landroid/net/Uri;->getPath()Ljava/lang/String;

    move-result-object v3

    invoke-direct {v1, v3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v1

    .line 1804
    new-instance v3, Ljava/io/File;

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Ltrycatch/TestNestedTryCatch4;->getApplicationContext()Landroid/content/Context;

    move-result-object v6

    invoke-virtual {v6}, Landroid/content/Context;->getCacheDir()Ljava/io/File;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v6, "/"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {v3, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 1806
    invoke-virtual {p0}, Ltrycatch/TestNestedTryCatch4;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    .line 1810
    :try_start_142
    invoke-virtual {v1, p1}, Landroid/content/ContentResolver;->openInputStream(Landroid/net/Uri;)Ljava/io/InputStream;

    move-result-object v1
    :try_end_146
    .catch Ljava/io/IOException; {:try_start_142 .. :try_end_146} :catch_1df

    .line 1818
    :try_start_146
    new-instance v5, Ljava/io/FileOutputStream;

    invoke-direct {v5, v3}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V

    const/high16 v6, 0x100000

    new-array v6, v6, [B

    .line 1824
    :goto_14f
    invoke-virtual {v1, v6}, Ljava/io/InputStream;->read([B)I

    move-result v7

    const/4 v8, -0x1

    if-eq v7, v8, :cond_15b

    const/4 v8, 0x0

    .line 1825
    invoke-virtual {v5, v6, v8, v7}, Ljava/io/OutputStream;->write([BII)V

    goto :goto_14f

    .line 1828
    :cond_15b
    invoke-virtual {v5}, Ljava/io/OutputStream;->close()V

    const-string v5, "contentIntent"

    .line 1831
    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Landroid/net/Uri;->getPath()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v6, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v6, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, v5, p1}, Ltrycatch/TestNestedTryCatch4;->nativeProcessIntentUriQuery(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_17d
    .catch Ljava/io/IOException; {:try_start_146 .. :try_end_17d} :catch_18b
    .catchall {:try_start_146 .. :try_end_17d} :catchall_189

    .line 1842
    :try_start_17d
    invoke-virtual {v1}, Ljava/io/InputStream;->close()V
    :try_end_180
    .catch Ljava/io/IOException; {:try_start_17d .. :try_end_180} :catch_182

    goto/16 :goto_208

    :catch_182
    move-exception p1

    .line 1845
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    goto :goto_1b1

    :catchall_189
    move-exception p1

    goto :goto_1c3

    :catch_18b
    move-exception p1

    .line 1833
    :try_start_18c
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "IOException while copying file from content intent\n"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/io/IOException;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v4, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_1a4
    .catchall {:try_start_18c .. :try_end_1a4} :catchall_189

    .line 1837
    :try_start_1a4
    invoke-virtual {v3}, Ljava/io/File;->delete()Z
    :try_end_1a7
    .catch Ljava/lang/Exception; {:try_start_1a4 .. :try_end_1a7} :catch_1a7
    .catchall {:try_start_1a4 .. :try_end_1a7} :catchall_189

    .line 1842
    :catch_1a7
    :try_start_1a7
    invoke-virtual {v1}, Ljava/io/InputStream;->close()V
    :try_end_1aa
    .catch Ljava/io/IOException; {:try_start_1a7 .. :try_end_1aa} :catch_1ab

    goto :goto_208

    :catch_1ab
    move-exception p1

    .line 1845
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    :goto_1b1
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/io/IOException;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_208

    .line 1842
    :goto_1c3
    :try_start_1c3
    invoke-virtual {v1}, Ljava/io/InputStream;->close()V
    :try_end_1c6
    .catch Ljava/io/IOException; {:try_start_1c3 .. :try_end_1c6} :catch_1c7

    goto :goto_1de

    :catch_1c7
    move-exception v1

    .line 1845
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/io/IOException;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 1847
    :goto_1de
    throw p1

    :catch_1df
    move-exception p1

    .line 1813
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "IOException while opening file from content intent\n"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/io/IOException;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    return-void

    .line 1788
    :cond_1f9
    :goto_1f9
    invoke-virtual {p1}, Landroid/net/Uri;->getHost()Ljava/lang/String;

    move-result-object v0

    .line 1789
    invoke-virtual {p1}, Landroid/net/Uri;->getQuery()Ljava/lang/String;

    move-result-object p1

    if-nez v0, :cond_205

    if-eqz p1, :cond_208

    .line 1792
    :cond_205
    invoke-virtual {p0, v0, p1}, Ltrycatch/TestNestedTryCatch4;->nativeProcessIntentUriQuery(Ljava/lang/String;Ljava/lang/String;)V

    :cond_208
    :goto_208
    return-void
.end method
