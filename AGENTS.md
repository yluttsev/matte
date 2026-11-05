# Working on Matte

- Before changing a module, read its relevant documentation in `docs/en/`
  when available. Also check `MATTE_PROJECT_OVERVIEW.md` and
  `MATTE_IMPLEMENTATION_PLAN.md` for project invariants, accepted decisions,
  module responsibilities, and current scope. Verify the current code as well;
  plans are not proof that work exists.
- Follow the documented design and update the documentation when changing
  documented behavior.
- If the project has a `.codegraph/` directory, prefer CodeGraph for locating
  and understanding code. Pass the project root as `projectPath`. Fall back
  to ordinary file search when CodeGraph does not answer the question.
- If the project has no `.codegraph/` directory, do not use CodeGraph or
  create an index unless explicitly asked.
- Keep each module's responsibilities within its boundary and preserve the
  established dependency direction. Add cross-module dependencies only when
  needed and consistent with the architecture.
- Add or update tests for behavior changes. Run the project's checks, or
  report what could not be checked.
- For new features, bug fixes, and refactors, review the relevant
  documentation and update or add content when needed so it describes the
  implemented behavior, contracts, and limitations accurately.
- Keep corresponding English and Russian documentation synchronized when both
  versions exist.

## Code style

- Follow the project's Checkstyle configuration and established Java style.
- When a method or constructor signature needs multiple lines, put each
  parameter on its own line. Keep the opening parenthesis after the method or
  constructor name and put the closing parenthesis on its own line at the
  declaration indentation.
- Prefer `if/else` blocks over ternary operators, including when choosing a
  value.
- Keep methods focused on one responsibility. Extract cohesive steps into
  smaller methods with clear names when a method handles several tasks or is
  difficult to follow; avoid splitting code mechanically.
- Keep conditions in control-flow statements such as `if`, `while`, and `for`
  easy to read. Extract long or compound predicates into clearly named methods.
- Replace opaque or domain-specific literals with named constants that explain
  their meaning.

## Tests

- Store complete OpenAPI, YAML, or JSON documents used as test inputs or
  expected outputs in the module's test resources. Load them through the
  existing fixture helper where available; do not embed complete documents in
  test strings or text blocks.
- Name tests using
  `test[MethodName]_When[Condition]_Should[ExpectedResult]` or
  `test[MethodName]_When[Condition]_Then[ExpectedResult]`.
- Think through several relevant scenarios before implementing a behavior
  change. Cover positive and negative cases, boundaries, non-obvious
  combinations, and likely failure modes, not only the basic successful path.
  Where an operation is expected to be atomic, verify that failure leaves the
  original state unchanged.

## Commits

- Split work into small, coherent, atomic commits, each representing an
  independently understandable task.
- Combine changes in one commit when the change is small or splitting tightly
  related work would add no review or maintenance value.
- Follow the commit-message style already used in `git log`, including its
  type and scope format where applicable (for example, `feat(openapi): ...`,
  `fix(openapi): ...`, and `test(openapi): ...`). Check recent history when
  unsure instead of introducing a new format.
