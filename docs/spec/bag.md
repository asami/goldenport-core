Bag Specification
=================

This document specifies the semantics and contracts of the **Bag** abstraction.

This specification is a concrete application of the general rules defined in
RULE.md, especially regarding:

- Builder semantics
- Error modeling (Consequence vs exceptions)
- Dual-API rules
- Quasi-Error I/O handling

Unless otherwise stated, sections marked as **Stable Semantics** are normative.
Sections marked as **Design Notes** are non-normative and may change.

----------------------------------------------------------------------
Stable Semantics
----------------------------------------------------------------------

Overview
--------

A **Bag** represents a container of binary data with well-defined lifecycle
and interpretation semantics.

A Bag is primarily intended as:

- A temporary storage container
- A value-object component in domain models
- A boundary-safe representation of external data

Bag itself does **not** imply text semantics.

----------------------------------------------------------------------
Core Types
----------------------------------------------------------------------

Bag
---

- Represents a container of binary data
- Provides binary-level access (streams, bytes)
- Does **not** assume any character encoding

Bag is intentionally neutral with respect to interpretation.

TextBag
-------

A **TextBag** is a Bag whose text semantics are explicitly fixed.

Guarantees:

- A Charset is always defined
- Text interpretation is stable and reproducible
- Charset inference is **never** performed

Example shape:

trait TextBag extends Bag {
  def charset: Charset
}

BinaryBag
---------

A **BinaryBag** is a Bag that explicitly declares the absence of text semantics.

Guarantees:

- Text interpretation is intentionally forbidden
- No charset is associated
- Binary operations remain available

BinaryBag exists purely as a **semantic marker**.

----------------------------------------------------------------------
Builder Semantics
----------------------------------------------------------------------

BagBuilder
----------

Bag instances are constructed using **BagBuilder**.

BagBuilder accumulates binary data and finalizes construction
using build-family methods.

build()
-------

def build(): Consequence[Bag]

Semantics:

- Finalizes the builder according to its configuration
- May return:
  - Bag
  - TextBag (if text semantics are configured)
- Does **not** guarantee text semantics

Failure model:

- Uses Consequence
- I/O failures that indicate system corruption may throw exceptions
  (Quasi-Error I/O rule)

buildText()
-----------

def buildText(): Consequence[TextBag]

buildText() is a **semantic finalization operation**.

Exact Semantics:

- Explicitly requires text semantics
- Uses the builder configuration as-is
- Performs **no charset inference**
- Does **not** reinterpret or auto-promote

Success Condition:

- The underlying build() result is a TextBag

Failure Condition:

- Text semantics are not defined
- Charset is not explicitly configured
- Builder is configured for binary-only output

Failure is reported as Consequence failure.

----------------------------------------------------------------------
Charset Handling Policy
----------------------------------------------------------------------

- Charset inference is **forbidden**
- Platform default charset must never be used implicitly
- Charset must be fixed at build time

This rule exists to prevent silent data corruption
(e.g. Shift-JIS interpreted as UTF-8).

----------------------------------------------------------------------
Promotion Rules
----------------------------------------------------------------------

Bag → BinaryBag
---------------

def promoteToBinary(): BinaryBag

- Always succeeds
- Narrows semantics
- Does not lose information

This promotion is safe and unconditional.

Bag → TextBag
-------------

Promotion from Bag to TextBag is **deliberate and potentially failing**.

Rules:

- Requires explicit charset
- Must use Consequence
- Must not infer or guess encoding

----------------------------------------------------------------------
Error Model
----------------------------------------------------------------------

Quasi-Error I/O
---------------

The following failures are treated as unrecoverable and may throw exceptions:

- Temporary files created by the same process cannot be opened
- Process-owned paths suddenly become inaccessible
- Internal streams fail under normal conditions

These failures indicate environmental corruption rather than domain errors.

Dual-API Rule
-------------

Operations that almost always succeed but may fail
(e.g. text decoding) may expose:

def toText: String
def toTextC: Consequence[String]

This rule applies primarily to TextBag.

----------------------------------------------------------------------
Design Notes (Non-Normative)
----------------------------------------------------------------------

Why Bag Does Not Imply Text
--------------------------

Many external inputs are ambiguous:

- Files without encoding metadata
- Network payloads
- Mixed binary/text formats

Requiring explicit text semantics avoids accidental misinterpretation.

Promotion Ergonomics
--------------------

APIs such as:

bag.promoteToText(charset).toText

may appear frequently in practice.

Whether convenience APIs should be added is intentionally deferred
until real usage patterns are observed.

Open Questions
--------------

- Should Bag.toText exist as a convenience?
- Should buildBinary() be introduced?
- Should TextBag promotion be discouraged outside builders?

These questions are intentionally left open.
