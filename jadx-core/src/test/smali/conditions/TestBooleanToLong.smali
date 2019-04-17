.class public LTestBooleanToLong;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToLong;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToLong;->showConsent:Z

    int-to-long p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToLong;->write(J)V

    return-void
.end method

.method public write(J)V
    .locals 0

    return-void
.end method
