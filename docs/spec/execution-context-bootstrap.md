ExecutionContext Bootstrap and Factory
=====================================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document specifies how ExecutionContext is constructed at
application startup, including responsibility boundaries between
core and infrastructure layers.

The bootstrap process is responsible for *detecting*, *collecting*,
and *injecting* concrete context values, but not for interpreting
their semantics.

----------------------------------------------------------------------
2. Design Principles
----------------------------------------------------------------------

ExecutionContext bootstrap MUST be:

- Explicit and deterministic
- Free from hidden defaults
- Testable by injection
- Layered (core vs infrastructure)
- One-time at startup (no mutation afterward)

Bootstrap logic MUST NOT be embedded in core Context classes.

----------------------------------------------------------------------
3. Bootstrap Responsibilities
----------------------------------------------------------------------

The bootstrap layer (typically CNCF / infrastructure) is responsible
for:

- Detecting execution environment facts
- Capturing VM-level runtime characteristics
- Selecting language and locale configuration
- Creating immutable Context values
- Injecting them into ExecutionContext

----------------------------------------------------------------------
4. Context Construction Flow
----------------------------------------------------------------------

A typical construction flow is:

1. Detect EnvironmentContext
   - cwd, host, OS, user, etc.

2. Detect VirtualMachineContext
   - clock, timezone, encoding, math, env vars, etc.

3. Construct I18nContext
   - language and formatting policies
   - locale-related configuration

4. Compose ExecutionContext
   - aggregate all sub-contexts
   - no additional semantics

Detection and construction MUST be explicit at each step.

----------------------------------------------------------------------
5. Factory Interfaces
----------------------------------------------------------------------

Core defines abstract factories or builders that accept fully-formed
context values.

Example responsibility split:

- core:
  - defines ExecutionContext and its structure
  - defines factory interfaces

- infrastructure:
  - implements detection
  - supplies concrete values
  - invokes factories

Factories MUST NOT perform detection or I/O.

----------------------------------------------------------------------
6. Testability
----------------------------------------------------------------------

All factories MUST allow injection of:

- clock
- locale
- timezone
- environment variables
- encoding

This enables deterministic tests and reproducible execution.

----------------------------------------------------------------------
7. Explicit Non-Goals
----------------------------------------------------------------------

Bootstrap and factory logic does NOT include:

- Runtime reconfiguration
- Hot reload or dynamic context mutation
- Application configuration semantics
- Observability exporters
- Resolver or manager implementations

----------------------------------------------------------------------
8. Rationale
----------------------------------------------------------------------

By isolating bootstrap and detection logic from core Context types,
the system preserves immutability, testability, and clear separation
of concerns, while enabling flexible integration with different
infrastructure environments.

END
----------------------------------------------------------------------
