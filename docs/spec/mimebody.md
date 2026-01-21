======================================================================
MimeBody Specification
======================================================================

status = design-fixed
since  = Phase 2.8

----------------------------------------------------------------------
Overview
----------------------------------------------------------------------

MimeBody is a DataType that represents typed materialized data
intended for external exposure (HTTP, CLI, Mail, File, etc.).

MimeBody combines:

- ContentType: external contract (how data is exposed)
- Bag: internal materialized data

This specification defines the normative semantics of MimeBody,
especially the charset handling rules between internal storage
and external representation.


----------------------------------------------------------------------
MimeBody
----------------------------------------------------------------------

MimeBody consists of:

- contentType : ContentType
- value   : Bag

ContentType defines how the data SHALL be exposed externally.
Bag contains the actual materialized data and its internal metadata.


----------------------------------------------------------------------
ContentType
----------------------------------------------------------------------

ContentType represents an external contract.

- mimeType  : MimeType
- charset   : Optional external charset contract
- parameters: Optional MIME parameters

ContentType is authoritative for external interpretation.
When specified, ContentType information MUST be respected
by all adapters and output layers.


----------------------------------------------------------------------
Bag and BagMetadata
----------------------------------------------------------------------

Bag represents internally stored materialized data.

BagMetadata may include descriptive information such as:

- name
- mimeType
- charset
- size

BagMetadata is descriptive only and MUST NOT be treated
as an external contract.


----------------------------------------------------------------------
Charset Semantics
----------------------------------------------------------------------

CNCF separates internal encoding from external exposure.

Although both BagMetadata and ContentType may contain charset
information, their semantics are different.


----------------------------------------------------------------------
BagMetadata.charset (Internal Encoding)
----------------------------------------------------------------------

BagMetadata.charset represents the actual charset of the data
stored inside the Bag.

- Internal factual information
- Derived from storage or I/O
- NOT an external contract
- May be undefined (None)

BagMetadata.charset MUST NOT determine external representation.


----------------------------------------------------------------------
ContentType.charset (External Contract)
----------------------------------------------------------------------

ContentType.charset represents the charset used when data
is exposed externally.

- API / Argument / output contract
- Explicitly specified by the caller
- Highest priority for output decisions
- charset = None means no external charset contract

ContentType.charset is the only authoritative source
for external charset decisions.


----------------------------------------------------------------------
Priority Rule
----------------------------------------------------------------------

When both BagMetadata.charset and ContentType.charset are present,
ContentType.charset ALWAYS takes precedence.

BagMetadata.charset remains internal reference information only.


----------------------------------------------------------------------
Charset Conversion Semantics
----------------------------------------------------------------------

Charset conversion SHALL occur only when all conditions below hold:

1. ContentType.charset is defined
2. BagMetadata.charset is defined
3. The two charsets are different

Conversion behavior:

- Decode data using BagMetadata.charset
- Encode data using ContentType.charset
- Output the converted data externally


----------------------------------------------------------------------
Example (Maximal Case)
----------------------------------------------------------------------

- Internal data stored in EUC-JP
- External contract specifies Shift_JIS

MimeBody(
  contentType = ContentType(charset = Shift_JIS),
  value   = BinaryBag(metadata.charset = EUC_JP)
)

When toBinary or external output is requested:

- Decode using EUC-JP
- Encode using Shift_JIS
- Output Shift_JIS binary


----------------------------------------------------------------------
Non-goals
----------------------------------------------------------------------

- No implicit charset conversion
- No charset inference
- No silent fallback on conversion failure


----------------------------------------------------------------------
Error Handling
----------------------------------------------------------------------

Charset conversion failures MUST be surfaced explicitly
(exception or Consequence.Failure).

Silent or heuristic recovery is forbidden.

This is especially important in Japanese environments.


----------------------------------------------------------------------
Design Rationale
----------------------------------------------------------------------

- Clear separation of internal storage and external contract
- Consistent semantics across HTTP, CLI, Mail, and File
- Correct behavior even in maximal edge cases
- Long-term maintainability and extensibility
