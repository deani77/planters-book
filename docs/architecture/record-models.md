# Operational record model

Units are explicit in names. Missing values are null; measured zero is zero. Fields retain user identifiers. Monthly crop records have a reporting month, explicit cutoff and a completion flag; a complete report ends at month end. ABW is annual or YTD with an explicit cutoff; annual ends on December 31. Estate-wide upkeep uses no field code. Bag counts are retained without inferring a bag weight; conversion requires a positive explicit kg/bag value.

These domain types complement the existing version-1 estate envelope. This PR does not migrate storage or reinterpret old annual/YTD ABW. Existing raw fields and extensions remain intact. Workbook mapping belongs to PR #21 and must be implemented against these definitions with preview and validation before new input sheets ship.
