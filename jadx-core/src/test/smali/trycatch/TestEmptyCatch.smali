.class synthetic Ltrycatch/TestEmptyCatch;
.super Ljava/lang/Object;


# static fields
.field static final synthetic $SwitchMap$Access:[I


# direct methods
.method static constructor <clinit>()V
    .locals 3

    .line 49
    invoke-static {}, Lother/EnumAccess;->values()[Lother/EnumAccess;

    move-result-object v0

    array-length v0, v0

    new-array v0, v0, [I

    sput-object v0, Ltrycatch/TestEmptyCatch;->$SwitchMap$Access:[I

    :try_start_0
    sget-object v1, Lother/EnumAccess;->QUERY:Lother/EnumAccess;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x1

    aput v2, v0, v1
    :try_end_0
    .catch Ljava/lang/NoSuchFieldError; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :try_start_1
    sget-object v0, Ltrycatch/TestEmptyCatch;->$SwitchMap$Access:[I

    sget-object v1, Lother/EnumAccess;->MODIFY:Lother/EnumAccess;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x2

    aput v2, v0, v1
    :try_end_1
    .catch Ljava/lang/NoSuchFieldError; {:try_start_1 .. :try_end_1} :catch_1

    :catch_1
    :try_start_2
    sget-object v0, Ltrycatch/TestEmptyCatch;->$SwitchMap$Access:[I

    sget-object v1, Lother/EnumAccess;->MODIFY_CONST:Lother/EnumAccess;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x3

    aput v2, v0, v1
    :try_end_2
    .catch Ljava/lang/NoSuchFieldError; {:try_start_2 .. :try_end_2} :catch_2

    :catch_2
    :try_start_3
    sget-object v0, Ltrycatch/TestEmptyCatch;->$SwitchMap$Access:[I

    sget-object v1, Lother/EnumAccess;->MODIFY_GETTER_SETTER:Lother/EnumAccess;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x4

    aput v2, v0, v1
    :try_end_3
    .catch Ljava/lang/NoSuchFieldError; {:try_start_3 .. :try_end_3} :catch_3

    :catch_3
    :try_start_4
    sget-object v0, Ltrycatch/TestEmptyCatch;->$SwitchMap$Access:[I

    sget-object v1, Lother/EnumAccess;->CONVERT_ACCESSOR_TO_DATA:Lother/EnumAccess;

    invoke-virtual {v1}, Ljava/lang/Enum;->ordinal()I

    move-result v1

    const/4 v2, 0x5

    aput v2, v0, v1
    :try_end_4
    .catch Ljava/lang/NoSuchFieldError; {:try_start_4 .. :try_end_4} :catch_4

    :catch_4
    return-void
.end method
