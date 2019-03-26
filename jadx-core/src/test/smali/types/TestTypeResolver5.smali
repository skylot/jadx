.class public LTestTypeResolver5;
.super Landroid/content/Context;
.source "SourceFile"


# static fields
.field public static final EXTERNAL_SOURCE:Ljava/lang/String; = "externalsource"

.field public static final IS_APPBOY_CAMPAIGN:Ljava/lang/String; = "appBoyCampaign"

.field public static final IS_NEWS_FEED:Ljava/lang/String; = "isNewsFeed"


# direct methods
.method public constructor <init>()V
    .locals 0

    .prologue
    .line 35
    invoke-direct {p0}, Landroid/content/Context;-><init>()V

    return-void
.end method

.method private openNextScreen(Landroid/os/Bundle;)V
    .locals 3

    .prologue
    const/4 v1, 0x0

    .line 56
    if-eqz p1, :cond_2

    const-string v0, "externalsource"

    invoke-virtual {p1, v0}, Landroid/os/Bundle;->containsKey(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_2

    const-string v0, "externalsource"

    .line 57
    invoke-virtual {p1, v0}, Landroid/os/Bundle;->getString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    move-object v2, v0

    .line 58
    :goto_0
    if-eqz p1, :cond_3

    const-string v0, "isNewsFeed"

    invoke-virtual {p1, v0}, Landroid/os/Bundle;->containsKey(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_3

    const-string v0, "isNewsFeed"

    .line 59
    invoke-virtual {p1, v0}, Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z

    move-result v0

    .line 61
    :goto_1
    invoke-static {v2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    const/4 v1, 0x1

    .line 64
    :cond_0
    if-eqz p1, :cond_1

    .line 65
    new-instance v2, Landroid/webkit/WebView;

    invoke-direct {v2, p0}, Landroid/webkit/WebView;-><init>(Landroid/content/Context;)V

    .line 66
    invoke-direct {p0, v2, p1}, LTestTypeResolver5;->runJavaScriptForCampaign(Landroid/webkit/WebView;Landroid/os/Bundle;)V

    .line 70
    :cond_1
    if-eqz v0, :cond_4

    .line 72
    invoke-direct {p0, p1}, LTestTypeResolver5;->startHomeActivity(Landroid/os/Bundle;)V

    .line 73
    invoke-virtual {p0}, LTestTypeResolver5;->finish()V

    .line 80
    :goto_2
    return-void

    .line 57
    :cond_2
    const-string v0, ""

    move-object v2, v0

    goto :goto_0

    :cond_3
    move v0, v1

    .line 59
    goto :goto_1

    .line 74
    :cond_4
    invoke-virtual {p0}, LTestTypeResolver5;->isTaskRoot()Z

    move-result v0

    if-nez v0, :cond_5

    if-eqz v1, :cond_6

    .line 76
    :cond_5
    invoke-direct {p0, p1}, LTestTypeResolver5;->openSplash(Landroid/os/Bundle;)V

    goto :goto_2

    .line 78
    :cond_6
    invoke-virtual {p0}, LTestTypeResolver5;->finish()V

    goto :goto_2
.end method

.method private openSplash(Landroid/os/Bundle;)V
    .locals 1
    return-void
.end method

.method private runJavaScriptForCampaign(Landroid/webkit/WebView;Landroid/os/Bundle;)V
    .locals 1
    return-void
.end method

.method private startHomeActivity(Landroid/os/Bundle;)V
    .locals 1
    return-void
.end method

.method public onBackPressed()V
    .locals 1
    return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
    .locals 1
    return-void
.end method

.method protected onPause()V
    .locals 1
    return-void
.end method

.method protected onStart()V
    .locals 1
    return-void
.end method
