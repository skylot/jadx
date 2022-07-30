.class public Ltrycatch/TestTryCatchFinally15;
.super Ljava/lang/Object;

.implements Landroid/os/IInterface;

.field private final zza:Landroid/os/IBinder;
.field private final zzb:Ljava/lang/String;

.method protected final test(ILandroid/os/Parcel;)Landroid/os/Parcel;
    .registers 6
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation

    .line 1
    invoke-static {}, Landroid/os/Parcel;->obtain()Landroid/os/Parcel;
    move-result-object v0

    :try_start_4
    iget-object v1, p0, Ltrycatch/TestTryCatchFinally15;->zza:Landroid/os/IBinder;
    const/4 v2, 0x0

    .line 2
    invoke-interface {v1, p1, p2, v0, v2}, Landroid/os/IBinder;->transact(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z
    .line 3
    invoke-virtual {v0}, Landroid/os/Parcel;->readException()V
    :try_end_d
    .catch Ljava/lang/RuntimeException; {:try_start_4 .. :try_end_d} :catch_13
    .catchall {:try_start_4 .. :try_end_d} :catchall_11

    .line 6
    invoke-virtual {p2}, Landroid/os/Parcel;->recycle()V
    return-object v0

    :catchall_11
    move-exception p1
    goto :goto_18

    .line 5
    :catch_13
    move-exception p1

    .line 4
    :try_start_14
    invoke-virtual {v0}, Landroid/os/Parcel;->recycle()V

    .line 5
    throw p1
    :try_end_18
    .catchall {:try_start_14 .. :try_end_18} :catchall_11

    .line 6
    :goto_18
    invoke-virtual {p2}, Landroid/os/Parcel;->recycle()V

    .line 7
    throw p1
.end method
