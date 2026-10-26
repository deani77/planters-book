# Estate name from local data

Home and Estate Details use the user's saved Settings estate name first. If no custom name has been saved, an imported overview row with columns `Item`, `Details` and item `Estate name` can supply the name. Otherwise they show `Your Estate`.

The imported name must occur once, be nonblank, no more than 120 UTF-16 code units, and contain no control characters. It remains available in local saved data and changes only when the user confirms a newer local import. JSON and XLSX Overview sheets use the same validated records. No server, network client, or login is used. Custom names and estate identifiers are retained.
