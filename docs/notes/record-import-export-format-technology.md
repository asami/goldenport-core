# Record Import / Export Format Technology

## Summary

This note records the `simplemodeling-lib` side of Record import/export
technology. The library owns format decoding and rendering. Application and CNCF
runtime policy should consume these APIs instead of maintaining parallel format
parsers.

`Record` is a semi-structured tree exchange unit. It can represent flat rows and
nested objects.

## Record Shape

A `Record` may contain:

- scalar values;
- nested `Record` values;
- `Vector[Record]`;
- scalar vectors.

This makes `Record` suitable for tree-shaped data from JSON, YAML, HOCON, XML,
Excel workbooks, and service payloads. It is not limited to flat CSV-like rows.

Flat formats can still produce nested Records when schema or header metadata
uses paths such as `identifiers.isbn13`, `publication.date`, or
`contributors.authors`.

## Import Formats

Supported or intended import formats:

- CSV;
- TSV;
- LTSV;
- line-delimited text;
- JSON;
- YAML;
- XML;
- HOCON;
- Excel.

CSV, TSV, and LTSV are tabular formats. Field names come from header metadata or
schema. If no header or schema is available, decoding should fail
deterministically.

Line-delimited text uses a configured field name. For book import, TKE can use
`isbn` as the line field.

JSON, YAML, XML, and HOCON are self-describing enough to become Records without
a schema. Schema remains useful for later shaping, field filtering, ordering,
and scalar coercion.

Excel v1 treats one worksheet as `Vector[Record]`. `.xlsx` and `.xls` import are
accepted through Apache POI. The first sheet is used by default, and callers may
select a sheet name. Header rows work like CSV/TSV headers; headerless sheets
require schema column order. Cell values are read through POI `DataFormatter` so
the user-visible value is preserved, with schema coercion applied only when the
caller asks for it. Workbook-level tree mapping can be added later using sheet
names, named ranges, metadata sheets, or explicit schema rules.

## Export Formats

Supported or intended export formats:

- CSV;
- TSV;
- LTSV;
- JSON;
- YAML;
- XML;
- HOCON;
- Excel.

Export accepts `Record` or `Vector[Record]`. Tree-preserving formats should keep
nested Record structure where possible. Tabular formats should flatten or shape
the data according to schema/profile rules supplied by the caller.

Excel export produces `.xlsx`. V1 writes a single `records` worksheet from
`Vector[Record]`; schema or top-level field union determines the columns.
Nested `Record` values and arrays are serialized as compact JSON cells. Later
versions can write multiple sheets for nested collections.

## Schema And Header Metadata

Schema is required for ambiguous tabular input when there is no header. Schema
is optional for self-describing formats.

Schema may be used for all formats to:

- resolve field names, normalized names, and labels;
- coerce scalar values;
- select or order fields;
- decide whether unknown fields are kept, dropped, or reported as issues;
- map flat paths into nested Records.

CSV/TSV/LTSV header metadata may provide:

- field names;
- source labels;
- simple type annotations;
- preamble metadata such as `# key: value` or `# @key: value`.

Conflicts between schema and header metadata should be reported as issues when
possible rather than silently changing row data.

## Google Spreadsheet

Google Spreadsheet is not a file format owned by `simplemodeling-lib`; it is an
external service source and destination.

The library may provide Record import/export primitives that a Google provider
uses after it retrieves sheet values. It should not depend on Google APIs.

The intended service integration is a CNCF provider implemented by
`textus-google`, returning `Vector[Record]` for import and accepting
`Vector[Record]` for export.

## Implemented API

- `RecordImportDecoder.decodeBytes` and `decodePath` handle binary Excel input.
- `RecordImportFormat.Excel` and `RecordFormat.Excel` are the canonical format
  values.
- `RecordExportEncoder` renders `Vector[Record]` to text formats or `.xlsx`
  bytes.

## References

- `docs/journal/2026/02/tree-record-definition.md`
- `docs/journal/2026/03/record-multiformat-decoder-consolidation-2026-03-27.md`
