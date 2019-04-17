.class public LTestBooleanToLong;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToLong;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToLong;->showConsent:Z

    int-to-long p1, p1

    invoke-virtual {p0, p1}, LTestBooleanToLong;->write(J)V

    return-void
.end method

.method public write(J)V
    .locals 0

    return-void
.end method
