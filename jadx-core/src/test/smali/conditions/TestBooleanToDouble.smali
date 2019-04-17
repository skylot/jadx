.class public LTestBooleanToDouble;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToDouble;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToDouble;->showConsent:Z

    int-to-double p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToDouble;->write(D)V

    return-void
.end method

.method public write(D)V
    .locals 0

    return-void
.end method
