# HTTP No Content

Status: normative static contract

## Status selection

`HttpStatus.NoContent` is the named singleton for HTTP 204. Its `code` is
`204`, and every `HttpStatus.fromInt(204)` selection returns that same
singleton. This is an append-only addition to the core-owned status model.

The established named statuses keep their singleton identities, including
`Ok` (200), `Created` (201), `SeeOther` (303), and `TemporaryRedirect` (307).
Other codes within 300 through 399 retain their numeric redirection value and
value equality. Unsupported codes outside that redirection range, other than
the established named statuses and the new 204, remain absent from `fromInt`.
The response parser retains its existing 500 fallback for unsupported codes.
There is no generic 2xx status fallback or arbitrary public status constructor.

## Empty response representation

The existing text and binary response constructors accept `NoContent` and
empty bodies without changing their signatures, content type, or header
behavior. A text response retains `Some("")` as its string representation;
a binary response retains an empty binary Bag and no text interpretation.

The public `HttpResponse.parser` preserves 204 for an empty input stream.
With a text Content-Type it produces the existing empty text representation;
with a binary Content-Type it produces the existing empty binary
representation. Without Content-Type it retains
`ContentType.APPLICATION_OCTET_STREAM` and the binary representation.
Content-Type selection and response header lookup remain case-insensitive.
Supplied response metadata survives both parsing and header attachment.

This addition does not introduce a body suppression policy: it adds status
selection while preserving the existing constructor and parser behavior.
The caller retains ownership of parser input streams.

## Compatibility

`HttpResponse.isSuccess` retains its established `status == HttpStatus.Ok`
meaning. It is true for 200 and false for 201, 204, and redirections. The new
status does not reinterpret this predicate as the entire HTTP 2xx category.
Existing header, text, binary, charset, and unsupported-status semantics remain.

## Executable specifications

[HttpNoContentSpec](../../src/test/scala/org/goldenport/http/HttpNoContentSpec.scala)
defines NC1 singleton selection, NC2 empty construction and parsing, and NC3
generated bounded metadata and compatibility behavior with at least fifty
successful generated cases.
[HttpStatusRedirectSpec](../../src/test/scala/org/goldenport/http/HttpStatusRedirectSpec.scala)
preserves the complete redirection range, named identities, payloads, headers,
and remaining unsupported probes.
[HttpResponseSpec](../../src/test/scala/org/goldenport/http/HttpResponseSpec.scala)
retains the existing response metadata and parser contracts.
