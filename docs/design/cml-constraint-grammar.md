# CML Constraint Grammar
*(OCL-derived Constraint DSL for Parameters)*

## Status
- Normative
- Applies to: ParameterDefinition.constraint
- Semantic basis: OCL (Object Constraint Language)
- Predicate catalog: `docs/design/protocol-constraint-predicates.md`

## 1. Purpose
This document defines the formal grammar of CML constraints. CML constraints
are execution-free, evaluated before OperationRequest creation, and derived
from OCL invariants.

## 2. Design Principles
- The block form is canonical.
- The inline form is syntactic sugar.
- Phase 1 supports AND-only composition.
- OR, implies, and conditional constraints are not supported in Phase 1.

## 3. Canonical Syntax
Canonical constraints are defined as a block attached to a parameter and
contain one or more predicate statements.

Example (block form):

```
constraint {
  notEmpty
  length >= 3
}
```

## 4. Inline Syntax (Syntactic Sugar)
Inline constraints are a compact form that MUST be normalized to the block
form. Inline predicates are equivalent to a block with the same predicate
sequence.

Example (inline form):

```
name notEmpty length >= 3
```

## 5. Grammar (EBNF)

```
Constraint         ::= ConstraintBlock | ConstraintInline

ConstraintBlock    ::= "constraint" "{" PredicateStmt+ "}"
ConstraintInline   ::= Identifier PredicateStmt+

PredicateStmt      ::= PredicateExpr
PredicateExpr      ::= StringPredicate
                     | NumericPredicate
                     | EnumPredicate
                     | ReservedPredicate

StringPredicate    ::= "notEmpty"
                     | "length" RelOp IntegerLiteral

NumericPredicate   ::= RelOp NumberLiteral

EnumPredicate      ::= "in" "{" EnumLiteral ("," EnumLiteral)* "}"

ReservedPredicate  ::= "matches" RegexLiteral
```

## 6. Predicate Forms (Phase 1)

Supported predicates:

- `notEmpty`
  - Example: `name notEmpty`

- Length predicates
  - Example: `name length >= 3`
  - Example: `title length < 64`

- Numeric relational predicates
  - Example: `age >= 0`
  - Example: `count < 10`

- Enum predicates
  - Example: `status in {"active", "inactive"}`

Reserved (Phase 2):

- `matches <regex>`
  - Example: `code matches /[A-Z]{3}/`

## 7. Lexical Elements

Relational operators:

```
RelOp ::= ">=" | ">" | "<=" | "<"
```

Numeric literals:

```
NumberLiteral  ::= IntegerLiteral | DecimalLiteral
IntegerLiteral ::= Digit+
DecimalLiteral ::= Digit+ "." Digit+
Digit          ::= "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
```

String literals:

```
StringLiteral ::= "\"" Char* "\""
```

Enum literals:

```
EnumLiteral ::= StringLiteral | Identifier
```

Regex literals:

```
RegexLiteral ::= "/" RegexChar* "/"
```

Identifiers:

```
Identifier ::= Letter (Letter | Digit | "_" | "-")*
Letter     ::= "A".."Z" | "a".."z"
```

## 8. Semantics Mapping (OCL)
Each predicate maps to an OCL expression over the parameter value, and all
predicates are combined by logical AND.

Examples:

- `notEmpty` → `self.size() > 0`
- `length >= 3` → `self.size() >= 3`
- `age >= 0` → `self >= 0`
- `status in {"active","inactive"}` → `self->includes("active") or self->includes("inactive")`

Combined example:

- CML: `constraint { notEmpty length >= 3 }`
- OCL: `self.size() > 0 and self.size() >= 3`

## 9. Normalization Rules
- Inline constraints MUST be normalized to block form.
- Predicate order is preserved during normalization.
- All predicates in a block are combined using logical AND.

## 10. Examples

String parameter:

```
name constraint {
  notEmpty
  length >= 3
}
```

Numeric parameter:

```
limit constraint {
  >= 1
  <= 100
}
```

Enum parameter:

```
status constraint {
  in {"active", "inactive"}
}
```

## 11. Non-goals (Phase 1)
- OR / implies / conditional constraints
- Cross-parameter constraints
- Runtime or business rules
- Authorization or lifecycle semantics

## 12. Related Documents
- `docs/design/protocol-constraint-predicates.md`
- `docs/design/protocol-core.md`
- `docs/design/operation-definition.md`
- `docs/design/parameter-resolution.md`
