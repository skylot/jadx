.class public Lloops/TestMultiEntryLoop3;
.super Ljava/lang/Object;

.method public static test(III)I
    .registers 4

    packed-switch p0, :pswitch_data_0
    goto :exit

    :pswitch_0
    :block_a
    if-eqz p1, :exit
    add-int/lit8 p1, p1, -1
    if-eqz p2, :block_b
    goto :block_c

    :pswitch_1
    :block_b
    if-eqz p2, :exit
    add-int/lit8 p2, p2, -1
    if-eqz p1, :block_c
    goto :block_a

    :pswitch_2
    :block_c
    add-int/2addr p1, p2
    if-lez p1, :exit
    goto :block_a

    :exit
    return p1

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
        :pswitch_1
        :pswitch_2
    .end packed-switch
.end method
