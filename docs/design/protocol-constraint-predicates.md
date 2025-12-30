# Protocol Constraint Predicates
*(OCL-based Standard Predicate Set)*

## Status
- Normative
- Applies to: ParameterDefinition.constraint
- Semantic basis: OCL (Object Constraint Language)

## 1. Purpose
This document defines the standard predicate set used for protocol-level
constraint validation. These predicates are evaluated at the semantic boundary
before OperationRequest creation.

## 2. Semantic Foundation
All constraints are derived from OCL invariants. Multiple predicates are
combined using logical AND.

## 3. String Predicates
Each predicate below is expressed as a constraint on a single string value.

- **notEmpty**
  - OCL: `self.size() > 0`
  - CML: `name notEmpty`
  - Meaning: The string must have at least one character.

- **length >= n**
  - OCL: `self.size() >= n`
  - CML: `name length >= 3`
  - Meaning: The string length must be at least `n`.

- **length > n**
  - OCL: `self.size() > n`
  - CML: `name length > 3`
  - Meaning: The string length must be greater than `n`.

- **length <= n**
  - OCL: `self.size() <= n`
  - CML: `name length <= 20`
  - Meaning: The string length must not exceed `n`.

- **length < n**
  - OCL: `self.size() < n`
  - CML: `name length < 20`
  - Meaning: The string length must be less than `n`.

- **matches <regex>** *(reserved, Phase 2)*
  - OCL: `self.matches(regex)`
  - CML: `name matches "[A-Z][a-z]+"`
  - Meaning: The string must match the regular expression.
  - Status: Reserved for Phase 2.

## 4. Numeric Predicates
Numeric predicates apply to normalized numeric values.

- **>= n (MinInclusive)**
  - OCL: `self >= n`
  - CML: `age >= 0`
  - Meaning: The value must be at least `n`.

- **> n (MinExclusive)**
  - OCL: `self > n`
  - CML: `count > 0`
  - Meaning: The value must be greater than `n`.

- **<= n (MaxInclusive)**
  - OCL: `self <= n`
  - CML: `rate <= 100`
  - Meaning: The value must not exceed `n`.

- **< n (MaxExclusive)**
  - OCL: `self < n`
  - CML: `ratio < 1`
  - Meaning: The value must be less than `n`.

- **in {v1, v2, ...} (Enum)**
  - OCL: `self->includes(v1) or self->includes(v2) ...`
  - CML: `status in {"active", "inactive"}`
  - Meaning: The value must be one of the listed values.

## 5. Combination Rules
All predicates are combined using logical AND. OR, implies, and conditional
constraints are not supported in Phase 1.

Example:
- CML: `age >= 0` and `age <= 120`
- OCL: `self >= 0 and self <= 120`

## 6. Validation Timing
Constraint validation occurs:
- after datatype normalization
- before OperationRequest creation

Violations result in ValueDomainError.

## 7. Projection Rules (Non-normative)
- MCP get_manifest exposes constraints as semantic labels.
- OpenAPI / JSON Schema maps predicates where possible.
- Projections MUST NOT redefine constraint semantics.

## 8. Non-goals
- Cross-parameter constraints
- Conditional constraints
- Runtime or business rules
- Authorization or lifecycle semantics

## 9. Summary
- OCL provides the semantic root.
- CML is a derived, human-readable DSL.
- Validation occurs at the protocol semantic boundary.
- Projections interpret constraints without redefining them.

## Related Documents
- `docs/design/protocol-core.md`
- `docs/design/operation-definition.md`
- `docs/design/parameter-resolution.md`
- `docs/design/protocol-introspection-openapi.md`
- `docs/design/protocol-introspection-mcp.md`
