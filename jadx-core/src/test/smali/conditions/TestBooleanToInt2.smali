.class public LTestBooleanToInt2;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToInt2;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToInt2;->showConsent:Z

    invoke-virtual {p0, p1}, LTestBooleanToInt2;->write(I)V

    return-void
.end method

.method public write(I)V
    .locals 0

    return-void
.end method
