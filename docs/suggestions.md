# Suggestions

Settings → Suggestions opens a locally saved suggestion draft on Android and iOS.

**Email Suggestion** opens the user's email app with `deani77@gmail.com`, the subject **Planters Book — Suggestion**, the entered message and the app version filled in. Users must tap Send in their email app. Opening the email app is not proof of delivery, so Planters Book keeps the draft until the user clears it.

No background sending, SMTP password, API key, account integration or VPS is used. An email app must be installed/configured; email delivery requires that app's connectivity. No estate database, PDF or other attachments are included automatically.

Validation: Swift syntax parsing and mailto URL encoding checked. Navigation assertions updated. End-to-end device/email testing remains pending. No APK created.
