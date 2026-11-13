# Azure Translator setup

1. Sign in at https://portal.azure.com and create a Translator resource.
2. Select the **F0 Free** pricing tier. The app cannot verify your billing tier; S1 is paid.
3. In the resource's **Keys and Endpoint** page, copy one key and the region.
4. In the keyboard app, open **Azure Translator → Azure Key / Region**, enter them, and save.
5. For a global single-service resource, use `global`; regional resources require the exact region (for example `southeastasia`).
6. Enable Azure and tap **Test Azure connection**. It translates `Hello` from English to Myanmar using saved settings.
7. Test with VPN off to check whether your own network can reach the service. VPN-free access is not guaranteed by this app.

Keys are encrypted on the device and never bundled with the application. The endpoint is fixed to Microsoft's HTTPS Translator v3 API. Network translation must be enabled.

Azure mode uses only Azure for network translation. Errors are shown without automatically sending text to another provider. Turn Azure off to return to Gemini mode. The current build disables OpenRouter fallback with `GEMINI_ONLY = true`. Saved keys are retained. Unambiguous local Mon dictionary matches can be returned without contacting Azure.

Azure uses standard translation without Natural/Formal/Casual prompt control. The language chooser still includes languages for the AI providers; Azure may reject pairs it does not support.

Official references:
- https://learn.microsoft.com/en-us/azure/ai-services/translator/text-translation/how-to/use-rest-api
- https://azure.microsoft.com/en-us/pricing/details/translator/
- https://learn.microsoft.com/en-us/azure/ai-services/translator/language-support
