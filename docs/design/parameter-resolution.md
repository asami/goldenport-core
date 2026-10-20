Parameter Resolution Design
===========================

This document defines the design and responsibility boundaries for
parameter resolution in OperationDefinition.

The goal is to provide a single, consistent mechanism for:
- extracting raw request values
- validating value domains and constraints
- converting values into target runtime types
- supporting applicative-style construction of OperationRequest


----------------------------------------------------------------------
1. Design Principles
----------------------------------------------------------------------

1. Parameter semantics MUST be defined only in ParameterDefinition.
2. Raw protocol values MUST NOT be interpreted outside OperationDefinition.
3. Value conversion MUST occur exactly once, during parameter resolution.
4. Operation-specific logic MUST consume already-resolved values.
5. take_* helpers are convenience views, not conversion engines.


----------------------------------------------------------------------
2. Conceptual Flow
----------------------------------------------------------------------

Protocol-level Request
  └─ raw arguments / switches / properties (String-based)
       ↓
ParameterDefinition
  └─ name, kind, ValueDomain
       ↓
resolveParameter
  ├─ extract raw values
  ├─ validate multiplicity
  ├─ convert to target value (via DataType)
  ├─ validate constraints
       ↓
Resolved Value (e.g. String, Int, ZonedDateTime, etc.)
       ↓
take_* helpers / applicative build
       ↓
Concrete OperationRequest


----------------------------------------------------------------------
3. Responsibility Separation
----------------------------------------------------------------------

### ParameterDefinition

ParameterDefinition is the single source of truth for parameter semantics.

It defines:
- parameter name
- parameter kind (argument / switch / property)
- ValueDomain (datatype, multiplicity, constraints)

ParameterDefinition MUST NOT perform:
- value extraction
- value conversion
- validation logic


### ValueDomain

ValueDomain describes the semantic domain of a parameter.

It contains:
- datatype        (schema.DataType)
- multiplicity    (schema.Multiplicity)
- constraints     (schema.Constraint)

ValueDomain is declarative.
It MUST NOT contain executable logic.
Constraint opacity is a projection concern, not a definition concept.


### DataType (schema)

schema.DataType is a symbolic description of a domain type.

Examples:
- XString
- XInt
- XZonedDateTime

DataType:
- DOES NOT take type parameters
- DOES NOT expose parsing APIs
- DOES NOT perform conversion by itself

DataType acts as a key for selecting conversion logic.


----------------------------------------------------------------------
4. Parameter Resolution
----------------------------------------------------------------------

### resolveParameter

resolveParameter is the only place where:
- raw request values are interpreted
- values are converted into runtime types

Signature (conceptual):

```
resolveParameter(
  param: ParameterDefinition,
  req: Request
): Consequence[Any]
```

Responsibilities:

1. Extract raw values from Request based on parameter kind
2. Enforce multiplicity rules
3. Convert raw values into target values using DataType
4. Validate constraints
5. Return resolved value(s)

Error split:

- FormatError: parsing/type mismatch during normalization
- ValueDomainError: constraint violation (including enum/range)
- Argument.Missing / Argument.Redundant: structural errors


### Value Conversion

Value conversion is driven by DataType.

Conceptually:

```
raw String
  └─ DataType-specific factory
       └─ typed value (e.g. ZonedDateTime)
```

Conversion MAY be implemented using:
- a DataTypeResolver
- a registry of DataTypeFactory instances
- pattern matching on DataType

OperationDefinition MUST NOT depend on concrete conversion mechanisms.


----------------------------------------------------------------------
5. take_* Helpers
----------------------------------------------------------------------

take_* helpers are thin, typed views over resolved values.

Examples:
- take_string
- take_int
- take_datetime

Characteristics:

- take_* helpers MUST assume that resolveParameter already performed:
  - extraction
  - conversion
  - validation
- take_* helpers MAY:
  - cast
  - adapt Option / Vector / NonEmptyVector
- take_* helpers MUST NOT:
  - parse strings
  - validate domains
  - inspect raw Request fields directly


Example (conceptual):

```
protected def take_datetime(
  name: String
)(using req: Request): Consequence[ZonedDateTime]
```

If the resolved value is not compatible with the expected type,
the helper MUST return a failure.


----------------------------------------------------------------------
6. Applicative Construction
----------------------------------------------------------------------

OperationDefinition.createOperationRequest SHOULD use applicative style
to combine resolved parameters.

Example (conceptual):

```
(id, name, birthday).mapN { (id, name, birthday) =>
  CreateUser(id, name, birthday)
}
```

Key properties:

- Order-independent validation
- Error aggregation
- No semantic interpretation outside OperationDefinition


----------------------------------------------------------------------
7. Common Pitfalls (Avoided by This Design)
----------------------------------------------------------------------

- Treating DataType as a generic type constructor (DataType[A])
- Performing parsing inside take_* helpers
- Allowing CLI or resolver layers to interpret parameter semantics
- Mixing raw Request access with resolved values
- Re-validating already resolved parameters


----------------------------------------------------------------------
8. Summary
----------------------------------------------------------------------

- ParameterDefinition defines WHAT a parameter means.
- ValueDomain describes WHERE a parameter lives semantically.
- resolveParameter defines HOW values are extracted, validated, and converted.
- take_* helpers define HOW resolved values are consumed.
- OperationDefinition defines WHEN semantics are applied.

This separation ensures correctness, testability,
and consistency across all protocol front-ends.

## Typed structured collection carriers — 2026-10-19

For canonical `XJson` and `XSemiStructuredData` parameters whose existing
multiplicity permits multiple values, a single extracted value that is exactly
a Scala `Vector` is a collection carrier. Resolution unwraps that one carrier
before the existing absence, single-value and multiple-value branches. Each
element still passes through the canonical scalar normalizer and existing
domain/constraint validation. This accommodates the actual typed protocol
producer without bypassing operation validation or introducing another reader.

An omitted or empty carrier follows existing absence semantics: zero-or-more
resolves to `ResolvedEmpty`; one-or-more fails as missing. A singleton resolves
to `ResolvedSingle`, and ordered multiple elements resolve to `ResolvedMultiple`,
just like the existing repeated-property representation. Range and Ranges
multiplicities use the existing required/multiple predicates; this adjustment
introduces no new count-bound behavior.

A `Json` array remains one document, including under a multiple-value
declaration; it is never interpreted as a Vector carrier. Repeated extracted
property lists are not flattened, nor are nested Vector elements, arbitrary
Seq/Array/NonEmptyVector carriers, scalar or optional parameters, or other
datatypes. Canonical normalization rejects invalid elements and native null.
`Json.Null` remains a valid Json document; a null Record document remains
invalid. Record and object Json elements retain canonical nested Record rules.

[OperationDefinitionResolveParameterSpec](../../src/test/scala/org/goldenport/protocol/OperationDefinitionResolveParameterSpec.scala)
specifies the actual Property carriers, literal resolved values, repeated-property
equivalence and strict rejection boundaries. Its existing integer, enum and range
scenarios continue to describe their original semantics.
