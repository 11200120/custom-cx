import { NgModule } from '@angular/core';
import { AnonymousConsentsModule, AuthModule, CostCenterOccModule, ExternalRoutesModule, ProductModule, ProductOccModule, provideFeatureToggles, UserModule, UserOccModule } from "@spartacus/core";
import { AnonymousConsentManagementBannerModule, AnonymousConsentsDialogModule, BannerCarouselModule, BannerModule, BreadcrumbModule, CategoryNavigationModule, CmsParagraphModule, ConsentManagementModule, FooterNavigationModule, HamburgerMenuModule, HomePageEventModule, LinkModule, LoginRouteModule, LogoutModule, MyAccountV2Module, MyCouponsModule, MyInterestsModule, NavigationEventModule, NavigationModule, NotificationPreferenceModule, PageTitleModule, PaymentMethodsModule, PDFModule, ProductCarouselModule, ProductDetailsPageModule, ProductFacetNavigationModule, ProductImagesModule, ProductIntroModule, ProductListingPageModule, ProductListModule, ProductPageEventModule, ProductReferencesModule, ProductSummaryModule, ProductTabsModule, ScrollToTopModule, SearchBoxModule, SiteContextSelectorModule, StockNotificationModule, TabParagraphContainerModule, VideoModule } from "@spartacus/storefront";
import { UserFeatureModule } from './features/user/user-feature.module';
import { CartBaseFeatureModule } from './features/cart/cart-base-feature.module';
import { CartSavedCartFeatureModule } from './features/cart/cart-saved-cart-feature.module';
import { WishListFeatureModule } from './features/cart/wish-list-feature.module';
import { CartQuickOrderFeatureModule } from './features/cart/cart-quick-order-feature.module';
import { CartImportExportFeatureModule } from './features/cart/cart-import-export-feature.module';
import { OrderFeatureModule } from './features/order/order-feature.module';
import { CheckoutFeatureModule } from './features/checkout/checkout-feature.module';
import { PersonalizationFeatureModule } from './features/tracking/personalization-feature.module';
import { TagManagementFeatureModule } from './features/tracking/tag-management-feature.module';
import { PdfinvoicesModuleFeatureModule } from './features/pdf-invoices/pdfinvoices-module-feature.module';
import { CustomerTicketingFeatureModule } from './features/customer-ticketing/customer-ticketing-feature.module';
import { OrganizationUserRegistrationFeatureModule } from './features/organization/organization-user-registration-feature.module';
import { OrganizationAdministrationFeatureModule } from './features/organization/organization-administration-feature.module';
import { OrganizationAccountSummaryFeatureModule } from './features/organization/organization-account-summary-feature.module';
import { OrganizationUnitOrderFeatureModule } from './features/organization/organization-unit-order-feature.module';
import { OrganizationOrderApprovalFeatureModule } from './features/organization/organization-order-approval-feature.module';
import { ProductConfiguratorFeatureModule } from './features/product-configurator/product-configurator-feature.module';
import { StoreFinderFeatureModule } from './features/storefinder/store-finder-feature.module';
import { AsmFeatureModule } from './features/asm/asm-feature.module';
import { AsmCustomer360FeatureModule } from './features/asm/asm-customer360-feature.module';
import { SmartEditFeatureModule } from './features/smartedit/smart-edit-feature.module';
import { ProductVariantsFeatureModule } from './features/product/product-variants-feature.module';
import { ProductImageZoomFeatureModule } from './features/product/product-image-zoom-feature.module';
import { ProductBulkPricingFeatureModule } from './features/product/product-bulk-pricing-feature.module';
import { PickupInStoreFeatureModule } from './features/pickup-in-store/pickup-in-store-feature.module';

