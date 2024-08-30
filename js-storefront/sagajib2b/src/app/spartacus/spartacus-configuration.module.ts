import { NgModule } from '@angular/core';
import { translationChunksConfig, translations } from "@spartacus/assets";
import { FeaturesConfig, I18nConfig, OccConfig, provideConfig, SiteContextConfig } from "@spartacus/core";
import { defaultB2bOccConfig } from "@spartacus/setup";
import { defaultCmsContentProviders, layoutConfig, mediaConfig } from "@spartacus/storefront";
import { SmartEditConfig } from '@spartacus/smartedit/root';

@NgModule({
  declarations: [],
  imports: [
  ],
  providers: [provideConfig(layoutConfig), provideConfig(mediaConfig), ...defaultCmsContentProviders, provideConfig(<OccConfig>{
    backend: {
      occ: {
        baseUrl: 'https://api.cdw4d97joy-distribuc1-d1-public.model-t.cc.commerce.ondemand.com/',
      }
    },
  }), provideConfig(<SiteContextConfig>{
    context: {
      urlParameters: ['baseSite', 'language', 'currency'],
      baseSite: ['powertools-spa'],
      currency: ['USD',]
    },
  }), provideConfig(<I18nConfig>{
    i18n: {
      resources: translations,
      chunks: translationChunksConfig,
      fallbackLang: 'en'
    },
  }), provideConfig(<FeaturesConfig>{
    features: {
      level: '2211.27'
    }
  }), provideConfig(defaultB2bOccConfig),
      provideConfig({
        smartEdit: {
          storefrontPreviewRoute: 'cx-preview',
          allowOrigin: 'localhost:9002,*.*.model-t.cc.commerce.ondemand.com:443',
        }
        } as SmartEditConfig)]
})
export class SpartacusConfigurationModule { }
