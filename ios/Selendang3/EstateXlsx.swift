import Foundation
import ZIPFoundation

private final class WorkbookXMLReader: NSObject, XMLParserDelegate {
    private(set) var rows: [[String]] = []
    private var row: [Int: String]?
    private var column = 0
    private var cellType = ""
    private var cellText = ""
    private var capture = false
    private(set) var sharedStrings: [String] = []
    private var inStringItem = false
    private var itemText = ""
    private var inValue = false

    init(shared: [String] = []) { sharedStrings = shared }
    func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName qName: String?, attributes: [String: String] = [:]) {
        switch name {
        case "row": row = [:]
        case "c":
            column = Self.columnIndex(attributes["r"] ?? "A1")
            cellType = attributes["t"] ?? ""
            cellText = ""
        case "si": inStringItem = true; itemText = ""
        case "v", "t": capture = true; inValue = name == "v"
        default: break
        }
    }
    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if capture { cellText += string }
        if inStringItem && capture && !inValue { itemText += string }
    }
    func parser(_ parser: XMLParser, didEndElement name: String, namespaceURI: String?, qualifiedName qName: String?) {
        switch name {
        case "v", "t": capture = false; inValue = false
        case "c":
            let value: String
            if cellType == "s", let index = Int(cellText), sharedStrings.indices.contains(index) {
                value = sharedStrings[index]
            } else { value = cellText }
            row?[column] = value
        case "row":
            if let row {
                let lastColumn = row.keys.max() ?? -1
                rows.append(lastColumn >= 0 ? (0...lastColumn).map { row[$0] ?? "" } : [])
            }
            row = nil
        case "si": sharedStrings.append(itemText); inStringItem = false
        default: break
        }
    }
    private static func columnIndex(_ reference: String) -> Int {
        var result = 0
        for char in reference.prefix(while: { $0.isLetter }) {
            result = result * 26 + Int(char.uppercased().unicodeScalars.first!.value) - 64
        }
        return max(result - 1, 0)
    }
}

enum EstateXlsx {
    private static let sections = ["overview", "fields", "ages", "soil", "rainfall", "abw", "roads"]
    private static let optionalKeys: Set<String> = ["yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget"]

