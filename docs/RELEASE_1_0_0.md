# Brain Keyboard 1.0.0

- Version code: **2**; Android 8.0/API 26 and newer; package `com.zrok.myanmaraikeyboard`.
- Brain Keyboard name, original mint/navy logo, adaptive and Android 13 themed icons.
- Myanmar/English layouts, globe switch, newline Enter, long-press emoji, repeated Delete and English suggestions.
- Auto-detected translation source, editable preview, clear Close control, full Clear/cancellation of keyboard drafts and caches.
- Gemini setup guide; bounded translation requests and local/glossary-assisted Mon dictionary lookup.
- 12 Burmese reaction stickers, user PNG/WebP/GIF/JPEG import and MIME-aware image insertion.
- Selectable key click sounds, volume and system/custom keyboard haptics.

## Files

| File | Purpose |
| --- | --- |
| `Brain-Keyboard-1.0.0-release.apk` | Direct Android installation |
| `Brain-Keyboard-1.0.0-release.aab` | Google Play upload |
| `SHA256SUMS.txt` | SHA-256 checksums for both files |

Signed with the supplied keystore's `key` alias. Public signing-certificate SHA-256:

```text
ecf8757c42db7092b7ab544e80c518776e35ef7fb26486675792124120e744c8
```

The keystore and passwords are not in this repository or release assets. APK v2/v3 signatures, alignment and the AAB JAR signature were verified. Android release keys normally use self-signed certificates; the JAR certificate-chain warning does not mean the signature failed.

## Validation and limitations

- Release unit tests: **30 passed**.
- Release lint: **0 errors**, **35 warnings**, **5 informational issues**.
- Signed release is not debuggable. Device interaction tests were performed with development builds; replacing the installed debug-signed app with this release was not performed.
- Experimental Mon/Shan/Karen/Jinghpaw translation is not certified. Dictionary support does not guarantee accurate Mon sentences; a tested Mon response retained Burmese. No dedicated Mon translation server is included.
- Gboard's proprietary engine, complete online sticker catalog and Emoji Kitchen are not included. Image insertion depends on the receiving app's MIME support.
- Existing installations signed with another certificate cannot be updated by this APK. Uninstalling an old app deletes its local settings, saved API keys and custom stickers.

See [README](../README.md) for installation, API setup, privacy, building, signing and dataset attribution.
