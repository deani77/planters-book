import Foundation
import ZIPFoundation

private final class WorkbookXMLReader: NSObject, XMLParserDelegate {
    private(set) var rows: [[String]] = []
    private(set) var sharedStrings: [String]
    private(set) var elements: [(String, [String: String])] = []
    private(set) var failure: String?
    private var row: [Int: String]?
    private var column = 0
    private var nextColumn = 0
    private var cellType = ""
    private var cellText = ""
    private var capture = false
    private var inStringItem = false
    private var itemText = ""
    private var inValue = false
    private var hasFormula = false

    init(shared: [String] = []) { sharedStrings = shared }
    private func fail(_ parser: XMLParser, _ message: String) {
        failure = message
        parser.abortParsing()
    }
    func parser(_ parser: XMLParser, didStartElement name: String, namespaceURI: String?, qualifiedName qName: String?, attributes: [String: String] = [:]) {
        let tag = name.split(separator: ":").last.map(String.init) ?? name
        switch tag {
        case "sheet", "Relationship":
            let normalized = Dictionary(attributes.map { (String($0.key.split(separator: ":").last ?? Substring($0.key)), $0.value) }, uniquingKeysWith: { first, _ in first })
            elements.append((tag, normalized))
        case "row": row = [:]; nextColumn = 0
        case "c":
            if let ref = attributes["r"] {
                guard let col = Self.columnIndex(ref) else { fail(parser, "Invalid workbook cell reference."); return }
                column = col
            } else { column = nextColumn }
            nextColumn = column + 1
            cellType = attributes["t"] ?? ""
            cellText = ""
            hasFormula = false
        case "f": hasFormula = true
        case "si": inStringItem = true; itemText = ""
        case "v", "t": capture = true; inValue = tag == "v"
        default: break
        }
    }
    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if capture { cellText += string }
        if inStringItem && capture && !inValue { itemText += string }
    }
    func parser(_ parser: XMLParser, didEndElement name: String, namespaceURI: String?, qualifiedName qName: String?) {
        let tag = name.split(separator: ":").last.map(String.init) ?? name
        switch tag {
        case "v", "t": capture = false; inValue = false
        case "c":
            guard cellType != "e", cellType != "b", !hasFormula || !cellText.isEmpty else {
                fail(parser, "Use plain values; the workbook contains an error or an uncalculated formula."); return
            }
            let value: String
            if cellType == "s" {
                guard let index = Int(cellText), sharedStrings.indices.contains(index) else {
                    fail(parser, "Invalid shared string reference."); return
                }
                value = sharedStrings[index]
            } else { value = cellText }
            guard column < 20 || value.isEmpty else { fail(parser, "Estate tables support at most 20 columns."); return }
            if !value.isEmpty {
                guard row?[column] == nil else { fail(parser, "Duplicate cell reference."); return }
                row?[column] = value
            }
        case "row":
            if let row {
                guard rows.count <= 10000 else { fail(parser, "Too many workbook rows."); return }
                let lastColumn = row.keys.max() ?? -1
                rows.append(lastColumn >= 0 ? (0...lastColumn).map { row[$0] ?? "" } : [])
            }
            row = nil
        case "si": sharedStrings.append(itemText); inStringItem = false
        default: break
        }
    }
    private static func columnIndex(_ reference: String) -> Int? {
        let bytes = Array(reference.utf8)
        let letters = bytes.prefix { (65...90).contains($0) }
        let digits = bytes.dropFirst(letters.count)
        guard (1...3).contains(letters.count), !digits.isEmpty, digits.allSatisfy({ (48...57).contains($0) }) else { return nil }
        var result = 0
        for byte in letters { result = result * 26 + Int(byte) - 64 }
        return (1...16384).contains(result) ? result - 1 : nil
    }
}

enum EstateXlsx {
    private static let sections = ["overview", "fields", "ages", "soil", "rainfall", "abw", "roads"]
    private static let optionalKeys: Set<String> = ["yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget"]

    static func readEstate(_ data: Data) throws -> Data {
        guard data.count <= 8 * 1024 * 1024 else { throw ProductionError.invalid("Estate file exceeds 8 MB.") }
        return data.prefix(2) == Data([80, 75]) ? try parse(data) : data
    }

    private static func xml(_ data: Data, shared: [String] = []) throws -> WorkbookXMLReader {
        guard let decoded = String(data: data, encoding: .utf8), !decoded.contains("\0") else { throw ProductionError.invalid("Use UTF-8 workbook XML.") }
        let text = decoded.uppercased()
        guard !text.contains("<!DOCTYPE"), !text.contains("<!ENTITY") else { throw ProductionError.invalid("Workbook XML declarations are not supported.") }
        let reader = WorkbookXMLReader(shared: shared)
        let parser = XMLParser(data: data)
        parser.delegate = reader
        parser.shouldResolveExternalEntities = false
        guard parser.parse(), reader.failure == nil else { throw ProductionError.invalid(reader.failure ?? "Invalid workbook XML.") }
        return reader
    }

