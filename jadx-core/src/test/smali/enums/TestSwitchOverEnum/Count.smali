.class public final enum Lenums/TestSwitchOverEnum$Count;
.super Ljava/lang/Enum;

.field private static final synthetic $VALUES:[Lenums/TestSwitchOverEnum$Count;
.field public static final enum ONE:Lenums/TestSwitchOverEnum$Count;
.field public static final enum THREE:Lenums/TestSwitchOverEnum$Count;
.field public static final enum TWO:Lenums/TestSwitchOverEnum$Count;

.method private static synthetic $values()[Lenums/TestSwitchOverEnum$Count;
    .registers 3
    const/4 v0, 0x3
    new-array v0, v0, [Lenums/TestSwitchOverEnum$Count;
    const/4 v1, 0x0
    sget-object v2, Lenums/TestSwitchOverEnum$Count;->ONE:Lenums/TestSwitchOverEnum$Count;
    aput-object v2, v0, v1
    const/4 v1, 0x1
    sget-object v2, Lenums/TestSwitchOverEnum$Count;->TWO:Lenums/TestSwitchOverEnum$Count;
    aput-object v2, v0, v1
    const/4 v1, 0x2
    sget-object v2, Lenums/TestSwitchOverEnum$Count;->THREE:Lenums/TestSwitchOverEnum$Count;
    aput-object v2, v0, v1
    return-object v0
.end method

.method static constructor <clinit>()V
    .registers 3
    new-instance v0, Lenums/TestSwitchOverEnum$Count;
    const-string v1, "ONE"
    const/4 v2, 0x0
    invoke-direct {v0, v1, v2}, Lenums/TestSwitchOverEnum$Count;-><init>(Ljava/lang/String;I)V
    sput-object v0, Lenums/TestSwitchOverEnum$Count;->ONE:Lenums/TestSwitchOverEnum$Count;
    new-instance v0, Lenums/TestSwitchOverEnum$Count;
    const-string v1, "TWO"
    const/4 v2, 0x1
    invoke-direct {v0, v1, v2}, Lenums/TestSwitchOverEnum$Count;-><init>(Ljava/lang/String;I)V
    sput-object v0, Lenums/TestSwitchOverEnum$Count;->TWO:Lenums/TestSwitchOverEnum$Count;
    new-instance v0, Lenums/TestSwitchOverEnum$Count;
    const-string v1, "THREE"
    const/4 v2, 0x2
    invoke-direct {v0, v1, v2}, Lenums/TestSwitchOverEnum$Count;-><init>(Ljava/lang/String;I)V
    sput-object v0, Lenums/TestSwitchOverEnum$Count;->THREE:Lenums/TestSwitchOverEnum$Count;
    invoke-static {}, Lenums/TestSwitchOverEnum$Count;->$values()[Lenums/TestSwitchOverEnum$Count;
    move-result-object v0
    sput-object v0, Lenums/TestSwitchOverEnum$Count;->$VALUES:[Lenums/TestSwitchOverEnum$Count;
    return-void
.end method

.method private constructor <init>(Ljava/lang/String;I)V
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestSwitchOverEnum$Count;
    .registers 2
    const-class v0, Lenums/TestSwitchOverEnum$Count;
    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    move-result-object v0
    check-cast v0, Lenums/TestSwitchOverEnum$Count;
    return-object v0
.end method

.method public static values()[Lenums/TestSwitchOverEnum$Count;
    .registers 1
    sget-object v0, Lenums/TestSwitchOverEnum$Count;->$VALUES:[Lenums/TestSwitchOverEnum$Count;
    invoke-virtual {v0}, [Lenums/TestSwitchOverEnum$Count;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Lenums/TestSwitchOverEnum$Count;
    return-object v0
.end method
