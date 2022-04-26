.class public Lconditions/TestIfCodeStyle;
.super Ljava/lang/Object;
.implements Landroid/os/Parcelable;


.field public isActive:Z
.field public isFactory:Z
.field public moduleName:Ljava/lang/String;
.field public modulePath:Ljava/lang/String;
.field public preinstalledModulePath:Ljava/lang/String;
.field public versionCode:J
.field public versionName:Ljava/lang/String;


.method public final readFromParcel(Landroid/os/Parcel;)V
    .registers 9
    .param p1, "_aidl_parcel"    # Landroid/os/Parcel;

    .line 44
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v0

    .line 45
    .local v0, "_aidl_start_pos":I
    invoke-virtual {p1}, Landroid/os/Parcel;->readInt()I
    move-result v1

    .line 47
    .local v1, "_aidl_parcelable_size":I
    const-string v2, "Overflow in the size of parcelable"
    const v3, 0x7fffffff
    if-gez v1, :cond_1e

    .line 63
    sub-int/2addr v3, v1

    if-gt v0, v3, :cond_18

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 47
    return-void

    .line 64
    :cond_18
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 48
    :cond_1e
    :try_start_1e
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I

    move-result v4
    :try_end_22
    .catchall {:try_start_1e .. :try_end_22} :catchall_fd

    sub-int/2addr v4, v0

    if-lt v4, v1, :cond_34

    .line 63
    sub-int/2addr v3, v1
    if-gt v0, v3, :cond_2e

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 48
    return-void

    .line 64
    :cond_2e
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 49
    :cond_34
    :try_start_34
    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;
    move-result-object v4

    iput-object v4, p0, Lconditions/TestIfCodeStyle;->moduleName:Ljava/lang/String;

    .line 50
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v4
    :try_end_3e
    .catchall {:try_start_34 .. :try_end_3e} :catchall_fd

    sub-int/2addr v4, v0

    if-lt v4, v1, :cond_50

    .line 63
    sub-int/2addr v3, v1

    if-gt v0, v3, :cond_4a

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 50
    return-void

    .line 64
    :cond_4a
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 51
    :cond_50
    :try_start_50
    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;
    move-result-object v4
    iput-object v4, p0, Lconditions/TestIfCodeStyle;->modulePath:Ljava/lang/String;

    .line 52
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v4

    :try_end_5a
    .catchall {:try_start_50 .. :try_end_5a} :catchall_fd

    sub-int/2addr v4, v0
    if-lt v4, v1, :cond_6c

    .line 63
    sub-int/2addr v3, v1
    if-gt v0, v3, :cond_66

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 52
    return-void

    .line 64
    :cond_66
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 53
    :cond_6c
    :try_start_6c
    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;
    move-result-object v4
    iput-object v4, p0, Lconditions/TestIfCodeStyle;->preinstalledModulePath:Ljava/lang/String;

    .line 54
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v4

    :try_end_76
    .catchall {:try_start_6c .. :try_end_76} :catchall_fd

    sub-int/2addr v4, v0
    if-lt v4, v1, :cond_88

    .line 63
    sub-int/2addr v3, v1
    if-gt v0, v3, :cond_82

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 54
    return-void

    .line 64
    :cond_82
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 55
    :cond_88
    :try_start_88
    invoke-virtual {p1}, Landroid/os/Parcel;->readLong()J
    move-result-wide v4
    iput-wide v4, p0, Lconditions/TestIfCodeStyle;->versionCode:J

    .line 56
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v4

    :try_end_92
    .catchall {:try_start_88 .. :try_end_92} :catchall_fd

    sub-int/2addr v4, v0
    if-lt v4, v1, :cond_a4

    .line 63
    sub-int/2addr v3, v1
    if-gt v0, v3, :cond_9e

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 56
    return-void

    .line 64
    :cond_9e
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 57
    :cond_a4
    :try_start_a4
    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;
    move-result-object v4
    iput-object v4, p0, Lconditions/TestIfCodeStyle;->versionName:Ljava/lang/String;

    .line 58
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I
    move-result v4
    :try_end_ae
    .catchall {:try_start_a4 .. :try_end_ae} :catchall_fd

    sub-int/2addr v4, v0
    if-lt v4, v1, :cond_c0

    .line 63
    sub-int/2addr v3, v1
    if-gt v0, v3, :cond_ba

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 58
    return-void

    .line 64
    :cond_ba
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 59
    :cond_c0
    :try_start_c0
    invoke-virtual {p1}, Landroid/os/Parcel;->readInt()I
    move-result v4
    const/4 v5, 0x1
    const/4 v6, 0x0
    if-eqz v4, :cond_ca
    move v4, v5
    goto :goto_cb

    :cond_ca
    move v4, v6

    :goto_cb
    iput-boolean v4, p0, Lconditions/TestIfCodeStyle;->isFactory:Z

    .line 60
    invoke-virtual {p1}, Landroid/os/Parcel;->dataPosition()I

    move-result v4
    :try_end_d1
    .catchall {:try_start_c0 .. :try_end_d1} :catchall_fd

    sub-int/2addr v4, v0
    if-lt v4, v1, :cond_e3

    .line 63
    sub-int/2addr v3, v1

    if-gt v0, v3, :cond_dd

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 60
    return-void

    .line 64
    :cond_dd
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 61
    :cond_e3
    :try_start_e3
    invoke-virtual {p1}, Landroid/os/Parcel;->readInt()I
    move-result v4

    if-eqz v4, :cond_ea
    goto :goto_eb

    :cond_ea
    move v5, v6

    :goto_eb
    iput-boolean v5, p0, Lconditions/TestIfCodeStyle;->isActive:Z
    :try_end_ed
    .catchall {:try_start_e3 .. :try_end_ed} :catchall_fd

    .line 63
    sub-int/2addr v3, v1

    if-gt v0, v3, :cond_f7

    .line 66
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 67
    nop

    .line 68
    return-void

    .line 64
    :cond_f7
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 63
    :catchall_fd
    move-exception v4
    sub-int/2addr v3, v1
    if-le v0, v3, :cond_107

    .line 64
    new-instance v3, Ljava/lang/RuntimeException;
    invoke-direct {v3, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V
    throw v3

    .line 66
    :cond_107
    add-int v2, v0, v1
    invoke-virtual {p1, v2}, Landroid/os/Parcel;->setDataPosition(I)V

    .line 67
    throw v4
.end method
