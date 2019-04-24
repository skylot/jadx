.class public LTestBooleanToShort;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToShort;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToShort;->showConsent:Z

    int-to-short p1, p1

    invoke-virtual {p0, p1}, LTestBooleanToShort;->write(S)V

    return-void
.end method

.method public write(S)V
    .locals 0

    return-void
.end method
