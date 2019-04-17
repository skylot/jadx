.class public LTestBooleanToDouble;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToDouble;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToDouble;->showConsent:Z

    int-to-double p1, p1

    invoke-virtual {p0, p1}, LTestBooleanToDouble;->write(D)V

    return-void
.end method

.method public write(D)V
    .locals 0

    return-void
.end method
