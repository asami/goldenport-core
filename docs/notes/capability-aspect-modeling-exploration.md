# Capability / Aspect Modeling Exploration

status: exploratory
scope: SimpleModeling methodology and simplemodeling-lib
authority: non-normative note

## Purpose

This note records the current working position on `Capability` and `Aspect`.

Both terms are useful as concepts, but their practical value as independent
model elements has not yet been demonstrated. This note therefore does not
define a metamodel, syntax, identifier scheme, or required modeling activity.

## Current Approximation

For the current methodology and implementation, use the following operational
approximations:

- `Capability` is represented by the closest existing `Service`.
- `Aspect` is represented through quality attributes and Views constructed for
  quality-attribute concerns.

These are approximations, not assertions of conceptual identity.

Conceptually, a Capability may express what a Model can accomplish independently
of how that ability is exposed. A Service is the current concrete representation
because it already provides an explicit boundary, operations, and a contract.
In simplemodeling-lib, `ServiceDefinition` and `OperationDefinition` also provide
the existing introspection surface used by CLI, OpenAPI, and MCP projections.

Conceptually, an Aspect may express a concern that cuts across multiple model
elements or behaviors. A View is not itself an Aspect: it is the current means
of selecting and visualizing model elements relevant to that concern. For now,
quality attributes provide the principal vocabulary for such concerns.

## Why Independent Model Elements Are Deferred

Introducing independent Capability and Aspect elements would add extraction,
identity, synchronization, and lifecycle work. That cost is justified only if
the elements provide effects that existing Services, quality attributes, and
Views cannot provide.

Potential evidence for promoting Capability includes:

- one Capability being realized by multiple Services;
- managing Capabilities that are intentionally not exposed as Services;
- analyzing gaps between required Capabilities and available Services;
- planning, verification, or generation that operates specifically on
  Capability identity.

Potential evidence for promoting Aspect includes:

- applying one concern consistently across multiple model elements;
- deriving validation, configuration, tests, or runtime behavior from the
  concern;
- detecting missing or conflicting applications of the concern;
- representing semantics that cannot be expressed adequately as a quality
  attribute and its corresponding View.

Without such evidence, Capability risks becoming a synonym for Service, while
Aspect risks becoming another name for a quality attribute or View. In that
case, independent model elements would create duplicate maintenance without a
corresponding modeling benefit.

## Current Modeling Rule of Thumb

Until a concrete use case demonstrates the need for promotion:

1. Model an actionable ability as a `Service` with its operations and contract.
2. Model quality-related conditions using quality attributes.
3. Use a purpose-specific View to examine the elements related to a
   quality-attribute concern.
4. Use `Capability` and `Aspect` only as explanatory concepts where helpful.
5. Do not assign independent Capability or Aspect identifiers.
6. Do not introduce Capability or Aspect syntax into CML solely to preserve
   these conceptual labels.

Human-readable codes such as `CAP-...` are therefore not part of the current
modeling contract.

## Promotion Questions

Before either concept becomes an independent model element, the following must
be answered with concrete examples:

### Capability

- What information does Capability carry that Service does not?
- Is Capability authored directly or derived from Services and other models?
- What operations require Capability identity?
- How are decomposition, realization, and verification defined?
- Can CML, Cozy, or another tool use the element to reduce manual work?

### Aspect

- What information does Aspect carry that a quality attribute does not?
- To which model elements may it apply?
- Is an application of an Aspect distinct from the Aspect definition?
- How are conflicts, inheritance, measurement, and verification handled?
- Can tools derive validation, tests, configuration, or runtime behavior from
  it?

If stable identities are eventually introduced, their relationship with RDF
node identities must be designed before human-readable aliases or CML syntax
are standardized.

## Review Condition

Revisit this position when at least one real modeling workflow demonstrates
that the approximation loses important information or prevents useful
automation. Promotion should proceed from this note to `docs/design`, and then
to `docs/spec`, only after the semantic boundary and operational benefit are
clear.
