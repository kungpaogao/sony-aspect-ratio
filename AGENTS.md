# Agent rules

These apply to every agent working in this repo: Claude, Cursor, Codex and any other.

## Comments
- **Default to none:** don't add a comment unless the code needs context a reader can't get from the code itself.
- **Worth keeping:**
  - a camera or firmware quirk;
  - evidence from a camera log;
  - a workaround;
  - an API 10 limitation;
  - a non-obvious unit or magic number;
  - why something surprising is done.
- **Don't write comments that:**
  - narrate what the next line does;
  - restate a name or a type;
  - label sections;
  - record history, like "now", "used to" or "changed from".
- **KDoc counts too:** write it only when it says something the name and signature don't. Keep it to a sentence or two.
- **Keep comments current:** when you change code, fix or delete any comment that no longer holds.
- **Style:** no parenthetical asides. Give a detail its own sentence or clause instead.
