import Foundation
import ZIPFoundation

func unpack(_ data: Data) throws -> [String: Data] {
    let archive = try Archive(data: data, accessMode: .read)
    var files: [String: Data] = [:]
    for entry in archive {
        var data = Data()
        _ = try archive.extract(entry) { data.append($0) }
        files[entry.path] = data
    }
    return files
}
func pack(_ files: [String: Data]) throws -> Data {
    let archive = try Archive(data: Data(), accessMode: .create)
    for (path, bytes) in files {
        try archive.addEntry(with: path, type: path.hasSuffix("/") ? .directory : .file, uncompressedSize: Int64(bytes.count)) { position, size in
            let start = Int(position)
            return bytes.subdata(in: start..<min(start + size, bytes.count))
        }
    }
    return archive.data!
}
func sheet(_ rows: [[String]]) -> Data {
    func escape(_ value: String) -> String { value.replacingOccurrences(of: "&", with: "&amp;").replacingOccurrences(of: "<", with: "&lt;") }
    let xml = rows.enumerated().map { r, row in
        "<row>" + row.enumerated().map { c, value in
            "<c r=\"" + String(UnicodeScalar(65 + c)!) + String(r + 1) + "\" t=\"inlineStr\"><is><t>" + escape(value) + "</t></is></c>"
        }.joined() + "</row>"
    }.joined()
    return Data(("<worksheet><sheetData>" + xml + "</sheetData></worksheet>").utf8)
}
func parsed(_ files: [String: Data]) throws -> EstateReference { try EstateReference.parse(EstateXlsx.readEstate(pack(files))) }
func rejects(_ files: [String: Data]) {
    do { _ = try parsed(files); fatalError("Invalid workbook accepted") } catch {}
}
let template = try EstateXlsx.template()
let base = try unpack(template)
var files = base
files["xl/"] = Data()
let blank = try parsed(files)
precondition(blank.sections.count == 7 && blank.sections.allSatisfy { $0.rows.isEmpty })
let plainJSON = try EstateXlsx.parse(template)
let detectedJSON = try EstateXlsx.readEstate(plainJSON)
precondition(detectedJSON == plainJSON)

files = base
files["xl/worksheets/custom-overview.xml"] = sheet([["Item", "Value"], ["Zero", "0"], ["Pending", ""]])
files.removeValue(forKey: "xl/worksheets/sheet2.xml")
let relPath = "xl/_rels/workbook.xml.rels"
files[relPath] = Data(String(decoding: files[relPath]!, as: UTF8.self).replacingOccurrences(of: "worksheets/sheet2.xml", with: "worksheets/custom-overview.xml").utf8)
let overview = try parsed(files).sections.first { $0.id == "overview" }!
precondition(overview.rows[0][1]?.text == "0" && overview.rows[1][1] == nil)

files = base
files["xl/sharedStrings.xml"] = Data("<sst><si><t>Value</t></si><si><r><t>Estate </t></r><r><t>name</t></r></si></sst>".utf8)
files["xl/worksheets/sheet2.xml"] = Data("<worksheet><sheetData><row><c r=\"A1\" t=\"s\"><v>0</v></c></row><row><c r=\"A2\" t=\"s\"><v>1</v></c></row></sheetData></worksheet>".utf8)
let shared = try parsed(files)
precondition(shared.sections[0].rows[0][0]?.text == "Estate name")
files["xl/worksheets/sheet2.xml"] = Data(String(decoding: files["xl/worksheets/sheet2.xml"]!, as: UTF8.self).replacingOccurrences(of: "<v>1</v>", with: "<v>99</v>").utf8)
rejects(files)

files = base
files["xl/worksheets/sheet9.xml"] = sheet([["key", "json"], ["manuringReport", "{\"reportDate\":\"2026-10-01\",\"appliedYtdMt\":10}"]])
let report = try parsed(files)
precondition(report.manuringReport?.appliedYtdMt == 10)
files["xl/worksheets/sheet9.xml"] = sheet([["key", "json"], ["manuringReport", "{broken"]])
rejects(files)
files = base
files["xl/worksheets/sheet2.xml"] = sheet([["Value"], ["A", "Unexpected"]])
rejects(files)
files["xl/worksheets/sheet2.xml"] = Data("<worksheet><sheetData><row><c r=\"AAAAAAAAAAAA1\"><v>1</v></c></row></sheetData></worksheet>".utf8)
rejects(files)
files["xl/worksheets/sheet2.xml"] = Data("<!DOCTYPE worksheet [<!ENTITY x 'text'>]><worksheet/>".utf8)
rejects(files)
print("XLSX round-trip, worksheet mapping, zero/null preservation and rejected-input checks passed")
