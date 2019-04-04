.class public LTestTernaryInIf2;
.super Ljava/lang/Object;


# instance fields
.field private a:Ljava/lang/String;

.field private b:Ljava/lang/String;

.field private c:Ljava/lang/String;

# direct methods
.method public constructor <init>()V
    .locals 0

    .line 10
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public equals(Ljava/lang/Object;)Z
    .locals 4

    const/4 v0, 0x1

    if-ne p0, p1, :cond_0

    return v0

    :cond_0
    const/4 v1, 0x0

    if-eqz p1, :cond_a

    .line 110
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v2

    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    if-eq v2, v3, :cond_1

    goto :goto_4

    .line 112
    :cond_1
    check-cast p1, LTestTernaryInIf2;

    .line 114
    iget-object v2, p0, LTestTernaryInIf2;->a:Ljava/lang/String;

    if-eqz v2, :cond_2

    iget-object v2, p0, LTestTernaryInIf2;->a:Ljava/lang/String;

    iget-object v3, p1, LTestTernaryInIf2;->a:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_3

    goto :goto_0

    :cond_2
    iget-object v2, p1, LTestTernaryInIf2;->a:Ljava/lang/String;

    if-eqz v2, :cond_3

    :goto_0
    return v1

    .line 116
    :cond_3
    iget-object v2, p0, LTestTernaryInIf2;->b:Ljava/lang/String;

    if-eqz v2, :cond_4

    iget-object v2, p0, LTestTernaryInIf2;->b:Ljava/lang/String;

    iget-object v3, p1, LTestTernaryInIf2;->b:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_5

    goto :goto_1

    :cond_4
    iget-object v2, p1, LTestTernaryInIf2;->b:Ljava/lang/String;

    if-eqz v2, :cond_5

    :goto_1
    return v1

    .line 118
    :cond_5
    iget-object v2, p0, LTestTernaryInIf2;->c:Ljava/lang/String;

    if-eqz v2, :cond_6

    iget-object v2, p0, LTestTernaryInIf2;->c:Ljava/lang/String;

    iget-object v3, p1, LTestTernaryInIf2;->c:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_7

    return v1


    :cond_6
    iget-object v2, p1, LTestTernaryInIf2;->c:Ljava/lang/String;

    if-eqz v2, :cond_7

    .line 120
    :cond_7

    :cond_8
    const/4 v0, 0x0

    :goto_3
    return v0

    :cond_a
    :goto_4
    return v1
.end method

