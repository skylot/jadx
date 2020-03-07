.class public Lothers/B;
.super Lothers/A;

.field public A00:Lothers/C;

# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .registers 3

    .prologue
    invoke-direct {p0, p1}, Lothers/A;-><init>(Ljava/lang/String;)V

    return-void
.end method


.method public add(I)I
    .registers 3

    iget v1, p0, Lothers/A;->A00:I

    add-int/2addr v1, p1

	return v1
.end method
