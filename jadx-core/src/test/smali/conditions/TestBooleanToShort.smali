.class public LTestBooleanToShort;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToShort;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToShort;->showConsent:Z

    int-to-short p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToShort;->write(S)V

    return-void
.end method

.method public write(S)V
    .locals 0

    return-void
.end method
