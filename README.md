# Moon AI

Your own offline AI app for Android. It runs Google's **Gemma 3n E2B**
model fully on the phone with LiteRT-LM — no internet needed after the
model is downloaded once, no account, no ads, no analytics.

- Pure black theme (#000000) with white text throughout.
- English-only UI.
- Chat with streaming replies, clear-chat, Settings with a Hugging Face
  token field (the Gemma model is gated on Hugging Face).

## First run

1. Open Settings and save a free Hugging Face read token (see the
   steps on the Settings screen — you must accept the Gemma license
   on the model page first).
2. Tap **Download model** (about 3.4 GB — use WiFi).
3. When the status says "Model ready — fully offline", just chat.

## Building (GitHub Actions)

Upload `moon-ai-android.zip` to the repo root, add the workflow file at
`.github/workflows/build-apk.yml` (a copy lives in this project), push
to `main`, then download the `moon-ai-apk` artifact from the Actions tab.

No Gradle wrapper is included on purpose: the workflow installs
Gradle 8.7 itself (same as the Moon-Chat project).

## Model

- Repo: `google/gemma-3n-E2B-it-litert-lm`
- File: `gemma-3n-E2B-it-int4.litertlm` (3,655,827,456 bytes)
- SHA-256: `2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6`
- License: Gemma Terms of Use (accept on Hugging Face before downloading)
