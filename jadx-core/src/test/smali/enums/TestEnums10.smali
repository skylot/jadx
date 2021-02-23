.class public final enum Lenums/TestEnums10;
.super Ljava/lang/Enum;
.source ""

.field public static final synthetic A02:[Lenums/TestEnums10;

.field public static final enum A03:Lenums/TestEnums10;
.field public static final enum A04:Lenums/TestEnums10;
.field public static final enum A05:Lenums/TestEnums10;
.field public static final enum A06:Lenums/TestEnums10;
.field public static final enum A07:Lenums/TestEnums10;
.field public static final enum A08:Lenums/TestEnums10;
.field public static final enum A09:Lenums/TestEnums10;
.field public static final enum A0A:Lenums/TestEnums10;
.field public static final enum A0B:Lenums/TestEnums10;
.field public static final enum A0C:Lenums/TestEnums10;

.field public final A00:I

.method public static constructor <clinit>()V
    .locals 33
    const/16 v28, 0x0
    const/16 v27, 0x1
    const-string v3, "CONNECT"
    new-instance v26, Lenums/TestEnums10;
    move-object/from16 v2, v26
    move/from16 v1, v28
    move/from16 v0, v27
    invoke-direct {v2, v3, v1, v0}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v26, Lenums/TestEnums10;->A04:Lenums/TestEnums10;
    const/4 v4, 0x2
    const-string v2, "CONNACK"
    new-instance v25, Lenums/TestEnums10;
    move-object/from16 v1, v25
    invoke-direct {v1, v2, v0, v4}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v25, Lenums/TestEnums10;->A03:Lenums/TestEnums10;
    const/4 v6, 0x3
    const-string v1, "PUBLISH"
    new-instance v24, Lenums/TestEnums10;
    move-object/from16 v0, v24
    invoke-direct {v0, v1, v4, v6}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v24, Lenums/TestEnums10;->A08:Lenums/TestEnums10;
    const/4 v7, 0x4
    const-string v1, "PUBACK"
    new-instance v23, Lenums/TestEnums10;
    move-object/from16 v0, v23
    invoke-direct {v0, v1, v6, v7}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v23, Lenums/TestEnums10;->A07:Lenums/TestEnums10;
    const/4 v8, 0x5
    const-string v1, "PUBREC"
    new-instance v22, Lenums/TestEnums10;
    move-object/from16 v0, v22
    invoke-direct {v0, v1, v7, v8}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    const/4 v9, 0x6
    const-string v1, "PUBREL"
    new-instance v21, Lenums/TestEnums10;
    move-object/from16 v0, v21
    invoke-direct {v0, v1, v8, v9}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    const/4 v10, 0x7
    const-string v1, "PUBCOMP"
    new-instance v20, Lenums/TestEnums10;
    move-object/from16 v0, v20
    invoke-direct {v0, v1, v9, v10}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    const/16 v11, 0x8
    const-string v1, "SUBSCRIBE"
    new-instance v19, Lenums/TestEnums10;
    move-object/from16 v0, v19
    invoke-direct {v0, v1, v10, v11}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v19, Lenums/TestEnums10;->A0A:Lenums/TestEnums10;
    const/16 v12, 0x9
    const-string v1, "SUBACK"
    new-instance v18, Lenums/TestEnums10;
    move-object/from16 v0, v18
    invoke-direct {v0, v1, v11, v12}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v18, Lenums/TestEnums10;->A09:Lenums/TestEnums10;
    const/16 v13, 0xa
    const-string v1, "UNSUBSCRIBE"
    new-instance v17, Lenums/TestEnums10;
    move-object/from16 v0, v17
    invoke-direct {v0, v1, v12, v13}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v17, Lenums/TestEnums10;->A0C:Lenums/TestEnums10;
    const/16 v14, 0xb
    const-string v0, "UNSUBACK"
    new-instance v5, Lenums/TestEnums10;
    invoke-direct {v5, v0, v13, v14}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v5, Lenums/TestEnums10;->A0B:Lenums/TestEnums10;
    const/16 v15, 0xc
    const-string v0, "PINGREQ"
    new-instance v3, Lenums/TestEnums10;
    invoke-direct {v3, v0, v14, v15}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v3, Lenums/TestEnums10;->A05:Lenums/TestEnums10;
    const/16 v2, 0xd
    const-string v0, "PINGRESP"
    new-instance v1, Lenums/TestEnums10;
    invoke-direct {v1, v0, v15, v2}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    sput-object v1, Lenums/TestEnums10;->A06:Lenums/TestEnums10;
    const/16 v15, 0xe
    const-string v0, "DISCONNECT"
    new-instance v16, Lenums/TestEnums10;
    move-object/from16 v29, v16
    move-object/from16 v30, v0
    move/from16 v31, v2
    move/from16 v32, v15
    invoke-direct/range {v29 .. v32}, Lenums/TestEnums10;-><init>(Ljava/lang/String;II)V
    new-array v15, v15, [Lenums/TestEnums10;
    aput-object v26, v15, v28
    aput-object v25, v15, v27
    aput-object v24, v15, v4
    aput-object v23, v15, v6
    aput-object v22, v15, v7
    aput-object v21, v15, v8
    aput-object v20, v15, v9
    aput-object v19, v15, v10
    aput-object v18, v15, v11
    aput-object v17, v15, v12
    aput-object v5, v15, v13
    aput-object v3, v15, v14
    const/16 v0, 0xc
    aput-object v1, v15, v0
    aput-object v16, v15, v2
    sput-object v15, Lenums/TestEnums10;->A02:[Lenums/TestEnums10;

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;II)V
    .locals 0
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V
    iput p3, p0, Lenums/TestEnums10;->A00:I
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnums10;
    .locals 1
    const-class v0, Lenums/TestEnums10;
    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    move-result-object v0
    check-cast v0, Lenums/TestEnums10;
    return-object v0
.end method

.method public static values()[Lenums/TestEnums10;
    .locals 1
    sget-object v0, Lenums/TestEnums10;->A02:[Lenums/TestEnums10;
    invoke-virtual {v0}, [Ljava/lang/Object;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Lenums/TestEnums10;
    return-object v0
.end method
