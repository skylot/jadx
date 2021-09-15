.class public Lothers/TestFieldInitOrder2;
.super Ljava/lang/Object;

.field private static final VALUE:Ljava/lang/String;
.field static final ZPREFIX:Ljava/lang/String; = "SOME_"


# direct methods
.method static constructor <clinit>()V
    .registers 2

    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    sget-object v1, Lothers/TestFieldInitOrder2;->ZPREFIX:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v0

    const-string v1, "VALUE"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0

    sput-object v0, Lothers/TestFieldInitOrder2;->VALUE:Ljava/lang/String;
    return-void
.end method

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method


.method public check()V
    .registers 3
    sget-object v0, Lothers/TestFieldInitOrder2;->VALUE:Ljava/lang/String;
    invoke-static {v0}, Ljadx/tests/api/utils/assertj/JadxAssertions;->assertThat(Ljava/lang/String;)Ljadx/tests/api/utils/assertj/JadxCodeAssertions;
    move-result-object v0
    const-string v1, "SOME_VALUE"
    invoke-virtual {v0, v1}, Ljadx/tests/api/utils/assertj/JadxCodeAssertions;->isEqualTo(Ljava/lang/String;)Lorg/assertj/core/api/AbstractStringAssert;
    return-void
.end method
