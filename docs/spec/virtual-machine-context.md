VirtualMachineContext
=====================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

VirtualMachineContext represents a value-backed snapshot of the
runtime characteristics observed by the virtual machine at execution
startup.

It captures VM-level facts required for reproducibility, debugging,
observability, and AI-assisted diagnosis, without participating in
business or language semantics.

----------------------------------------------------------------------
2. Design Principles
----------------------------------------------------------------------

VirtualMachineContext MUST be:

- Explicit and value-backed
- Immutable after construction
- Free from I/O and side effects
- Independent from application logic
- Suitable for logging and diagnostic inspection

----------------------------------------------------------------------
3. Core Structure
----------------------------------------------------------------------

VirtualMachineContext includes the following categories of information:

- Time and clock configuration
- Locale-independent VM settings
- Numeric and textual low-level settings
- Environment variables (snapshot)
- Resource bundle metadata (no I/O)

----------------------------------------------------------------------
4. Responsibilities
----------------------------------------------------------------------

4.1 Time and Clock

VirtualMachineContext records time-related VM settings:

- clock (injected, testable)
- timezone

These values describe how the VM perceives and measures time.

---------------------------------------------------------------------
4.2 Encoding and Textual Settings
----------------------------------------------------------------------

VirtualMachineContext records low-level textual settings:

- default character encoding
- line separator

These settings affect byte-to-text interpretation and output
normalization at the VM boundary.

----------------------------------------------------------------------
4.3 Numeric and Math Settings
----------------------------------------------------------------------

VirtualMachineContext records numeric computation settings:

- MathContext (precision and rounding mode)

These settings describe numeric behavior of the VM but do not define
domain-level numeric semantics.

----------------------------------------------------------------------
4.4 Environment Variables
----------------------------------------------------------------------

VirtualMachineContext contains a complete snapshot of environment
variables visible to the VM at startup.

- All key-value pairs are retained
- No filtering or masking is applied within the Context
- The snapshot is immutable

Security, masking, or redaction MUST be handled at output or export
boundaries, not within VirtualMachineContext itself.

----------------------------------------------------------------------
4.5 Resource Bundle Metadata
----------------------------------------------------------------------

VirtualMachineContext MAY include metadata about resource bundle usage,
such as:

- base names
- locales
- resolution order

ResourceBundle instances themselves MUST NOT be stored, accessed, or
resolved within VirtualMachineContext.

----------------------------------------------------------------------
5. Explicit Non-Goals
----------------------------------------------------------------------

VirtualMachineContext does NOT include:

- Application configuration semantics
- I18n or language interpretation
- ResourceBundle resolution or I/O
- Dynamic or mutable runtime state
- Security or secret management policies

----------------------------------------------------------------------
6. Relationship with Other Contexts
----------------------------------------------------------------------

- EnvironmentContext describes execution location and host assumptions
- VirtualMachineContext describes VM-observed runtime characteristics
- I18nContext describes language-related semantics

All are composed into ExecutionContext, but each remains independent
and single-purpose.

----------------------------------------------------------------------
7. Implementation Responsibility
----------------------------------------------------------------------

- core:
  - Defines the structure of VirtualMachineContext
  - Enforces immutability and value-backed design

- CNCF / infrastructure:
  - Detects VM characteristics
  - Injects concrete values during bootstrap

----------------------------------------------------------------------
8. Rationale
----------------------------------------------------------------------

By separating VM-level observations from language and domain semantics,
VirtualMachineContext enables reproducible execution, precise debugging,
and AI-assisted reasoning about runtime behavior without polluting
core logic or higher-level contexts.

END
----------------------------------------------------------------------
