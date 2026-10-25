# Field average bunch weight

Crop Production shows Average Bunch Weight immediately after Crop Budget and Actual on each field report. iPhone, iPad and Android use the same labels, kg/bunch units and source values.

The locally imported estate JSON's `abw` section carries cumulative/YTD source averages, with columns:

- `Field code`
- `Year`
- `YTD ABW (kg/bunch)`
- `Reporting cutoff`

Each field/year pair must be unique. Use the source's field subtotal, not a simple average of block averages. The app shows the latest non-future source year and the preceding year, with missing, duplicate or invalid values Pending. Confirmed zero remains zero. Older snapshots stay compatible and show Pending when field ABW is absent.

The supplied comparison does not identify its YTD cutoff. The field report explicitly marks that cutoff unconfirmed and does not imply that ABW and the dated crop report cover the same period. Monthly figures are excluded from this import. Source files and operational values stay in user-controlled local files and private backups. No server connection is required.
