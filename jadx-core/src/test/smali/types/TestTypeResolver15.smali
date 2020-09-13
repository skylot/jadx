.class public Ltypes/TestTypeResolver15;
.super Ljava/lang/Object;

.method private test(Z)V
    .locals 2

    if-eqz p1, :cond_0

	const/4 v1, 0x0
	goto :goto_0

	:cond_0
	const/16 v1, 0x8

	:goto_0
	invoke-virtual {p0, v1}, Ltypes/TestTypeResolver15;->useInt(I)V

	xor-int/lit8 p1, p1, 0x1
	invoke-virtual {p0, p1}, Ltypes/TestTypeResolver15;->useInt(I)V

	return-void
.end method

.method private useInt(I)V
    .registers 2
    return-void
.end method
