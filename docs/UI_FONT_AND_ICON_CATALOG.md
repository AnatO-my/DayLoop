# Native font and planning icon catalog

## Fonts
Recommended monospace choices: JetBrains Mono (the existing bundled default) and Android system monospace.

Five widely used sans-serif choices are bundled: Roboto, Open Sans, Lato, Montserrat and Poppins. This is a practical selection, not a verified ranking of native-mobile usage. Files come from the official [Google Fonts repository](https://github.com/google/fonts/tree/main/ofl); each family's OFL license is bundled under app/src/main/assets/licenses.

All choices work offline. More > Appearance > Advanced > Font persists the selection and applies it through Material typography, including both Focus timer views. Existing text-size and font-weight preferences remain independent. The separate Glance widget is not restyled by this change.

Reimport using scripts/import-ui-fonts.ps1. Original supplied JetBrains files are not modified.

## Project and goal icons
Available choices: Goal (flag), Planning (calendar), Focus (timer), Routine (checked calendar), Milestone (check), Activity (play), Writing (pencil), Files (save).

These use already-imported coolicons assets and existing attribution. Creation and editing dialogs include a selector; cards show the selected icon. Unknown/absent selections fall back to Planning for projects and Goal for goals.

Icon selection is decorative presentation metadata in DataStore, keyed by entity type and immutable ID. Room continues to own projects, goals, tasks and their relationships. Saving decoration precedes the Room write so a failed decoration write does not create an unstyled duplicate; a later Room failure may leave unused decoration metadata but cannot delete or modify task content. This is not a cross-store transaction. Deleted entities may leave harmless ID-keyed decoration until app data is cleared.
