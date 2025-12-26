# Observation

## Role

`Observation` is a factual record of an event.
It is a data structure only and does not execute interpretation or handling.

## Message

`Observation.message` is an optional `I18nMessage`.
It may be absent for structural or library-level observations.

## displayMessage

`Observation.displayMessage` delegates to `I18nMessage.displayMessage`
when a message exists. If no message is present, it falls back to a
structural value (for example, `phenomenon.toString`).

## Debug vs Display

- `displayMessage` is a minimal, deterministic string for system-facing output.
- `debugMessage` (when present) is for diagnostics and may include extra detail.

## Non-Interpretation

Observation does not choose strategies, handlers, or reactions.
Interpretation is a responsibility of higher layers.
