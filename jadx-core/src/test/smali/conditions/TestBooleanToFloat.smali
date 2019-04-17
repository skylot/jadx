.class public LTestBooleanToFloat;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToFloat;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToFloat;->showConsent:Z

    int-to-float p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToFloat;->write(F)V

    return-void
.end method

.method public write(F)V
    .locals 0

    return-void
.end method
