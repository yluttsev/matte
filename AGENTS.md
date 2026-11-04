# Working on Matte

- Before changing a module, read its relevant documentation in `docs/en/`
  when available. Follow the documented design and update the documentation
  when changing documented behavior.
- If the project has a `.codegraph/` directory, prefer CodeGraph for locating
  and understanding code. Pass the project root as `projectPath`. Fall back
  to ordinary file search when CodeGraph does not answer the question.
- If the project has no `.codegraph/` directory, do not use CodeGraph or
  create an index unless explicitly asked.
- Respect existing module boundaries and dependency direction. Do not
  introduce cross-module dependencies without a reason.
- Add or update tests for behavior changes. Run the project's checks, or
  report what could not be checked.
- Keep corresponding English and Russian documentation in sync when both
  versions exist.
