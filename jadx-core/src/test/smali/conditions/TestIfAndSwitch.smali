###### Class conditions.TestIfAndSwitch (conditions.TestIfAndSwitch)
.class Lconditions/TestIfAndSwitch;
.super Ljava/lang/Object;
.source "TestIfAndSwitch.java"


# static fields
.field private static final ACTION_MOVE:I = 0x2

.field private static final C:I

.field private static i:I

.field private static final rd:Ljava/util/Random;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 8
    new-instance v0, Ljava/util/Random;

    invoke-direct {v0}, Ljava/util/Random;-><init>()V

    sput-object v0, Lconditions/TestIfAndSwitch;->rd:Ljava/util/Random;

    return-void
.end method

.method constructor <init>()V
    .registers 1

    .line 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static ifAndSwitch()Z
    .registers 4

    .line 12
    nop

    .line 13
    sget-object v0, Lconditions/TestIfAndSwitch;->rd:Ljava/util/Random;

    invoke-virtual {v0}, Ljava/util/Random;->nextInt()I

    move-result v0

    const/4 v1, 0x2

    const/4 v2, 0x1

    const/4 v3, 0x0

    if-ne v0, v1, :cond_14

    .line 14
    sget v0, Lconditions/TestIfAndSwitch;->i:I

    packed-switch v0, :pswitch_data_1a

    goto :goto_14

    .line 16
    :pswitch_12
    const/4 v0, 0x1

    goto :goto_15

    .line 20
    :cond_14
    :goto_14
    const/4 v0, 0x0

    :goto_15
    if-eqz v0, :cond_18

    .line 21
    return v2

    .line 23
    :cond_18
    return v3

    nop

    :pswitch_data_1a
    .packed-switch 0x0
        :pswitch_12
    .end packed-switch
.end method
