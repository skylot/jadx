.class public Larrays/TestArrayInitField2;
.super Ljava/lang/Object;

.field static myArr:[J

.method static constructor <clinit>()V
    .locals 4

    const v0, 0x3
    new-array v0, v0, [J
    sput-object v0, Larrays/TestArrayInitField2;->myArr:[J
    const/4 v1, 0x0
    const-wide/32 v2, 0x4c78b648
    aput-wide v2, v0, v1
    return-void
    nop

.end method

.method public check()V
    .registers 5
    sget-object v0, Larrays/TestArrayInitField2;->myArr:[J
    invoke-static {v0}, Lorg/assertj/core/api/Assertions;->assertThat([J)Lorg/assertj/core/api/AbstractLongArrayAssert;
    move-result-object v0
    const/4 v1, 0x3
    invoke-virtual {v0, v1}, Lorg/assertj/core/api/AbstractLongArrayAssert;->hasSize(I)Lorg/assertj/core/api/AbstractLongArrayAssert;

    sget-object v0, Larrays/TestArrayInitField2;->myArr:[J
    const/4 v1, 0x0
    aget-wide v0, v0, v1
    invoke-static {v0, v1}, Lorg/assertj/core/api/Assertions;->assertThat(J)Lorg/assertj/core/api/AbstractLongAssert;
    move-result-object v0
    const-wide/32 v2, 0x4c78b648
    invoke-virtual {v0, v2, v3}, Lorg/assertj/core/api/AbstractLongAssert;->isEqualTo(J)Lorg/assertj/core/api/AbstractLongAssert;
    return-void
.end method
