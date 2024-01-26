.class public Ltrycatch/TestUnreachableCatch;
.super Ljava/lang/Object;

.method private static prepareFontData(Landroid/content/Context;[Landroid/provider/FontsContract$FontInfo;Landroid/os/CancellationSignal;)Ljava/util/Map;
    .locals 18
    .param p0, "context"    # Landroid/content/Context;
    .param p1, "fonts"    # [Landroid/provider/FontsContract$FontInfo;
    .param p2, "cancellationSignal"    # Landroid/os/CancellationSignal;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/content/Context;",
            "[",
            "Landroid/provider/FontsContract$FontInfo;",
            "Landroid/os/CancellationSignal;",
            ")",
            "Ljava/util/Map<",
            "Landroid/net/Uri;",
            "Ljava/nio/ByteBuffer;",
            ">;"
        }
    .end annotation

    .line 728
    move-object/from16 v1, p1

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    move-object v2, v0

    .line 729
    .local v2, "out":Ljava/util/HashMap;, "Ljava/util/HashMap<Landroid/net/Uri;Ljava/nio/ByteBuffer;>;"
    invoke-virtual/range {p0 .. p0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v3

    .line 731
    .local v3, "resolver":Landroid/content/ContentResolver;
    array-length v4, v1

    const/4 v0, 0x0

    move v5, v0

    :goto_0
    if-ge v5, v4, :cond_5

    aget-object v6, v1, v5

    .line 732
    .local v6, "font":Landroid/provider/FontsContract$FontInfo;
    invoke-virtual {v6}, Landroid/provider/FontsContract$FontInfo;->getResultCode()I

    move-result v0

    if-eqz v0, :cond_0

    .line 733
    move-object/from16 v9, p2

    goto/16 :goto_5

    .line 736
    :cond_0
    invoke-virtual {v6}, Landroid/provider/FontsContract$FontInfo;->getUri()Landroid/net/Uri;

    move-result-object v7

    .line 737
    .local v7, "uri":Landroid/net/Uri;
    invoke-virtual {v2, v7}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1

    .line 738
    move-object/from16 v9, p2

    goto :goto_5

    .line 741
    :cond_1
    const/4 v8, 0x0

    .line 742
    .local v8, "buffer":Ljava/nio/ByteBuffer;
    :try_start_0
    const-string/jumbo v0, "r"
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_2

    .line 743
    move-object/from16 v9, p2

    :try_start_1
    invoke-virtual {v3, v7, v0, v9}, Landroid/content/ContentResolver;->openFileDescriptor(Landroid/net/Uri;Ljava/lang/String;Landroid/os/CancellationSignal;)Landroid/os/ParcelFileDescriptor;

    move-result-object v0
    :try_end_1
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_1

    move-object v10, v0

    .line 744
    .local v10, "pfd":Landroid/os/ParcelFileDescriptor;
    if-eqz v10, :cond_3

    .line 745
    :try_start_2
    new-instance v0, Ljava/io/FileInputStream;

    .line 746
    invoke-virtual {v10}, Landroid/os/ParcelFileDescriptor;->getFileDescriptor()Ljava/io/FileDescriptor;

    move-result-object v11

    invoke-direct {v0, v11}, Ljava/io/FileInputStream;-><init>(Ljava/io/FileDescriptor;)V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    move-object v11, v0

    .line 747
    .local v11, "fis":Ljava/io/FileInputStream;
    :try_start_3
    invoke-virtual {v11}, Ljava/io/FileInputStream;->getChannel()Ljava/nio/channels/FileChannel;

    move-result-object v12

    .line 748
    .local v12, "fileChannel":Ljava/nio/channels/FileChannel;
    invoke-virtual {v12}, Ljava/nio/channels/FileChannel;->size()J

    move-result-wide v16

    .line 749
    .local v16, "size":J
    sget-object v13, Ljava/nio/channels/FileChannel$MapMode;->READ_ONLY:Ljava/nio/channels/FileChannel$MapMode;

    const-wide/16 v14, 0x0

    invoke-virtual/range {v12 .. v17}, Ljava/nio/channels/FileChannel;->map(Ljava/nio/channels/FileChannel$MapMode;JJ)Ljava/nio/MappedByteBuffer;

    move-result-object v0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    move-object v8, v0

    .line 750
    .end local v12    # "fileChannel":Ljava/nio/channels/FileChannel;
    .end local v16    # "size":J
    :try_start_4
    invoke-virtual {v11}, Ljava/io/FileInputStream;->close()V
    :try_end_4
    .catch Ljava/io/IOException; {:try_start_4 .. :try_end_4} :catch_0
    .catchall {:try_start_4 .. :try_end_4} :catchall_2

    .line 752
    .end local v11    # "fis":Ljava/io/FileInputStream;
    goto :goto_3

    .line 745
    .restart local v11    # "fis":Ljava/io/FileInputStream;
    :catchall_0
    move-exception v0

    move-object v12, v0

    :try_start_5
    invoke-virtual {v11}, Ljava/io/FileInputStream;->close()V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_1

    goto :goto_1

    :catchall_1
    move-exception v0

    move-object v13, v0

    :try_start_6
    invoke-virtual {v12, v13}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    .end local v2    # "out":Ljava/util/HashMap;, "Ljava/util/HashMap<Landroid/net/Uri;Ljava/nio/ByteBuffer;>;"
    .end local v3    # "resolver":Landroid/content/ContentResolver;
    .end local v6    # "font":Landroid/provider/FontsContract$FontInfo;
    .end local v7    # "uri":Landroid/net/Uri;
    .end local v8    # "buffer":Ljava/nio/ByteBuffer;
    .end local v10    # "pfd":Landroid/os/ParcelFileDescriptor;
    .end local p0    # "context":Landroid/content/Context;
    .end local p1    # "fonts":[Landroid/provider/FontsContract$FontInfo;
    .end local p2    # "cancellationSignal":Landroid/os/CancellationSignal;
    :goto_1
    throw v12
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_0
    .catchall {:try_start_6 .. :try_end_6} :catchall_2

    .line 742
    .end local v11    # "fis":Ljava/io/FileInputStream;
    .restart local v2    # "out":Ljava/util/HashMap;, "Ljava/util/HashMap<Landroid/net/Uri;Ljava/nio/ByteBuffer;>;"
    .restart local v3    # "resolver":Landroid/content/ContentResolver;
    .restart local v6    # "font":Landroid/provider/FontsContract$FontInfo;
    .restart local v7    # "uri":Landroid/net/Uri;
    .restart local v8    # "buffer":Ljava/nio/ByteBuffer;
    .restart local v10    # "pfd":Landroid/os/ParcelFileDescriptor;
    .restart local p0    # "context":Landroid/content/Context;
    .restart local p1    # "fonts":[Landroid/provider/FontsContract$FontInfo;
    .restart local p2    # "cancellationSignal":Landroid/os/CancellationSignal;
    :catchall_2
    move-exception v0

    move-object v11, v0

    if-eqz v10, :cond_2

    :try_start_7
    invoke-virtual {v10}, Landroid/os/ParcelFileDescriptor;->close()V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_3

    goto :goto_2

    :catchall_3
    move-exception v0

    move-object v12, v0

    :try_start_8
    invoke-virtual {v11, v12}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    .end local v2    # "out":Ljava/util/HashMap;, "Ljava/util/HashMap<Landroid/net/Uri;Ljava/nio/ByteBuffer;>;"
    .end local v3    # "resolver":Landroid/content/ContentResolver;
    .end local v6    # "font":Landroid/provider/FontsContract$FontInfo;
    .end local v7    # "uri":Landroid/net/Uri;
    .end local v8    # "buffer":Ljava/nio/ByteBuffer;
    .end local p0    # "context":Landroid/content/Context;
    .end local p1    # "fonts":[Landroid/provider/FontsContract$FontInfo;
    .end local p2    # "cancellationSignal":Landroid/os/CancellationSignal;
    :cond_2
    :goto_2
    throw v11

    .line 750
    .restart local v2    # "out":Ljava/util/HashMap;, "Ljava/util/HashMap<Landroid/net/Uri;Ljava/nio/ByteBuffer;>;"
    .restart local v3    # "resolver":Landroid/content/ContentResolver;
    .restart local v6    # "font":Landroid/provider/FontsContract$FontInfo;
    .restart local v7    # "uri":Landroid/net/Uri;
    .restart local v8    # "buffer":Ljava/nio/ByteBuffer;
    .restart local p0    # "context":Landroid/content/Context;
    .restart local p1    # "fonts":[Landroid/provider/FontsContract$FontInfo;
    .restart local p2    # "cancellationSignal":Landroid/os/CancellationSignal;
    :catch_0
    move-exception v0

    .line 754
    :cond_3
    :goto_3
    if-eqz v10, :cond_4

    invoke-virtual {v10}, Landroid/os/ParcelFileDescriptor;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_1

    .line 756
    .end local v10    # "pfd":Landroid/os/ParcelFileDescriptor;
    :cond_4
    goto :goto_4

    .line 754
    :catch_1
    move-exception v0

    goto :goto_4

    :catch_2
    move-exception v0

    move-object/from16 v9, p2

    .line 760
    :goto_4
    invoke-virtual {v2, v7, v8}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 731
    .end local v6    # "font":Landroid/provider/FontsContract$FontInfo;
    .end local v7    # "uri":Landroid/net/Uri;
    .end local v8    # "buffer":Ljava/nio/ByteBuffer;
    :goto_5
    add-int/lit8 v5, v5, 0x1

    goto :goto_0

    .line 762
    :cond_5
    move-object/from16 v9, p2

    invoke-static {v2}, Ljava/util/Collections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;

    move-result-object v0

    return-object v0
.end method
