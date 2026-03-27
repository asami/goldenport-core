# Record Multi-Format Decoder Consolidation

## Purpose

This note proposes a clear ownership boundary for `Record` decoding across the stack.

The goal is to consolidate `json / yaml / tsl / csv -> Record` decoding in `simplemodeling-lib` so that higher layers do not maintain parallel format-specific parsing logic.

## Problem

The current situation is inconsistent.

- `simplemodeling-lib` already provides `json -> Record` and `yaml -> Record`.
- CNCF startup import currently parses YAML directly instead of delegating to core.
- Cozy has format-handling responsibilities that should not live in the generator layer.
- `csv -> Record` and `tsl -> Record` are not yet provided by core.

This creates three risks:

1. format semantics drift across repositories
2. duplicated parser behavior and duplicated bug fixes
3. format support expansion becoming expensive and inconsistent

## Direction

Move all input-side structured text decoding into core.

### Core responsibility

`simplemodeling-lib` should own:

- `json -> Record`
- `yaml -> Record`
- `tsl -> Record`
- `csv -> Record`

and, where appropriate:

- `json -> Vector[Record]`
- `yaml -> Vector[Record]`
- `tsl -> Vector[Record]`
- `csv -> Vector[Record]`

Core is the correct owner because format decoding is not application logic, not generator logic, and not CNCF runtime policy.

### Cozy responsibility

Cozy should consume the core decoder and support:

- JSON
- YAML
- TSL
- CSV

Cozy should not maintain a separate record-decoding implementation when the same semantics already belong to core.

If Cozy currently has YAML-specific parsing logic that is effectively `structured text -> Record`, that logic should be migrated into core and then reused from Cozy.

### CNCF responsibility

CNCF should not interpret structured text formats directly for import.

CNCF should:

- detect source type
- call the core decoder
- map decoded `Record` values into datastore/entitystore import flow

This keeps CNCF focused on runtime import semantics rather than format parsing.

## API Direction

The current single-record API is not enough for all formats.

`json` and `yaml` often map naturally to a single top-level object, but `csv` and `tsl` naturally map to multiple rows/entries.

For that reason, core should define an explicit multi-record decode surface.

Suggested direction:

- keep `RecordDecoder` for single-record decoding where it is meaningful
- add a companion decoder for plural decoding, for example:
  - `RecordsDecoder`
  - or equivalent API on `RecordDecoder`

The important point is semantic clarity:

- single object/document -> `Record`
- tabular/list-oriented document -> `Vector[Record]`

The API must not force CSV or TSL through an unnatural single-record abstraction.

## CSV Semantics

Initial CSV support should be intentionally small and deterministic.

### Supported shape

- first row is the header row
- each subsequent row becomes one `Record`
- each column name becomes a field name

Example:

```text
id,name,age
p1,taro,20
p2,hanako,30
```

becomes:

```text
Vector(
  Record(id = "p1", name = "taro", age = "20"),
  Record(id = "p2", name = "hanako", age = "30")
)
```

### Initial simplifications

The first step may keep field values as strings unless there is already a stable core convention for scalar coercion.

This keeps the first implementation predictable.

## TSL Semantics

TSL support must be defined explicitly before implementation.

The essential design question is:

- what TSL shape maps to one `Record`
- what TSL shape maps to `Vector[Record]`

The first implementation should choose one narrow, deterministic TSL profile and document it clearly.

Do not introduce a broad or ambiguous TSL interpretation in the first step.

## Migration Guidance

### Step 1

Implement in core:

- `csv -> Vector[Record]`
- `tsl -> Vector[Record]`
- if needed, `yaml -> Vector[Record]` for list-oriented YAML documents

### Step 2

Move Cozy record-oriented YAML handling to core usage.

### Step 3

Refactor CNCF startup import so that:

- YAML direct parsing is removed from CNCF
- all supported formats are decoded through core

## Non-Goals

This note does not define:

- domain-level type decoding
- entity decoding from `Record`
- datastore/entitystore import policy
- format rendering for final output
- broad schema inference

Those remain separate concerns.

## Expected Outcome

After consolidation:

- core owns structured text to `Record` semantics
- Cozy reuses core instead of maintaining parallel parsers
- CNCF import becomes format-agnostic above the decoder boundary
- future format additions are made once in core and reused consistently