@NgModule({
  declarations: [],
  imports: [
    AuthModule.forRoot(),
    LogoutModule,
    LoginRouteModule,
    HamburgerMenuModule,
    SiteContextSelectorModule,
    LinkModule,
    BannerModule,
    CmsParagraphModule,
    TabParagraphContainerModule,
    BannerCarouselModule,
    CategoryNavigationModule,
    NavigationModule,
    FooterNavigationModule,
    BreadcrumbModule,
    ScrollToTopModule,
    PageTitleModule,
    VideoModule,
    PDFModule,
    UserModule,
    UserOccModule,
    PaymentMethodsModule,
    NotificationPreferenceModule,
    MyInterestsModule,
    MyAccountV2Module,
    StockNotificationModule,
    ConsentManagementModule,
    MyCouponsModule,
    AnonymousConsentsModule.forRoot(),
    AnonymousConsentsDialogModule,
    AnonymousConsentManagementBannerModule,
    ProductModule.forRoot(),
    ProductOccModule,
    ProductDetailsPageModule,
    ProductListingPageModule,
    ProductListModule,
    SearchBoxModule,
    ProductFacetNavigationModule,
    ProductTabsModule,
    ProductCarouselModule,
    ProductReferencesModule,
    ProductImagesModule,
    ProductSummaryModule,
    ProductIntroModule,
    CostCenterOccModule,
    NavigationEventModule,
    HomePageEventModule,
    ProductPageEventModule,
    ExternalRoutesModule.forRoot(),
    UserFeatureModule,
    CartBaseFeatureModule,
    CartSavedCartFeatureModule,
    WishListFeatureModule,
    CartQuickOrderFeatureModule,
    CartImportExportFeatureModule,
    OrderFeatureModule,
    CheckoutFeatureModule,
    PersonalizationFeatureModule,
    TagManagementFeatureModule,
    PdfinvoicesModuleFeatureModule,
    CustomerTicketingFeatureModule,
    OrganizationUserRegistrationFeatureModule,
    OrganizationAdministrationFeatureModule,
    OrganizationAccountSummaryFeatureModule,
    OrganizationUnitOrderFeatureModule,
    OrganizationOrderApprovalFeatureModule,
    ProductConfiguratorFeatureModule,
    StoreFinderFeatureModule,
    AsmFeatureModule,
    AsmCustomer360FeatureModule,
    SmartEditFeatureModule,
    ProductVariantsFeatureModule,
    ProductImageZoomFeatureModule,
    ProductBulkPricingFeatureModule,
    PickupInStoreFeatureModule
  ],
  providers: [provideFeatureToggles({
    "formErrorsDescriptiveMessages": true,
    "showSearchingCustomerByOrderInASM": true,
    "showStyleChangesInASM": true,
    "shouldHideAddToCartForUnpurchasableProducts": true,
    "useExtractedBillingAddressComponent": true,
    "showBillingAddressInDigitalPayments": true,
    "showDownloadProposalButton": true,
    "showPromotionsInPDP": true,
    "recentSearches": true,
    "pdfInvoicesSortByInvoiceDate": true,
    "storeFrontLibCardParagraphTruncated": true,
    "productConfiguratorAttributeTypesV2": true,
    "a11yRequiredAsterisks": true,
    "a11yQuantityOrderTabbing": true,
    "a11yNavigationUiKeyboardControls": true,
    "a11yOrderConfirmationHeadingOrder": true,
    "a11yStarRating": true,
    "a11yViewChangeAssistiveMessage": true,
    "a11yReorderDialog": true,
    "a11yPopoverFocus": true,
    "a11yScheduleReplenishment": true,
    "a11yScrollToTop": true,
    "a11ySavedCartsZoom": true,
    "a11ySortingOptionsTruncation": true,
    "a11yExpandedFocusIndicator": true,
    "a11yCheckoutDeliveryFocus": true,
    "a11yMobileVisibleFocus": true,
    "a11yOrganizationsBanner": true,
    "a11yOrganizationListHeadingOrder": true,
    "a11yReplenishmentOrderFieldset": true,
    "a11yListOversizedFocus": true,
    "a11yStoreFinderOverflow": true,
    "a11yCartSummaryHeadingOrder": true,
    "a11ySearchBoxMobileFocus": true,
    "a11yFacetKeyboardNavigation": true,
    "a11yUnitsListKeyboardControls": true,
    "a11yCartItemsLinksStyles": true,
    "a11yHideSelectBtnForSelectedAddrOrPayment": true,
    "a11yFocusableCarouselControls": true,
    "cmsGuardsServiceUseGuardsComposer": true,
    "cartQuickOrderRemoveListeningToFailEvent": true,
    "a11yKeyboardAccessibleZoom": true,
    "a11yOrganizationLinkableCells": true,
    "a11yVisibleFocusOverflows": true,
    "a11yTruncatedTextForResponsiveView": true,
    "a11ySemanticPaginationLabel": true,
    "a11yPreventCartItemsFormRedundantRecreation": true,
    "a11yPreventSRFocusOnHiddenElements": true,
    "a11yMyAccountLinkOutline": true,
    "a11yCloseProductImageBtnFocus": true,
    "a11yNotificationPreferenceFieldset": true,
    "a11yImproveContrast": true,
    "a11yEmptyWishlistHeading": true,
    "a11yScreenReaderBloatFix": true,
    "a11yUseButtonsForBtnLinks": true,
    "a11yNotificationsOnConsentChange": true,
    "a11yDisabledCouponAndQuickOrderActionButtonsInsteadOfRequiredFields": true,
    "a11yFacetsDialogFocusHandling": true,
    "a11yStoreFinderAlerts": true,
    "a11yFormErrorMuteIcon": true,
    "a11yCxMessageFocus": true,
    "a11yLinkBtnsToTertiaryBtns": true,
    "a11yDeliveryModeRadiogroup": true,
    "a11yNgSelectMobileReadout": true,
    "occCartNameAndDescriptionInHttpRequestBody": true,
    "cmsBottomHeaderSlotUsingFlexStyles": true
  })]
})
export class SpartacusFeaturesModule { }
