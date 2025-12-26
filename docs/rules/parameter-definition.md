# ParameterDefinition Rules

These rules define enforceable constraints for parameter specification and
semantic resolution in protocol-level request interpretation.

## Core Responsibility

- ParameterDefinition MUST be the single source of truth for parameter semantics.
- ParameterDefinition MUST define the name, kind, and multiplicity of each parameter.
- ParameterDefinition MUST be declared in `RequestDefinition.parameters`.

## Usage Constraints

- Semantic interpretation MUST resolve parameters through ParameterDefinition.
- take_* utilities MUST resolve parameters via ParameterDefinition.
- take_* utilities MUST NOT read raw Request fields directly.
- If a parameter is not defined in ParameterDefinition, semantic interpretation MUST fail.

## Multiplicity

- Multiplicity MUST be defined using the shared schema model.
- Optional parameters MUST be explicitly marked with multiplicity `ZeroOne`.
- Required parameters MUST NOT rely on implicit defaults.

## Validation Semantics

- Parameter validation MUST occur during semantic interpretation.
- Validation errors MUST be reported via Consequence.
- Multiple parameter validation errors SHOULD be aggregated using applicative-style construction.

## Prohibited Patterns

- Semantic logic MUST NOT infer parameter meaning from argument position alone.
- Semantic logic MUST NOT bypass ParameterDefinition for convenience.
- CLI and resolver layers MUST NOT interpret parameter semantics.

## Summary

ParameterDefinition establishes the semantic contract between syntactic input
and operation execution. All parameter meaning, validation, and multiplicity
rules are centralized here to ensure consistency, testability, and correctness.
