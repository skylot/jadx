.class public final enum Lenums/TestEnumUsesOtherEnum;
.super Ljava/lang/Enum;
.source ""

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/TestEnumUsesOtherEnum;",
        ">;"
    }
.end annotation


.field public static final enum A:Lenums/TestEnumUsesOtherEnum;
.field private static final synthetic B:[Lenums/TestEnumUsesOtherEnum;
.field public static final enum i:Lenums/TestEnumUsesOtherEnum;
.field public static final enum j:Lenums/TestEnumUsesOtherEnum;
.field public static final enum k:Lenums/TestEnumUsesOtherEnum;
.field public static final enum l:Lenums/TestEnumUsesOtherEnum;
.field public static final enum m:Lenums/TestEnumUsesOtherEnum;
.field public static final enum n:Lenums/TestEnumUsesOtherEnum;
.field public static final enum o:Lenums/TestEnumUsesOtherEnum;
.field public static final enum p:Lenums/TestEnumUsesOtherEnum;
.field public static final enum q:Lenums/TestEnumUsesOtherEnum;
.field public static final enum r:Lenums/TestEnumUsesOtherEnum;
.field public static final enum s:Lenums/TestEnumUsesOtherEnum;
.field public static final enum t:Lenums/TestEnumUsesOtherEnum;
.field public static final enum u:Lenums/TestEnumUsesOtherEnum;
.field public static final enum v:Lenums/TestEnumUsesOtherEnum;
.field public static final enum w:Lenums/TestEnumUsesOtherEnum;
.field public static final enum x:Lenums/TestEnumUsesOtherEnum;
.field public static final enum y:Lenums/TestEnumUsesOtherEnum;
.field public static final enum z:Lenums/TestEnumUsesOtherEnum;


# instance fields
.field public c:Ljava/lang/String;
.field public d:Ljava/lang/String;
.field public e:Ljava/lang/Integer;
.field public f:Z
.field public g:Z
.field public h:[Ljava/lang/String;


.method static constructor <clinit>()V
    .registers 53

    new-instance v9, Lenums/TestEnumUsesOtherEnum;

    const/4 v10, 0x1

    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v11

    const/16 v12, 0xc

    new-array v8, v12, [Ljava/lang/String;

    const-string v0, "VARCHAR"

    const/4 v13, 0x0

    invoke-static {v13}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v23

    aput-object v0, v8, v13

    const-string v0, "VARCHAR2"

    aput-object v0, v8, v10

    const-string v0, "TEXT"

    const/4 v15, 0x2

    aput-object v0, v8, v15

    const-string v0, "STRING"

    const/4 v14, 0x3

    aput-object v0, v8, v14

    const-string v0, "VARBINARY"

    const/16 v24, 0x4

    aput-object v0, v8, v24

    const-string v0, "TINYTEXT"

    const/4 v7, 0x5

    aput-object v0, v8, v7

    const-string v0, "MEDIUMTEXT"

    const/16 v25, 0x6

    aput-object v0, v8, v25

    const-string v0, "LONGTEXT"

    const/4 v6, 0x7

    aput-object v0, v8, v6

    const-string v0, "NVARCHAR"

    const/16 v5, 0x8

    aput-object v0, v8, v5

    const-string v0, "NTEXT"

    const/16 v26, 0x9

    aput-object v0, v8, v26

    const-string v0, "MEMO"

    const/16 v27, 0xa

    aput-object v0, v8, v27

    const-string v0, "HYPERLINK"

    const/16 v28, 0xb

    aput-object v0, v8, v28

    const-string v1, "VARCHAR"

    const/4 v2, 0x0

    const-string v3, "VARCHAR"

    const-string v4, "100"

    const/16 v16, 0x0

    const/16 v17, 0x1

    move-object v0, v9

    const/16 v12, 0x8

    move-object v5, v11

    const/4 v12, 0x7

    move/from16 v6, v16

    move/from16 v7, v17

    invoke-direct/range {v0 .. v8}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v9, Lenums/TestEnumUsesOtherEnum;->i:Lenums/TestEnumUsesOtherEnum;

    new-instance v30, Lenums/TestEnumUsesOtherEnum;

    new-array v8, v15, [Ljava/lang/String;

    const-string v0, "CHAR"

    aput-object v0, v8, v13

    const-string v0, "NCHAR"

    aput-object v0, v8, v10

    const-string v1, "CHAR"

    const/4 v2, 0x1

    const-string v3, "CHAR"

    const-string v4, "1"

    const/4 v6, 0x0

    const/4 v7, 0x1

    move-object/from16 v0, v30

    invoke-direct/range {v0 .. v8}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v30, Lenums/TestEnumUsesOtherEnum;->j:Lenums/TestEnumUsesOtherEnum;

    new-instance v31, Lenums/TestEnumUsesOtherEnum;

    new-array v8, v12, [Ljava/lang/String;

    const-string v0, "INT"

    aput-object v0, v8, v13

    const-string v0, "INTEGER"

    aput-object v0, v8, v10

    const-string v0, "NUMBER"

    aput-object v0, v8, v15

    const-string v0, "SERIAL"

    aput-object v0, v8, v14

    const-string v0, "YEAR"

    aput-object v0, v8, v24

    const-string v0, "BIT"

    const/4 v7, 0x5

    aput-object v0, v8, v7

    const-string v0, "AUTONUMBER"

    aput-object v0, v8, v25

    const-string v1, "INT"

    const/4 v2, 0x2

    const-string v3, "INT"

    const-string v4, "10"

    const/4 v6, 0x1

    move-object/from16 v0, v31

    const/4 v11, 0x5

    move/from16 v7, v16

    invoke-direct/range {v0 .. v8}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v31, Lenums/TestEnumUsesOtherEnum;->k:Lenums/TestEnumUsesOtherEnum;

    new-instance v0, Lenums/TestEnumUsesOtherEnum;

    new-array v1, v10, [Ljava/lang/String;

    const-string v2, "TINYINT"

    aput-object v2, v1, v13

    const-string v17, "TINYINT"

    const/16 v18, 0x3

    const-string v19, "TINYINT"

    move-object/from16 v16, v0

    move-object/from16 v20, v31

    move-object/from16 v21, v1

    invoke-direct/range {v16 .. v21}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v0, Lenums/TestEnumUsesOtherEnum;->l:Lenums/TestEnumUsesOtherEnum;

    new-instance v1, Lenums/TestEnumUsesOtherEnum;

    new-array v2, v15, [Ljava/lang/String;

    const-string v3, "SMALLINT"

    aput-object v3, v2, v13

    const-string v3, "SMALLSERIAL"

    aput-object v3, v2, v10

    const-string v17, "SMALLINT"

    const/16 v18, 0x4

    const-string v19, "SMALLINT"

    move-object/from16 v16, v1

    move-object/from16 v21, v2

    invoke-direct/range {v16 .. v21}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v1, Lenums/TestEnumUsesOtherEnum;->m:Lenums/TestEnumUsesOtherEnum;

    new-instance v2, Lenums/TestEnumUsesOtherEnum;

    new-array v3, v10, [Ljava/lang/String;

    const-string v4, "MEDIUMINT"

    aput-object v4, v3, v13

    const-string v17, "MEDIUMINT"

    const/16 v18, 0x5

    const-string v19, "MEDIUMINT"

    move-object/from16 v16, v2

    move-object/from16 v21, v3

    invoke-direct/range {v16 .. v21}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v2, Lenums/TestEnumUsesOtherEnum;->n:Lenums/TestEnumUsesOtherEnum;

    new-instance v3, Lenums/TestEnumUsesOtherEnum;

    new-array v4, v15, [Ljava/lang/String;

    const-string v5, "BIGINT"

    aput-object v5, v4, v13

    const-string v5, "BIGSERIAL"

    aput-object v5, v4, v10

    const-string v17, "BIGINT"

    const/16 v18, 0x6

    const-string v19, "BIGINT"

    move-object/from16 v16, v3

    move-object/from16 v21, v4

    invoke-direct/range {v16 .. v21}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v3, Lenums/TestEnumUsesOtherEnum;->o:Lenums/TestEnumUsesOtherEnum;

    new-instance v4, Lenums/TestEnumUsesOtherEnum;

    new-array v5, v14, [Ljava/lang/String;

    const-string v6, "BOOLEAN"

    aput-object v6, v5, v13

    const-string v6, "BOOL"

    aput-object v6, v5, v10

    const-string v6, "YES/NO"

    aput-object v6, v5, v15

    const-string v6, "BOOLEAN"

    const/16 v16, 0x7

    const-string v17, "BOOLEAN"

    const/16 v18, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x0

    const/4 v7, 0x3

    move-object v14, v4

    const/4 v8, 0x2

    move-object v15, v6

    move-object/from16 v19, v23

    move-object/from16 v22, v5

    invoke-direct/range {v14 .. v22}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v4, Lenums/TestEnumUsesOtherEnum;->p:Lenums/TestEnumUsesOtherEnum;

    new-instance v5, Lenums/TestEnumUsesOtherEnum;

    new-array v6, v12, [Ljava/lang/String;

    const-string v14, "BLOB"

    aput-object v14, v6, v13

    const-string v14, "BYTEA"

    aput-object v14, v6, v10

    const-string v14, "BINARY"

    aput-object v14, v6, v8

    const-string v14, "TINYBLOB"

    aput-object v14, v6, v7

    const-string v14, "MEDIUMBLOB"

    aput-object v14, v6, v24

    const-string v14, "LONGBLOB"

    aput-object v14, v6, v11

    const-string v14, "IMAGE"

    aput-object v14, v6, v25

    const-string v15, "BLOB"

    const/16 v16, 0x8

    const-string v17, "BLOB"

    move-object v14, v5

    move-object/from16 v22, v6

    invoke-direct/range {v14 .. v22}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v5, Lenums/TestEnumUsesOtherEnum;->q:Lenums/TestEnumUsesOtherEnum;

    new-instance v6, Lenums/TestEnumUsesOtherEnum;

    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v37

    const/16 v14, 0x8

    new-array v15, v14, [Ljava/lang/String;

    const-string v14, "FLOAT"

    aput-object v14, v15, v13

    const-string v14, "REAL"

    aput-object v14, v15, v10

    const-string v14, "DEC"

    aput-object v14, v15, v8

    const-string v14, "DECIMAL"

    aput-object v14, v15, v7

    const-string v14, "MONEY"

    aput-object v14, v15, v24

    const-string v14, "SMALLMONEY"

    aput-object v14, v15, v11

    const-string v14, "SINGLE"

    aput-object v14, v15, v25

    const-string v14, "CURRENCY"

    aput-object v14, v15, v12

    const-string v33, "FLOAT"

    const/16 v34, 0x9

    const-string v35, "FLOAT"

    const-string v36, "10, 5"

    const/16 v38, 0x1

    const/16 v39, 0x0

    move-object/from16 v32, v6

    move-object/from16 v40, v15

    invoke-direct/range {v32 .. v40}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v6, Lenums/TestEnumUsesOtherEnum;->r:Lenums/TestEnumUsesOtherEnum;

    new-instance v29, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v8, [Ljava/lang/String;

    const-string v15, "DOUBLE"

    aput-object v15, v14, v13

    const-string v15, "DOUBLE PRECISION"

    aput-object v15, v14, v10

    const-string v33, "DOUBLE"

    const/16 v34, 0xa

    const-string v35, "DOUBLE"

    move-object/from16 v32, v29

    move-object/from16 v36, v6

    move-object/from16 v37, v14

    invoke-direct/range {v32 .. v37}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v29, Lenums/TestEnumUsesOtherEnum;->s:Lenums/TestEnumUsesOtherEnum;

    new-instance v32, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v10, [Ljava/lang/String;

    const-string v15, "SET"

    aput-object v15, v14, v13

    const-string v37, "SET"

    const/16 v38, 0xb

    const-string v39, "SET"

    const-string v40, "a, b, c"

    const/16 v41, 0x0

    const/16 v42, 0x0

    const/16 v43, 0x0

    move-object/from16 v36, v32

    move-object/from16 v44, v14

    invoke-direct/range {v36 .. v44}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v32, Lenums/TestEnumUsesOtherEnum;->t:Lenums/TestEnumUsesOtherEnum;

    new-instance v33, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v10, [Ljava/lang/String;

    const-string v15, "ENUM"

    aput-object v15, v14, v13

    const-string v45, "ENUM"

    const/16 v46, 0xc

    const-string v47, "ENUM"

    const-string v48, "a, b, c"

    const/16 v49, 0x0

    const/16 v50, 0x0

    const/16 v51, 0x0

    move-object/from16 v44, v33

    move-object/from16 v52, v14

    invoke-direct/range {v44 .. v52}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v33, Lenums/TestEnumUsesOtherEnum;->u:Lenums/TestEnumUsesOtherEnum;

    new-instance v34, Lenums/TestEnumUsesOtherEnum;

    new-array v15, v10, [Ljava/lang/String;

    const-string v14, "DATE"

    aput-object v14, v15, v13

    const-string v16, "DATE"

    const/16 v17, 0xd

    const-string v18, "DATE"

    const-string v19, ""

    move-object/from16 v14, v34

    move-object/from16 v22, v15

    move-object/from16 v15, v16

    move/from16 v16, v17

    move-object/from16 v17, v18

    move-object/from16 v18, v19

    move-object/from16 v19, v23

    invoke-direct/range {v14 .. v22}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v34, Lenums/TestEnumUsesOtherEnum;->v:Lenums/TestEnumUsesOtherEnum;

    new-instance v35, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v10, [Ljava/lang/String;

    const-string v15, "TIME"

    aput-object v15, v14, v13

    const-string v16, "TIME"

    const/16 v17, 0xe

    const-string v18, "TIME"

    move-object/from16 v15, v35

    move-object/from16 v19, v34

    move-object/from16 v20, v14

    invoke-direct/range {v15 .. v20}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v35, Lenums/TestEnumUsesOtherEnum;->w:Lenums/TestEnumUsesOtherEnum;

    new-instance v36, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v11, [Ljava/lang/String;

    const-string v15, "DATETIME"

    aput-object v15, v14, v13

    const-string v15, "DATETIME2"

    aput-object v15, v14, v10

    const-string v15, "SMALLDATETIME"

    aput-object v15, v14, v8

    const-string v15, "DATETIMEOFFSET"

    aput-object v15, v14, v7

    const-string v15, "DATE/TIME"

    aput-object v15, v14, v24

    const-string v16, "DATETIME"

    const/16 v17, 0xf

    const-string v18, "DATETIME"

    move-object/from16 v15, v36

    move-object/from16 v20, v14

    invoke-direct/range {v15 .. v20}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v36, Lenums/TestEnumUsesOtherEnum;->x:Lenums/TestEnumUsesOtherEnum;

    new-instance v37, Lenums/TestEnumUsesOtherEnum;

    new-array v14, v10, [Ljava/lang/String;

    const-string v15, "TIMESTAMP"

    aput-object v15, v14, v13

    const-string v16, "TIMESTAMP"

    const/16 v17, 0x10

    const-string v18, "TIMESTAMP"

    move-object/from16 v15, v37

    move-object/from16 v20, v14

    invoke-direct/range {v15 .. v20}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V

    sput-object v37, Lenums/TestEnumUsesOtherEnum;->y:Lenums/TestEnumUsesOtherEnum;

    new-instance v38, Lenums/TestEnumUsesOtherEnum;

    new-array v15, v8, [Ljava/lang/String;

    const-string v14, "JSON"

    aput-object v14, v15, v13

    const-string v14, "JSONB"

    aput-object v14, v15, v10

    const-string v16, "JSON"

    const/16 v17, 0x11

    const-string v18, "JSON"

    const/16 v19, 0x0

    const/16 v20, 0x0

    const/16 v21, 0x1

    move-object/from16 v14, v38

    move-object/from16 v22, v15

    move-object/from16 v15, v16

    move/from16 v16, v17

    move-object/from16 v17, v18

    move-object/from16 v18, v19

    move-object/from16 v19, v23

    invoke-direct/range {v14 .. v22}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v38, Lenums/TestEnumUsesOtherEnum;->z:Lenums/TestEnumUsesOtherEnum;

    new-instance v39, Lenums/TestEnumUsesOtherEnum;

    new-array v15, v10, [Ljava/lang/String;

    const-string v14, "UUID"

    aput-object v14, v15, v13

    const-string v16, "UUID"

    const/16 v17, 0x12

    const-string v18, "UUID"

    const/16 v19, 0x0

    move-object/from16 v14, v39

    move-object/from16 v22, v15

    move-object/from16 v15, v16

    move/from16 v16, v17

    move-object/from16 v17, v18

    move-object/from16 v18, v19

    move-object/from16 v19, v23

    invoke-direct/range {v14 .. v22}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    sput-object v39, Lenums/TestEnumUsesOtherEnum;->A:Lenums/TestEnumUsesOtherEnum;

    const/16 v14, 0x13

    new-array v14, v14, [Lenums/TestEnumUsesOtherEnum;

    aput-object v9, v14, v13

    aput-object v30, v14, v10

    aput-object v31, v14, v8

    aput-object v0, v14, v7

    aput-object v1, v14, v24

    aput-object v2, v14, v11

    aput-object v3, v14, v25

    aput-object v4, v14, v12

    const/16 v0, 0x8

    aput-object v5, v14, v0

    aput-object v6, v14, v26

    aput-object v29, v14, v27

    aput-object v32, v14, v28

    const/16 v0, 0xc

    aput-object v33, v14, v0

    const/16 v0, 0xd

    aput-object v34, v14, v0

    const/16 v0, 0xe

    aput-object v35, v14, v0

    const/16 v0, 0xf

    aput-object v36, v14, v0

    const/16 v0, 0x10

    aput-object v37, v14, v0

    const/16 v0, 0x11

    aput-object v38, v14, v0

    const/16 v0, 0x12

    aput-object v39, v14, v0

    sput-object v14, Lenums/TestEnumUsesOtherEnum;->B:[Lenums/TestEnumUsesOtherEnum;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;ILjava/lang/String;Lenums/TestEnumUsesOtherEnum;[Ljava/lang/String;)V
    .registers 15
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Lenums/TestEnumUsesOtherEnum;",
            "[",
            "Ljava/lang/String;",
            ")V"
        }
    .end annotation

    iget-object v4, p4, Lenums/TestEnumUsesOtherEnum;->d:Ljava/lang/String;

    iget-object v5, p4, Lenums/TestEnumUsesOtherEnum;->e:Ljava/lang/Integer;

    iget-boolean v6, p4, Lenums/TestEnumUsesOtherEnum;->f:Z

    iget-boolean v7, p4, Lenums/TestEnumUsesOtherEnum;->g:Z

    move-object v0, p0

    move-object v1, p1

    move v2, p2

    move-object v3, p3

    move-object v8, p5

    invoke-direct/range {v0 .. v8}, Lenums/TestEnumUsesOtherEnum;-><init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZZ[Ljava/lang/String;)V
    .registers 9
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            "Ljava/lang/Integer;",
            "ZZ[",
            "Ljava/lang/String;",
            ")V"
        }
    .end annotation

    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    iput-object p3, p0, Lenums/TestEnumUsesOtherEnum;->c:Ljava/lang/String;

    iput-object p4, p0, Lenums/TestEnumUsesOtherEnum;->d:Ljava/lang/String;

    iput-object p5, p0, Lenums/TestEnumUsesOtherEnum;->e:Ljava/lang/Integer;

    iput-boolean p6, p0, Lenums/TestEnumUsesOtherEnum;->f:Z

    iput-boolean p7, p0, Lenums/TestEnumUsesOtherEnum;->g:Z

    iput-object p8, p0, Lenums/TestEnumUsesOtherEnum;->h:[Ljava/lang/String;

    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumUsesOtherEnum;
    .registers 2

    const-class v0, Lenums/TestEnumUsesOtherEnum;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lenums/TestEnumUsesOtherEnum;

    return-object p0
.end method

.method public static values()[Lenums/TestEnumUsesOtherEnum;
    .registers 1

    sget-object v0, Lenums/TestEnumUsesOtherEnum;->B:[Lenums/TestEnumUsesOtherEnum;

    invoke-virtual {v0}, [Lenums/TestEnumUsesOtherEnum;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnumUsesOtherEnum;

    return-object v0
.end method


# virtual methods
.method public c()Z
    .registers 2

    iget-object v0, p0, Lenums/TestEnumUsesOtherEnum;->e:Ljava/lang/Integer;

    if-eqz v0, :cond_d

    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v0

    if-eqz v0, :cond_b

    goto :goto_d

    :cond_b
    const/4 v0, 0x0

    goto :goto_e

    :cond_d
    :goto_d
    const/4 v0, 0x1

    :goto_e
    return v0
.end method

.method public toString()Ljava/lang/String;
    .registers 2
    iget-object v0, p0, Lenums/TestEnumUsesOtherEnum;->c:Ljava/lang/String;
    return-object v0
.end method
