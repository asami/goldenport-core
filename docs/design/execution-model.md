Execution Model
===============

This document describes the prepare -> execute/run model used in core.
It explains intent and boundaries without changing existing APIs.

----------------------------------------------------------------------
1. Three-phase model
----------------------------------------------------------------------

- prepare: build an expression or program (AST)
- execute: evaluate a prepared expression (algebra evaluation)
- run: drive an interpreter or execution process

prepare does not evaluate. execute evaluates semantics. run drives a
runtime process.

----------------------------------------------------------------------
2. Expression vs program
----------------------------------------------------------------------

An expression/program is a value-level description of behavior.
It can be inspected, combined, and validated without execution.

prepare constructs these values.

----------------------------------------------------------------------
3. execute = algebra evaluation
----------------------------------------------------------------------

execute is the semantic evaluation step over a prepared structure.
It is deterministic given inputs and does not imply process control.

----------------------------------------------------------------------
4. run = interpreter driving
----------------------------------------------------------------------

run denotes driving an interpreter, engine, or process lifecycle.
It can involve orchestration and runtime concerns beyond pure algebra.

----------------------------------------------------------------------
5. Why core avoids run in public APIs
----------------------------------------------------------------------

Core favors stable, semantic APIs. execute exposes algebraic meaning
without binding callers to a specific runtime process model.

run is usually internal or delegated to infrastructure layers.

----------------------------------------------------------------------
6. Note on CNCF
----------------------------------------------------------------------

CNCF may expose run where it owns orchestration or runtime concerns.
Core should keep run minimal and prefer execute for semantic evaluation.
