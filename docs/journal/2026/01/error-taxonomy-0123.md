# Error Taxonomy — Core Classification Draft

status = draft
phase = 2.9
kind = journal

----------------------------------------------------------------------
## Purpose
----------------------------------------------------------------------

This document defines a draft taxonomy for classifying errors
in the simplemodeling-lib core.

Its purpose is to establish a shared vocabulary and conceptual
structure for errors before fixing any normative specifications
or APIs.

This taxonomy focuses on Observation-level classification only.

----------------------------------------------------------------------
## Scope
----------------------------------------------------------------------

In scope:

- Conceptual classification of errors
- Observation-level vocabulary
- Domain failure vs system defect distinction

Out of scope:

- API definitions
- Error codes or numeric identifiers
- Presentation or transport mapping (CLI, HTTP, etc.)
- Handling policies or severity rules

----------------------------------------------------------------------
## Core Classification Principles
----------------------------------------------------------------------

- Errors are classified by factual cause, not by reaction or severity
- Observation records facts only, without interpretation
- Interpretation and handling are deferred to Conclusion and context
- Classification must support aggregation and reuse

----------------------------------------------------------------------
## Top-Level Categories
----------------------------------------------------------------------

The core error taxonomy starts with the following top-level categories.

- argument
- state
- resource
- configuration
- system

This document focuses primarily on the "argument" category,
which represents invalid or unexpected external inputs.

----------------------------------------------------------------------
## Category: argument
----------------------------------------------------------------------

The "argument" category represents factual problems with
externally supplied arguments or parameters.

These errors are classified as domain failures.

They do not imply system defects or internal inconsistency.

----------------------------------------------------------------------
## argument Subcategories
----------------------------------------------------------------------

### argument.out_of_range

Meaning:
  - The argument value is outside the allowed or expected range
  - The type and format are otherwise valid

Facts captured:
  - argument name
  - provided value
  - expected range or constraint

----------------------------------------------------------------------

### argument.type_mismatch

Meaning:
  - The argument value cannot be interpreted as the expected type

Facts captured:
  - argument name
  - provided value
  - expected type

----------------------------------------------------------------------

### argument.missing

Meaning:
  - A required argument was not provided

Facts captured:
  - argument name

----------------------------------------------------------------------

### argument.unexpected

Meaning:
  - An argument was provided that is not defined or accepted

Facts captured:
  - argument name
  - provided value (if available)

----------------------------------------------------------------------
## Relationship to IllegalArgument
----------------------------------------------------------------------

"IllegalArgument" is treated as a semantic grouping,
not as a single concrete error kind.

Each concrete argument-related failure must be represented
by one of the argument subcategories defined above.

----------------------------------------------------------------------
## Non-Goals
----------------------------------------------------------------------

This taxonomy does not define:

- How errors are converted into messages
- How errors are mapped to protocols or transports
- How errors should be handled or recovered from

These decisions are intentionally deferred.

----------------------------------------------------------------------
## Role in Phase 2.9
----------------------------------------------------------------------

This taxonomy serves as a reference for:

- Observation vocabulary specification
- Convenience factory APIs (e.g. Consequence.illegalArgument*)
- Error code construction rules
- Component-level error translation

No specification or implementation change in Phase 2.9
should contradict this taxonomy.

## Appendix: Legacy Goldenport Error Mapping

This appendix documents how the legacy *goldenport* error model maps onto
the current Phase 2.9 error taxonomy.

Its purpose is historical grounding, not backward compatibility.

----------------------------------------------------------------------
A. Top-Level Category Mapping
----------------------------------------------------------------------

Phase 2.9 CategoryLegacy Trait / Concept
------------------------------------------------
argument                 ArgumentFault
value-domain             ValueDomainFault, ValueDomainResultFault
property                 PropertyFault
resource                 ResourceNotFoundFault, UnfoldResourceFault
io                       IoFault (Database / Network / File / System / Subsystem)
state                    IllegalStateDefect, NoReachDefect
configuration            IllegalConfigurationDefect, InvalidTokenFault
system                   Defect (system-level failures)

----------------------------------------------------------------------
B. Argument Category Mapping
----------------------------------------------------------------------

The legacy goldenport system distinguished multiple concrete
argument-related failures. These directly inform the Phase 2.9
argument subcategories.

Phase 2.9 Subcategory        Legacy Fault
------------------------------------------------
argument.missing            MissingArgumentFault
argument.empty              EmptyArgumentFault
argument.unexpected         TooManyArgumentsFault
argument.type_mismatch      ValueDomainDatatypeFault
argument.out_of_range       ValueDomainValueFault
argument.multiplicity       ValueDomainMultiplicityFault
argument.syntax             SyntaxErrorFault
argument.format             FormatErrorFault
argument.unsupported        UnsupportedOperationFault,
                            UnsupportedFormatFault
argument.invalid (generic)  InvalidArgumentFault

Note:
InvalidArgumentFault acted primarily as an aggregator rather than
a concrete semantic category. In Phase 2.9 it is treated as a
semantic grouping, not a leaf classification.

----------------------------------------------------------------------
C. Value Domain Errors
----------------------------------------------------------------------

Legacy goldenport separated value-domain failures from arguments:

- ValueDomainValueFault
- ValueDomainDatatypeFault
- ValueDomainMultiplicityFault

In Phase 2.9, these are candidates for integration under
argument.value.* since they represent factual input violations
rather than internal defects.

----------------------------------------------------------------------
D. Property Errors
----------------------------------------------------------------------

Phase 2.9 Category        Legacy Fault
------------------------------------------------
property.invalid           InvalidPropertyFault
property.missing           MissingPropertyFault
property.unmarshal         UnmarshallingDefect
property.unreachable       NoReachDefect

----------------------------------------------------------------------
E. IO and Resource Errors
----------------------------------------------------------------------

Phase 2.9 Category        Legacy Fault
------------------------------------------------
io.database                DatabaseIoFault
io.network                 NetworkIoFault
io.file                    FileIoFault
io.system                  SystemIoFault
io.subsystem               SubsystemIoFault
resource.not_found         ResourceNotFoundFault
resource.unfold            UnfoldResourceFault

These were consistently treated as system defects rather than
domain failures.

----------------------------------------------------------------------
F. State and Configuration Defects
----------------------------------------------------------------------

Phase 2.9 Category        Legacy Fault
------------------------------------------------
state.illegal              IllegalStateDefect
configuration.illegal      IllegalConfigurationDefect

These represent violations of internal invariants and must never
be surfaced as domain-level argument failures.

----------------------------------------------------------------------
G. Design Continuity
----------------------------------------------------------------------

The legacy goldenport model already adhered to principles now
formalized in Phase 2.9:

- factual error capture without interpretation
- separation of domain failure and system defect
- structured aggregation of multiple failures
- deferral of handling strategy to higher context

Phase 2.9 should therefore be understood as a taxonomy normalization
and API refinement, not a conceptual redesign.
