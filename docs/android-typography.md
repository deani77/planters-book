# Android typography

Android uses Rubik for every Material 3 typography role in both light and dark
mode. Regular body text, medium labels, semibold headings and existing explicit
bold values use the bundled variable font's actual 400/500/600/700 weight outlines.
Material's existing scalable sizes, line heights and letter spacing are retained.
The nested dashboard theme inherits the app typography. iOS is unchanged.

The font is bundled, so no connection, font provider or download is required.
Android's minimum supported version (API 26) supports variable fonts.

## Source and license

Unmodified `Rubik[wght].ttf` from https://github.com/google/fonts/tree/main/ofl/rubik
is stored as `app/src/main/res/font/rubik.ttf` (359,804 bytes).
Git blob: `a59aeb632df494345a542697c357f608875481b0`.

The accompanying SIL Open Font License 1.1 and copyright notice are packaged in
`app/src/main/assets/fonts/Rubik-OFL.txt`.
License Git blob: `03af63b533942ef3bb9919f81138fde8824d4554`.