    static func parse(_ data: Data) throws -> Data {
        guard data.count <= 8 * 1024 * 1024 else { throw ProductionError.invalid("Workbook exceeds 8 MB.") }
        let archive: Archive
        do { archive = try Archive(data: data, accessMode: .read) }
        catch { throw ProductionError.invalid("This is not a valid .xlsx workbook.") }
        var files: [String: Data] = [:]
        var expanded = 0
        var count = 0
        for entry in archive {
            count += 1
            guard count <= 128, !entry.path.hasPrefix("/"), !entry.path.split(separator: "/").contains(".."), !entry.path.contains("\\") else {
                throw ProductionError.invalid("Invalid workbook contents.")
            }
            if entry.type == .directory { continue }
            guard entry.type == .file, files[entry.path] == nil else { throw ProductionError.invalid("Duplicate or invalid workbook part.") }
            var contents = Data()
            _ = try archive.extract(entry) { chunk in
                expanded += chunk.count
                guard expanded <= 8 * 1024 * 1024 else { throw ProductionError.invalid("Workbook expands beyond 8 MB.") }
                contents.append(chunk)
            }
            files[entry.path] = contents
        }
        func part(_ name: String) throws -> Data {
            guard let bytes = files[name] else { throw ProductionError.invalid("Workbook is missing \(name).") }
            return bytes
        }
        var relationships: [String: [String: String]] = [:]
        for (_, attributes) in try xml(part("xl/_rels/workbook.xml.rels")).elements {
            guard let id = attributes["Id"], relationships[id] == nil else { throw ProductionError.invalid("Duplicate worksheet relationship.") }
            relationships[id] = attributes
        }
        var sheets: [String: String] = [:]
        for (_, attributes) in try xml(part("xl/workbook.xml")).elements {
            let name = (attributes["name"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if sections.contains(name) || name == "metadata" || name == "optional json" {
                guard sheets[name] == nil, let id = attributes["id"], let relationship = relationships[id],
                      relationship["TargetMode"] != "External", relationship["Type"]?.hasSuffix("/worksheet") == true,
                      let target = relationship["Target"], !target.isEmpty, !target.contains("\\"), !target.split(separator: "/").contains("..") else {
                    throw ProductionError.invalid("Invalid worksheet relationship.")
                }
                let path = target.hasPrefix("/") ? String(target.dropFirst()) : "xl/" + (target.hasPrefix("./") ? String(target.dropFirst(2)) : target)
                guard path.hasPrefix("xl/") else { throw ProductionError.invalid("Invalid worksheet target.") }
                sheets[name] = path
            }
        }
        let shared = try files["xl/sharedStrings.xml"].map { try xml($0).sharedStrings } ?? []
        func sheet(_ name: String) throws -> [[String]] {
            guard let path = sheets[name] else { throw ProductionError.invalid("Workbook is missing the \(name) sheet.") }
            return try xml(part(path), shared: shared).rows
        }
        func hasData(_ row: [String]) -> Bool { row.contains { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty } }
        var metadata: [String: String] = [:]
        for row in try sheet("metadata").dropFirst().filter(hasData) {
            let key = (row.first ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            guard !key.isEmpty, row.count <= 2, metadata[key] == nil else { throw ProductionError.invalid("Duplicate or invalid metadata key.") }
            metadata[key] = row.count > 1 ? row[1].trimmingCharacters(in: .whitespacesAndNewlines) : ""
        }
        guard let revision = Int(metadata["revision"] ?? ""), revision > 0 else {
            throw ProductionError.invalid("Enter a positive whole-number revision on the Metadata sheet.")
        }
        var root: [String: Any] = [
            "schemaVersion": 1, "estateId": metadata["estateId"].flatMap { $0.isEmpty ? nil : $0 } ?? "local-estate",
            "revision": revision, "reportingPeriod": metadata["reportingPeriod"] ?? "",
            "exportedAt": metadata["exportedAt"].flatMap { $0.isEmpty ? nil : $0 } ?? ISO8601DateFormatter().string(from: Date())
        ]
        var tables: [[String: Any]] = []
        for name in sections {
            let rows = try sheet(name)
            guard let rawHeader = rows.first else { throw ProductionError.invalid("The \(name) sheet needs column headings.") }
            let header = rawHeader.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            guard !header.isEmpty, header.allSatisfy({ !$0.isEmpty }) else { throw ProductionError.invalid("The \(name) sheet has blank headings.") }
            var values: [[Any]] = []
            for row in rows.dropFirst().filter(hasData) {
                guard row.count <= header.count else { throw ProductionError.invalid("The \(name) sheet has values outside the headings.") }
                values.append(header.indices.map { col -> Any in
                    guard row.indices.contains(col), !row[col].isEmpty else { return NSNull() }
                    return row[col]
                })
            }
            tables.append(["id": name, "columns": header, "rows": values])
        }
        root["sections"] = tables
        if sheets["optional json"] != nil {
            var seen: Set<String> = []
            for row in try sheet("optional json").dropFirst().filter(hasData) {
                let key = (row.first ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                guard row.count <= 2, optionalKeys.contains(key), seen.insert(key).inserted else { throw ProductionError.invalid("Unknown or duplicate optional report key.") }
                let json = row.count > 1 ? row[1].trimmingCharacters(in: .whitespacesAndNewlines) : ""
                if !json.isEmpty {
                    guard let bytes = json.data(using: .utf8) else { throw ProductionError.invalid("Invalid optional report JSON.") }
                    let object = try JSONSerialization.jsonObject(with: bytes)
                    guard (key == "cropProductionReportHistory" ? object is [Any] : object is [String: Any]) else { throw ProductionError.invalid("Invalid optional report shape.") }
                    root[key] = object
                }
            }
        }
        let result = try JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])
        _ = try EstateReference.parse(result)
        return result
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
