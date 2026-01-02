Execution Naming Rules
======================

This document defines how to choose between execute, run, and prepare
in core APIs.

----------------------------------------------------------------------
1. Core Verbs
----------------------------------------------------------------------

- prepare: build expression/program/AST only, no evaluation
- execute: evaluate a prepared expression/program (algebra evaluation)
- run: drive an interpreter or execution process

These responsibilities MUST NOT be mixed in one API.

----------------------------------------------------------------------
2. When to use execute
----------------------------------------------------------------------

Use execute when:
- the input is already prepared (expression/program/AST)
- evaluation is deterministic given inputs
- the action is a semantic evaluation, not orchestration

In core, "execution" is generally expressed as execute.

----------------------------------------------------------------------
3. When to use run
----------------------------------------------------------------------

Use run when:
- an interpreter, engine, or process is being driven
- the operation involves orchestration or lifecycle
- evaluation is not purely algebraic

In core, run should be rare and usually hidden behind execute.

----------------------------------------------------------------------
4. When to use prepare
----------------------------------------------------------------------

Use prepare when:
- building expressions/programs/AST
- assembling structure without evaluation
- validating shape without running behavior

prepare MUST NOT evaluate or trigger side effects.

----------------------------------------------------------------------
5. Free Monad / Tagless Final mapping (minimal)
----------------------------------------------------------------------

- prepare: build the program description (Free or tagless program)
- execute: interpret the program algebraically
- run: drive the interpreter or runtime process

----------------------------------------------------------------------
6. Core default
----------------------------------------------------------------------

Core APIs SHOULD prefer execute for evaluation and keep run internal.
