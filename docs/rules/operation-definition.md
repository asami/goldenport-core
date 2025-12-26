# OperationDefinition Rules

These rules define enforceable constraints for semantic request interpretation.

- OperationDefinition MUST be the semantic authority for request interpretation.
- OperationDefinition MUST construct OperationRequest using createOperationRequest.
- createOperationRequest MUST accept Request explicitly as its semantic boundary input.
- createOperationRequest MUST use take_* utilities for parameter extraction.
- createOperationRequest MUST use applicative-style construction for parameter validation and assembly.
- createOperationRequest MUST NOT read Request fields directly for parameter values.
- createOperationRequest MUST construct a domain-level OperationRequest, not a generic container.
- OperationDefinition MUST NOT delegate semantic interpretation to CLI or resolver layers.
