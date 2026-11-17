# Icon Actions Handoff

## Scope

Routine actions use existing Android vector icons, the native scalable equivalent of SVG assets. Wardrobe add/close/archive/restore/delete, outfit cancel/save, and note creation now use DayLoopIconButton. Existing consumers of this shared component also inherit its tooltip behavior.

## Approach

- Material3 TooltipBox and PlainTooltip provide a temporary label on hold and hover, preferring placement above the control when space permits.
- Keep accessible action descriptions, button semantics, disabled states, and circular clipping for press feedback. Default control size remains 48dp; caller size overrides should be audited separately.
- Archive and restore vectors follow Lucide's archive and archive-restore icons; the existing assets/licenses/lucide_isc.txt applies.
- Keep text for ambiguous choices and destructive confirmation dialogs. Wardrobe deletion now asks for confirmation before invoking the existing repository operation.
- Do not wrap controls that already use long press for another action (timer duration editing and mascot settings access).

## Continuation Guide

Use DayLoopIconButton for familiar commands. Keep labels for domain choices such as Wear this, Someday, and schedule modes. Do not convert option selectors into unexplained icons. Retain explicit confirmation or undo for destructive operations.

## Validation

Source review and vector XML parsing only. No build, automated tests, emulator or device checks were run, respecting the manual-review preference. On device, check tap invokes once, holding shows the label without invoking the action, disabled actions remain inert, labels fit in narrow dialogs, and TalkBack announces meaningful names. Check wardrobe archive/restore and delete cancellation/confirmation, outfit saving and note creation.

References: https://developer.android.com/develop/ui/compose/components/tooltip and https://lucide.dev/icons/archive
