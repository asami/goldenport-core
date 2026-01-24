# Error Taxonomy — Classification Examples

status = draft
phase = 2.9
kind = journal

This document collects concrete classification examples for the Phase 2.9
error taxonomy.

The purpose of these examples is to clarify how abstract taxonomy rules
should be applied to real situations, especially in cases that are easy
to misclassify (e.g. I/O failures, database access, external integrations).

NOTE:
- Fault or exception names do not by themselves define taxonomy;
  classification requires the raw observation plus the surrounding context.
- Interpretation (failure_kind, expectation, responsibility) is derived from
  Observation attributes and any context-driven contracts such as DbC.

Each example is recorded along multiple independent axes:

1. Taxonomy: what is factually wrong
2. Cause (category/detail): the immediate failure mechanism
3. Source: logical origin of the data or interaction
4. Scope: logical domain or invariant boundary affected
5. Channel: communication or access channel (http | grpc | akka | jdbc | filesystem | in_memory)
6. Substrate: execution substrate or platform layer (jvm | os | network_stack | storage | middleware)
7. Origin: the originating system or component that issued the request

These examples are explanatory only.
They do not define handling strategies, severity, or reactions, which are
intentionally deferred to Conclusion-level interpretation.

## Rule of Thumb (Observation-level)

The following rules clarify how classification axes should be applied
at the Observation level. These rules are descriptive, not prescriptive,
and are intended to prevent misclassification.

- Taxonomy describes the observed outcome (what is wrong),
  not the underlying mechanism.
- Cause describes the technical triggering mechanism (how it failed).
- Source describes where the data or interaction originated (where it came from).
- Scope describes the logical domain or invariant affected by the observation.
- Channel describes the communication or access channel used for the interaction.
- Substrate identifies the execution platform layer where the failure manifests.
- Origin identifies the system, service, or component that issued the request.

### resource.not_found vs resource.unavailable

- resource.not_found:
    The access attempt completed successfully and the system positively
    determined that the resource does not exist.
- resource.unavailable:
    The access attempt did not complete or could not reach the resource,
    so existence could not be determined.

These distinctions are independent of later interpretation, disposition,
or handling decisions.

## Channel vs Substrate

- Channel identifies how the interaction is performed (API, IPC, in-memory cache,
  filesystem, database, etc.).
- Substrate identifies where the failure materializes within the execution stack
  (JVM, OS, network stack, storage layer, middleware).
- Protocol-level artifacts such as HTTP or gRPC status codes describe channel
  responses, not causes; capture them through Taxonomy/Cause/Substrate instead.

Case:
  Description:
    A database query fails due to a network communication error.

  Taxonomy:
    resource.unavailable

  Cause:
    remote_failure

  Source:
    db_access

  Scope:
    domain

  Channel:
    jdbc

  Substrate:
    network_stack

  Origin:
    service.api

  Rationale:
    The database resource cannot be reached because the underlying communication
    failed. The query never completed, so resource usability is unknown.

Case:
  Description:
    An in-memory lookup (e.g. Map access) fails because the requested key
    is not present in an optional dataset.

  Taxonomy:
    resource.not_found

  Cause:
    lookup

  Source:
    in_memory

  Scope:
    domain

  Channel:
    in_memory

  Substrate:
    jvm

  Origin:
    service.cache

  Rationale:
    The in-memory access succeeded but determined that the resource is missing.

  Interpretation:
    failure_kind: domain
    expectation: allowed
    responsibility: user

Case:
  Description:
    An in-memory lookup fails because the requested key is not present; the
    raw observation is identical to the previous case.

  Taxonomy:
    resource.not_found

  Cause:
    lookup

  Source:
    in_memory

  Scope:
    domain

  Channel:
    in_memory

  Substrate:
    jvm

  Origin:
    service.cache

  Rationale:
    The lookup operation completed successfully but found the resource missing
    despite earlier confirmation that it was present.

  Interpretation:
    failure_kind: defect
    expectation: invariant_violation
    responsibility: developer

Case:
  Description:
    An in-memory resource exists, but its contents are incomplete,
    malformed, or corrupted.

  Taxonomy:
    resource.corrupted

  Cause:
    data_corruption

  Source:
    in_memory

  Scope:
    domain

  Channel:
    in_memory

  Substrate:
    jvm

  Origin:
    service.cache

  Rationale:
    The resource is present and accessible, but its internal data does not satisfy
    structural expectations.

  Interpretation:
    failure_kind: environment
    expectation: allowed
    responsibility: user

