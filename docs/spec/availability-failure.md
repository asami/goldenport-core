# Availability Failure Specification

## Scope

This specification defines the stable availability-failure vocabulary at the
`Observation` / `Conclusion` boundary. `Observation` records factual phenomena,
taxonomy, causal mechanism, and facets. `Conclusion` is the later judgment that
materializes interpretation, disposition, WebCode, and DetailCode. A factual
observation must not be changed merely to obtain a desired judgment.

## Cause vocabulary and projections

`Cause.Kind` is closed. Existing identities are unchanged; the availability
entries are `Timeout` (`timeout`, 11), `NotRunning` (`not-running`, 13),
`ConnectionRefused` (`connection-refused`, 14), and `Unreachable`
(`unreachable`, 15).

Descriptor facets are factual Record and JSON values. `Endpoint`, `Service`,
`Component`, and `Exception` respectively project the stable keys `endpoint`,
`service`, `component`, and `exception`; `Cause` projects `kind`. Their shape
is preserved when a Conclusion is materialized and serialized.

`serviceUnavailable` expresses an unavailable required service, while
`networkUnavailable` expresses a transport path failure. The former has the
service-unavailable taxonomy, system-failure interpretation, service-unavailable
disposition, and HTTP 503 WebCode. The latter has the network-unavailable
taxonomy, network-failure interpretation, service-unavailable disposition, and
HTTP 503 WebCode.

Representative DetailCode dimensions are stable: Docker daemon not running is
`11,11,13,5,4,3` and code `111113050403`; network connection refusal is
`18,11,14,6,4,3` and code `181114060403`.

## Throwable mapping and interruption

Availability detection traverses the complete throwable cause chain with
identity-based cycle protection. Timeout, connection-refused, and unreachable
throwables map to the corresponding closed `Cause.Kind`. `Cause.interruption`
uses the same full-chain, cycle-safe traversal and returns the actual nested
`InterruptedException`.

Interruption has priority over every availability mapping. If any chain member
is an `InterruptedException`, `availabilityKind` is absent; HTTP and shell
boundaries restore the current thread interrupt flag and rethrow that exact
exception before attempting availability or generic conversion.

## Boundary conversion

The HTTP external-reference boundary first preserves direct or wrapped
interruption, then maps recognized availability failures to
`networkUnavailable` with `Endpoint`, `Component(external-ref-resolver)`, and
`Exception` facets. Unrecognized throwables use the existing generic fallback.

The shell boundary follows the same interruption ordering. It starts exactly
two owned daemon drain tasks immediately after process creation, each using
`Bag.create` for one process stream, before waiting for the process. After
process completion it awaits both bags before building the result. An
interruption while waiting or draining destroys the owned process (forcibly when
still alive), cancels/interrupts the drain tasks, closes process streams best
effort, restores the interrupt flag, and rethrows the actual interruption.

Docker has a narrow service special case. Only a nonzero command whose exact
executable basename is `docker` and whose combined output explicitly contains
`cannot connect to the docker daemon`, `is the docker daemon running`, or
`docker daemon is not running` (case-insensitive) becomes the Docker-daemon
`NotRunning` service failure. Bare `error during connect`, TLS, DNS,
authentication, ordinary command errors, non-Docker lookalikes, and a missing
executable do not. A nonblank command `DOCKER_HOST` is the endpoint facet;
otherwise it is `docker://daemon`.

## Executable specifications

The paired executable specifications are:

- `org.goldenport.ErrorVocabularySpec`
- `org.goldenport.AvailabilityFailureSpec`
- `org.goldenport.process.ShellCommandExecutorSpec`
- `org.goldenport.protocol.handler.ingress.ArgsIngressSpec`
