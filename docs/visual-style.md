# App visual style

The home page uses Planter’s Log with Ladang Selendang 3 as estate context. A large forest-green dashboard action leads into the operational summary. Six secondary modules use consistent cards, symbols, borders and spacing; the application name and offline status stay in the footer.

The shared visual language uses a restrained forest palette, low-contrast original vector palm artwork, rounded content surfaces and clear typography. Decorative backgrounds are local and excluded from accessibility. Data stays on opaque surfaces. Light appearance uses ivory backgrounds, white surfaces and dark green accents; dark appearance uses charcoal-green backgrounds, deep green surfaces and pale mint accents. System text colors, scalable type and scroll access support readability.

| Role | Light | Dark |
| --- | --- | --- |
| Background | #F3F6F1 | #101C17 |
| Surface | #FFFFFF | #1A2B23 |
| Accent | #20543E | #9EDDB9 |
| Soft accent | #E5EFE7 | #243F31 |
| Border | #D5E2D8 | #365243 |

On iOS the semantic colors, background and screen/group-box styles live in EstateTheme.swift. Home buttons, the pinned iPad dashboard containing all seven cards, phone dashboard, estate panels, field details, upkeep, production, Settings and their child screens follow this style. Android uses the matching EstateColors.kt Material palette, icon-above-label home buttons, vector background and estate panel headers. Its opened dashboard uses the same seven sections, labels, units, source periods and missing-data rules as iOS. iPad groups all seven sections inside one large home dashboard card; phones open their dashboard through the home button.

Every numeric metric retains its unit and source period. Missing operational records remain Pending. Changing appearance does not alter imports, saved records, sync or calculations. No estate records, tokens or signing details are part of the visual design assets.


The iPad dashboard uses one adaptive surface with subtle section fills and a clear distinction between headings, figures and units. Missing values appear as an em dash with one Awaiting data key; accessibility labels retain the full Pending value and unit. Duplicate missing-period messages are suppressed without altering stored records or valid reporting periods. This readability revision is being reviewed on iPad first.