Case:
  Description:
    A file is accessed by path, but the file does not exist.

  Taxonomy:
    resource.not_found

  Cause:
    lookup

  Source:
    filesystem

  Scope:
    infrastructure

  Channel:
    filesystem

  Substrate:
    os

  Origin:
    deployment.script

  Rationale:
    The file access attempt succeeded and confirmed that the specified path
    does not exist.

Case:
  Description:
    A file exists, but access is denied due to insufficient permissions.

  Taxonomy:
    resource.unavailable

  Cause:
    io.permission

  Source:
    filesystem

  Scope:
    infrastructure

  Channel:
    filesystem

  Substrate:
    os

  Origin:
    user.operation

  Rationale:
    The resource exists, but access could not be completed because permissions
    prevented the operation from finishing.

Case:
  Description:
    A file exists and can be read, but its contents cannot be parsed as expected.

  Taxonomy:
    resource.corrupted

  Cause:
    format_error

  Source:
    filesystem

  Scope:
    infrastructure

  Channel:
    filesystem

  Substrate:
    os

  Origin:
    user.tool

  Rationale:
    The resource is present, but the returned data violates structural expectations.

  Interpretation:
    failure_kind: domain
    expectation: allowed
    responsibility: user

Case:
  Description:
    A file exists, but an I/O error occurs during read due to a system-level failure.

  Taxonomy:
    resource.unavailable

  Cause:
    io.system

  Source:
    filesystem

  Scope:
    infrastructure

  Channel:
    filesystem

  Substrate:
    storage

  Origin:
    system.process

  Rationale:
    The file resource cannot be accessed reliably because the underlying I/O
    layer failed.

Case:
  Description:
    An external HTTP API request returns a 404 Not Found response.

  Taxonomy:
    resource.not_found

  Cause:
    lookup

  Source:
    external_api

  Scope:
    integration

  Channel:
    http

  Substrate:
    network_stack

  Origin:
    client.gateway

  Rationale:
    The remote system explicitly reported that the requested resource does not exist.

  Interpretation:
    failure_kind: domain
    expectation: allowed
    responsibility: user

Case:
  Description:
    An external HTTP API request times out without receiving a response.

  Taxonomy:
    resource.unavailable

  Cause:
    timeout

  Source:
    external_api

  Scope:
    integration

  Channel:
    http

  Substrate:
    network_stack

  Origin:
    client.gateway

  Rationale:
    The request did not complete and the resource state could not be determined.

  Interpretation:
    failure_kind: environment
    expectation: allowed
    responsibility: system

Case:
  Description:
    An external HTTP API request succeeds, but the response body is invalid JSON.

  Taxonomy:
    resource.corrupted

  Cause:
    format_error

  Source:
    external_api

  Scope:
    integration

  Channel:
    http

  Substrate:
    jvm

  Origin:
    client.gateway

  Rationale:
    The remote resource is reachable and responded, but its payload violates structural expectations.

  Interpretation:
    failure_kind: domain
    expectation: allowed
    responsibility: user

Case:
  Description:
    An external HTTP API request returns a 500 Internal Server Error.

  Taxonomy:
    resource.unavailable

  Cause:
    remote.failure

  Source:
    external_api

  Scope:
    integration

  Channel:
    http

  Substrate:
    network_stack

  Origin:
    client.gateway

  Rationale:
    The remote operation failed before a meaningful resource state could be determined.

  Interpretation:
    failure_kind: environment
    expectation: allowed
    responsibility: system

Case:
  Description:
    A gRPC call to an external service returns NOT_FOUND.

  Taxonomy:
    resource.not_found

  Cause:
    lookup

  Source:
    external_service

  Scope:
    integration

  Channel:
    grpc

  Substrate:
    network_stack

  Origin:
    client.gateway

  Rationale:
    The external service explicitly reported a missing resource without providing protocol semantics.

  Interpretation:
    failure_kind: domain
    expectation: allowed
    responsibility: user

## Appendix — Legacy Fault Mapping (Informative)

| Fault Class              | Failure Kind              | Notes on Context Dependency |
|--------------------------|---------------------------|------------------------------|
| InvalidArgumentFault     | domain                    | Typically expectation=allowed unless DbC invariants run first |
| DatabaseIoFault          | environment               | May be reclassified once retries succeed or fail |
| IllegalStateDefect       | defect                    | Always tied to developer responsibility, interpretation only confirms |
| ResourceNotFoundFault    | domain / environment      | Depends on whether the resource was expected to exist; needs Observation context |
