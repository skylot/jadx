.class public LTestVariables6;
.super Lcom/paypal/android/p2pmobile/wallet/banksandcards/fragments/BasePaymentFragment;
.source "SourceFile"

# interfaces
.implements Landroid/support/v13/app/FragmentCompat$OnRequestPermissionsResultCallback;
.implements Landroid/widget/TextView$OnEditorActionListener;
.implements Lcom/paypal/android/p2pmobile/common/utils/ISafeClickVerifierListener;
.implements Lcom/paypal/android/p2pmobile/common/widgets/CSCTextWatcher$ICSCTextWatcherListener;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/paypal/android/p2pmobile/wallet/banksandcards/fragments/EnterCardFragment$IEnterCardFragmentListener;
    }
.end annotation


.field private static final DATE_SEPARATOR:C = '/'

.field mDateFormatOrder:Lcom/paypal/android/p2pmobile/common/utils/ValidatedDateFormatOrder;


# direct methods
.method static constructor <clinit>()V
    .locals 0

    return-void
.end method

.method public constructor <init>()V
    .locals 1

    return-void
.end method

.method private bindStartDateToMutableCredebitCard(Lcom/paypal/android/foundation/wallet/model/MutableCredebitCard;)Z
    .locals 10
    .param p1    # Lcom/paypal/android/foundation/wallet/model/MutableCredebitCard;
        .annotation build Landroid/support/annotation/NonNull;
        .end annotation
    .end param

    .line 1024
    iget-object v0, p0, LTestVariables6;->mFinancialInstrumentMetadataDefinition:Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataDefinition;

    invoke-virtual {v0}, Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataDefinition;->getStartMonth()Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataAttribute;

    move-result-object v0

    .line 1025
    iget-object v1, p0, LTestVariables6;->mFinancialInstrumentMetadataDefinition:Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataDefinition;

    invoke-virtual {v1}, Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataDefinition;->getStartYear()Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataAttribute;

    move-result-object v1

    const/4 v2, 0x2

    .line 1026
    new-array v2, v2, [Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataAttribute;

    const/4 v3, 0x0

    aput-object v0, v2, v3

    const/4 v0, 0x1

    aput-object v1, v2, v0

    invoke-static {v2}, Lcom/paypal/android/p2pmobile/wallet/banksandcards/utils/EnterCardFragmentUtils;->attributesAreRequired([Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataAttribute;)Z

    move-result v2

    if-nez v2, :cond_0

    return v0

    .line 1030
    :cond_0
    invoke-virtual {p0}, LTestVariables6;->getView()Landroid/view/View;

    move-result-object v2

    if-nez v2, :cond_1

    return v3

    .line 1035
    :cond_1
    invoke-virtual {v1}, Lcom/paypal/android/foundation/wallet/model/FinancialInstrumentMetadataAttribute;->getMaximumLength()I

    move-result v6

    .line 1036
    sget v1, Lcom/paypal/android/p2pmobile/wallet/R$id;->enter_card_start_date:I

    invoke-virtual {v2, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Landroid/widget/TextView;

    .line 1038
    new-instance v2, Lcom/paypal/android/p2pmobile/common/utils/DateStringParser;

    invoke-virtual {v1}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v1

    invoke-interface {v1}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v5

    iget-object v7, p0, LTestVariables6;->mDateFormatOrder:Lcom/paypal/android/p2pmobile/common/utils/ValidatedDateFormatOrder;

    const/16 v8, 0x2f

    const/4 v9, 0x0

    move-object v4, v2

    invoke-direct/range {v4 .. v9}, Lcom/paypal/android/p2pmobile/common/utils/DateStringParser;-><init>(Ljava/lang/String;ILcom/paypal/android/p2pmobile/common/utils/ValidatedDateFormatOrder;CZ)V

    .line 1039
    invoke-virtual {v2}, Lcom/paypal/android/p2pmobile/common/utils/DateStringParser;->isError()Z

    move-result v1

    if-nez v1, :cond_2

    .line 1040
    invoke-virtual {v2}, Lcom/paypal/android/p2pmobile/common/utils/DateStringParser;->getDate()Ljava/util/Date;

    move-result-object v1

    invoke-virtual {p1, v1}, Lcom/paypal/android/foundation/wallet/model/MutableCredebitCard;->setIssueDate(Ljava/util/Date;)V

    return v0

    :cond_2
    return v3
.end method
