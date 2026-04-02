# I18nString Exchange Encoding Note

## Status

Design note.
This note defines the intended application-facing convention for converting `I18nString`
and related single-text wrappers to and from exchange-format `String` values.
It is non-normative until promoted into `docs/spec/` and reflected in implementation.

## Intent

Most application storage and interchange paths should be able to store ordinary
single-locale text as a plain string without forcing JSON for every value.
At the same time, the model must preserve explicit locale-bearing or multi-locale
content when the caller needs that fidelity.

The public exchange-format API should therefore use:

- `encode`: object to exchange `String`
- `decode`: exchange `String` to object

Internal implementation may use JSON encoding/decoding where structured form is required.

The underlying library convention is:

- type classes
  - `StringEncoder[A]`
  - `StringDecoder[A]`
  - `StringCodex[A]`
- convenience / marker traits
  - `StringEncodable`
  - `StringDecodable`
  - `StringCodexable`

Code should use type classes for generic library behavior and use the traits for
domain wrappers or runtime dispatch where that is simpler.

## Scope

This rule applies to:

- `org.goldenport.datatype.I18nString`
- wrappers whose semantic payload is an `I18nString`, such as
  - `I18nLabel`
  - `I18nTitle`
  - `I18nBrief`
  - `I18nSummary`
  - `I18nDescription`
  - `I18nText`

## Encoding Rule

`encode` returns a `String`.

### Plain string form

When the value can be represented as a single string in the application locale,
`encode` should return a plain string with no structural wrapper.

This is the preferred representation for the majority of application data.

### Structured JSON form

When the value carries explicit locale information that must be preserved,
or when the value contains multiple locale entries,
`encode` should return a JSON string representation.

The structured form is selected when either of the following holds:

- the value has more than one locale entry
- the value has exactly one locale entry, but that locale differs from the application locale

### Discriminator rule

The encoded string is interpreted by its leading character.

- first character is `{`
  - structured JSON form
- otherwise
  - plain string form

## Decoding Rule

`decode` accepts a `String` and requires `ExecutionContext`.

### Structured JSON input

If the first character is `{`, `decode` must treat the input as JSON and reconstruct
an `I18nString` with the locale entries preserved.

### Plain string input

If the first character is not `{`, `decode` must treat the input as plain text and
construct an `I18nString` whose single locale entry is `ExecutionContext.locale`.

## Escaping Rule

A plain string that must literally begin with `{` cannot be left ambiguous.

The escape rule is:

- if the plain text begins with `{`, encode it as `\{...`
- during decode, if the input begins with `\{`, remove the leading backslash and treat the result as plain text

Examples:

- `Reader`
  - plain string
- `\{literal`
  - decoded as plain string `{literal`
- `{"entries":[["en","Reader"],["ja","読者"]]}`
  - decoded as structured `I18nString`

## Locale Preservation Rule

If an application needs to persist a single-locale value whose locale differs from
`ExecutionContext.locale`, the encoded form must be the structured JSON form,
even if there is only one entry.

This prevents silent reinterpretation on decode.

## Consequences

This convention gives the following behavior:

- ordinary text stays small and readable in storage
- locale-bearing values remain lossless when needed
- decode behavior is deterministic
- locale interpretation is explicit at decode time through `ExecutionContext`

## Recommended API Shape

Recommended public API naming:

- `encode(using ExecutionContext): String`
- `decode(p: String)(using ExecutionContext): Consequence[I18nString]`

Recommended library shape:

- `given StringCodex[I18nString]`
- `I18nString extends StringCodexable`

Wrapper types may provide the corresponding forms:

- `I18nTitle.encode(using ExecutionContext): String`
- `I18nTitle.decode(p: String)(using ExecutionContext): Consequence[I18nTitle]`

## Relationship To Record Encoding

This note concerns exchange-format `String` encoding only.
It does not replace:

- `toRecord/fromRecord`
- `ValueReader`
- `Recordable`
- `Record.toJsonString`

Those remain the primary conventions for structured interchange inside the stack.

This note defines the compact string-level convention to use when a field,
column, or protocol slot is intentionally string-based.

## Relationship To SQL Storage

When a datastore column is intentionally string-based, runtime code may treat
`StringEncodable` as the generic hook for compact exchange-format persistence.

This allows:

- model code to implement `encode/decode` through `StringCodex`
- runtime code such as SQL datastores to persist compact string values without
  hard-coding every concrete `I18n*` wrapper type