    static func parse(_ data: Data) throws -> Data {
        guard data.count <= 8 * 1024 * 1024 else { throw ProductionError.invalid("Workbook exceeds 8 MB.") }
        let archive: Archive
        do { archive = try Archive(data: data, accessMode: .read) }
        catch { throw ProductionError.invalid("This is not a valid .xlsx workbook.") }
        var files: [String: Data] = [:]
        var expanded = 0
        for entry in archive {
            if entry.type == .directory { continue }
            guard !entry.path.hasPrefix("/"), !entry.path.contains("../"), files.count < 64 else {
                throw ProductionError.invalid("Invalid workbook contents.")
            }
            var contents = Data()
            _ = try archive.extract(entry) { chunk in
                expanded += chunk.count
                guard expanded <= 8 * 1024 * 1024 else { throw ProductionError.invalid("Workbook expands beyond 8 MB.") }
                contents.append(chunk)
            }
            files[entry.path] = contents
        }
        let shared = try files["xl/sharedStrings.xml"].map { bytes -> [String] in
            let reader = WorkbookXMLReader()
            let parser = XMLParser(data: bytes); parser.delegate = reader
            guard parser.parse() else { throw ProductionError.invalid("Invalid workbook shared strings.") }
            return reader.sharedStrings
        } ?? []
        func sheet(_ n: Int) throws -> [[String]] {
            guard let bytes = files["xl/worksheets/sheet\(n).xml"] else { throw ProductionError.invalid("Workbook is missing sheet \(n).") }
            let reader = WorkbookXMLReader(shared: shared)
            let parser = XMLParser(data: bytes); parser.delegate = reader
            parser.shouldResolveExternalEntities = false
            guard parser.parse() else { throw ProductionError.invalid("Invalid workbook sheet.") }
            return reader.rows
        }
        let metadata = try sheet(1).dropFirst().reduce(into: [String: String]()) { result, row in
            if let key = row.first, row.count > 1 { result[key.trimmingCharacters(in: .whitespacesAndNewlines)] = row[1].trimmingCharacters(in: .whitespacesAndNewlines) }
        }
        guard let revision = Int(metadata["revision"] ?? ""), revision > 0 else {
            throw ProductionError.invalid("Enter a positive whole-number revision on the Metadata sheet.")
        }
        var root: [String: Any] = [
            "schemaVersion": 1, "estateId": metadata["estateId"].flatMap { $0.isEmpty ? nil : $0 } ?? "local-estate",
            "revision": revision, "reportingPeriod": metadata["reportingPeriod"] ?? "",
            "exportedAt": metadata["exportedAt"] ?? ISO8601DateFormatter().string(from: Date())
        ]
        var tables: [[String: Any]] = []
        for (index, name) in sections.enumerated() {
            let rows = try sheet(index + 2)
            guard let header = rows.first, !header.isEmpty, header.allSatisfy({ !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else {
                throw ProductionError.invalid("The \(name) sheet needs nonblank column headings.")
            }
            let values: [[Any]] = rows.dropFirst().filter { $0.contains(where: { !$0.isEmpty }) }.map { row in
                header.indices.map { column in
                    guard row.indices.contains(column), !row[column].isEmpty else { return NSNull() }
                    return row[column]
                }
            }
            tables.append(["id": name, "columns": header, "rows": values])
        }
        root["sections"] = tables
        if files["xl/worksheets/sheet9.xml"] != nil {
            for row in try sheet(9).dropFirst() where row.count > 1 {
                let key = row[0].trimmingCharacters(in: .whitespacesAndNewlines)
                let json = row[1].trimmingCharacters(in: .whitespacesAndNewlines)
                if optionalKeys.contains(key), let value = json.data(using: .utf8),
                   let object = try? JSONSerialization.jsonObject(with: value) {
                    root[key] = object
                }
            }
        }
        return try JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])
    }
    static func template() throws -> Data {
        var sheets: [[[String]]] = [
            [["key", "value"], ["estateId", "local-estate"], ["revision", "1"],
             ["reportingPeriod", "Enter reporting period"], ["exportedAt", ISO8601DateFormatter().string(from: Date())]]
        ]
        sheets += sections.map { _ in [["Value"]] }
        sheets.append([["key", "json"], ["yph", ""], ["cropProductionReport", ""],
                       ["cropProductionReportHistory", ""], ["manuringReport", ""], ["cropBudget", ""]])
        let names = ["Metadata", "Overview", "Fields", "Ages", "Soil", "Rainfall", "Abw", "Roads", "Optional JSON"]
        let archive = try Archive(data: Data(), accessMode: .create)
        func add(_ path: String, _ text: String) throws {
            let bytes = Data(text.utf8)
            try archive.addEntry(with: path, type: .file, uncompressedSize: Int64(bytes.count), provider: { position, size in
                let start = Int(position)
                return bytes.subdata(in: start..<min(start + size, bytes.count))
            })
        }
        var overrides = ""
        var sheetTags = ""
        var rels = ""
        for index in names.indices {
            let number = index + 1
            overrides += "<Override PartName=\"/xl/worksheets/sheet" + String(number) + ".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            sheetTags += "<sheet name=\"" + names[index] + "\" sheetId=\"" + String(number) + "\" r:id=\"rId" + String(number) + "\"/>"
            rels += "<Relationship Id=\"rId" + String(number) + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet" + String(number) + ".xml\"/>"
        }
        try add("[Content_Types].xml", "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" + overrides + "</Types>")
        try add("_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
        try add("xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" + sheetTags + "</sheets></workbook>")
        try add("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" + rels + "</Relationships>")
        for index in sheets.indices {
            var rowsXML = ""
            for rowIndex in sheets[index].indices {
                var cells = ""
                for colIndex in sheets[index][rowIndex].indices {
                    let ref = columnName(colIndex) + String(rowIndex + 1)
                    cells += "<c r=\"" + ref + "\" t=\"inlineStr\"><is><t>" + xmlEscape(sheets[index][rowIndex][colIndex]) + "</t></is></c>"
                }
                rowsXML += "<row r=\"" + String(rowIndex + 1) + "\">" + cells + "</row>"
            }
            try add("xl/worksheets/sheet" + String(index + 1) + ".xml", "<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>" + rowsXML + "</sheetData></worksheet>")
        }
        guard let result = archive.data else { throw ProductionError.invalid("Could not create Excel template.") }
        return result
    }

    private static func columnName(_ index: Int) -> String {
        var n = index + 1
        var result = ""
        while n > 0 {
            let digit = (n - 1) % 26
            result = String(UnicodeScalar(65 + digit)!) + result
            n = (n - 1) / 26
        }
        return result
    }

    private static func xmlEscape(_ value: String) -> String {
        value.replacingOccurrences(of: "&", with: "&amp;").replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;").replacingOccurrences(of: "\"", with: "&quot;")
            .replacingOccurrences(of: "'", with: "&apos;")
    }

}