# DayLoop Visual Contract

> Historical reference only. The active contract is
> [`EDITORIAL_VISUAL_CONTRACT.md`](EDITORIAL_VISUAL_CONTRACT.md), which defines
> Editorial Field Notes: orange/charcoal accents, neutral surfaces, sharp
> 2–8dp geometry, 24dp page margins, globally selected typography, and the
> edit-surface rule. Do not use the matcha specification below for new work.

This document is the reusable visual contract for DayLoop UI implementation prompts and Compose design work. It translates the accepted inspiration boards into stable product and implementation direction so later screens can be built consistently without redesigning after feature completion.

## Purpose

DayLoop should feel like a calm-but-energetic productivity OS: pale mint surfaces, vivid matcha/lime accents, near-black action cards, rounded pill controls, compact dashboards, bold numeric progress, and a clear sense that Today is the execution surface.

This contract governs future UI prompts, Compose components, theme tokens, screen previews, and visual QA unless a later design decision explicitly updates it.

## Master Style Direction

Use a distinctive matcha/mint/black system.

- Background: matcha/lime green, approximately `#B8D95A`.
- App surface: soft mint/off-white, approximately `#F7FAEF`.
- Primary dark: near-black, approximately `#111416`.
- Accent green: approximately `#A8CF3D`.
- Supporting text: muted olive/gray, approximately `#6E7568`.
- Alert accent: warm yellow/orange for warning, degraded reminders, and repair states only.
- Corners: large rounded phone/container edges; cards around 18-28dp; small chips around 14-20dp.
- Typography: bold compact headings, oversized numeric progress, minimal body copy, and compact labels.
- Components: pill tabs, black CTA bars, soft metric cards, circular progress rings, timeline rails, schedule chips, icon buttons, and bottom action bars.
- Icons: prefer provided SVGs when available; otherwise use temporary simple line icons from the app icon set. Temporary icon choices may use free resources such as Figma community libraries for reference, but copied assets must be license-compatible before entering the repository.

Avoid:

- Generic beige wellness minimalism.
- Purple/blue gradient tech styling.
- Sparse screens that feel unfinished.
- Decorative-only cards that do not communicate task, time, progress, or action.
- Text-heavy onboarding or instruction screens.

## Customization Requirements

The design starts with curated palettes, but the product must allow personalization later.

- The app should ship with predefined palette blends first, including the default matcha/mint/black palette.
- User-created color customization can follow after curated palettes exist.
- Screens should consume semantic design tokens instead of hard-coded colors.
- Font sizes must be adjustable in the future Advanced Settings area.
- Compose typography should be scalable and resilient before that setting exists.
- UI should remain legible with larger font settings and should not depend on single-line labels fitting perfectly.

## Navigation Rule

The bottom tab bar belongs on main screens only. Do not show it on onboarding, modal editors, focused setup steps, or blocking permission/repair flows unless the flow is intentionally part of main navigation.

The tab bar should be designed as a future animated component from the start.

- Active tab: lime icon/text glow or filled capsule.
- Inactive tabs: pale gray/white line icons.
- Background: near-black rounded pill bar.
- Future animation: active indicator slides between tabs.
- Future icon motion: active icon may bounce, morph, or pulse softly.
- Future add/focus motion: central add or focus action may pulse subtly.
- API rule: bottom navigation must be state-driven so animation can be added without rewriting screen content.
- Main tabs: Today, Plan, Focus, Goals, More.

## Reusable Component Targets

Future implementation should converge on these reusable Compose components before screen-specific polish grows too large.

- `DayLoopScreen`: pale app surface, consistent safe-area handling, and shared screen padding.
- `DayLoopHeroCard`: near-black dashboard card for high-priority progress or warning content.
- `DayLoopMetricCard`: compact soft card for streaks, counts, scores, and small summaries.
- `DayLoopPillTabRow`: segmented choices such as Time block vs Interval.
- `DayLoopScheduleSelector`: mutually exclusive schedule chips such as Now, Next, Anytime, Deadline, Time block, Interval, Someday.
- `DayLoopTaskRow`: rounded task row with completion affordance, schedule metadata, and optional time chip.
- `DayLoopTimeline`: vertical or horizontal timeline rail for time-block and daily planning views.
- `DayLoopBottomNav`: near-black animated-ready bottom tab shell.
- `DayLoopBottomActionBar`: wide black CTA/action pill for Add, Save, Start, or Repair.
- `DayLoopProgressRing`: circular progress visualization for Today, focus, and goals.
- `DayLoopWarningBanner`: near-black or warm-accent repair/permission warning component.

## Screen Implementation Prompts

Use these prompts when generating mock-ups, Compose implementation guidance, or design QA checklists. Keep copy short and adapt content to real app state.

### 1. Onboarding / Welcome

Prompt:

Create the DayLoop onboarding screen in the established matcha/mint/black design system. Use a full-screen pale mint phone surface. Place the DayLoop logo near the top with compact supporting copy: "Plan. Focus. Progress." Use a playful abstract mascot or brand mark in the middle with lime green, near-black, white, and small accent dots. Below it, show three compact benefit rows with small circular icons: "Plan your day with time", "Focus on what matters", and "Build lasting momentum". At the bottom, add a wide near-black rounded CTA bar reading "Start the day" with a lime circular arrow button on the right. Add small secondary sign-in text beneath only if account support exists. Do not show bottom navigation.

Functionality:

- First-run entry point.
- Primary CTA moves to Today.
- Notification or exact-alarm permission education may be added as a later step, not forced into the visual welcome screen.
- Use manually provided logo or mascot SVGs when available.

### 2. Today Dashboard

Prompt:

Create DayLoop's Today dashboard. Top row has a menu icon, title "Today", small date label, and a calendar icon. The main hero card is near-black with a lime circular progress ring, progress percent, task count, and focus time. Under it are three compact metric cards for streak, completed, and focus score. Render sectioned task lists for Now, Next, and Anytime. The Now item uses a pale lime highlight with a left timeline dot and a small duration chip. Next and Anytime items are white rounded rows with checkbox circles and small schedule icons. Add a floating near-black "+ Add task" pill above the bottom nav. Bottom navigation is a black rounded pill with Today, Plan, Focus, Goals, and More.

Functionality:

- Default landing screen.
- Derived from Today state; Today remains the execution surface.
- Empty state must still feel intentional and designed, not sparse.
- Add task opens the editor.
- Completion controls emit completion intent upward.
- Bottom navigation uses state-driven selected tab data for future animation.

### 3. Add Task Editor

Prompt:

Create a DayLoop Add Task screen. Use a pale mint background and a compact top app bar with Cancel, centered "Add task", and Save. Form fields use rounded white input containers. Include task title and optional notes. The Schedule section uses rounded icon cards/chips for Now, Next, Anytime, Deadline, Time block, Interval, and Someday as available. The selected chip is lime filled. Deadline row uses date and time pills. Time-block row uses start and end date/time pills. Project selector is a rounded dropdown. Priority is shown as colored circular dots, with the selected dot using a black/lime focus ring. Estimate uses a rounded stepper with minus and plus. Bottom has a wide near-black "Save task" button.

Functionality:

- Creates a task draft.
- Title is required.
- Schedule type is mutually exclusive.
- Schedule controls reveal progressively based on selected type.
- Save validates before creating or updating a domain `Task`.
- Icons may be manually supplied SVGs.

### 4. Time Block / Interval Editor

Prompt:

Create a DayLoop schedule editor screen. Top bar: Back, title "Time block", Done. Under it, show a segmented pill control with Time block and Interval. The selected segment is lime. A date row includes a calendar icon, date label, and optional All day toggle if supported. Start time and End time appear as two white rounded input cards. Repeat and Reminder rows are white list cells with right chevrons. Lower half shows a horizontal time ruler with a lime draggable block between start/end handles. Bottom has a near-black "Save block" button.

Functionality:

- Time block mode edits start/end date-time.
- Interval mode edits frequency/window/duration controls.
- Validates start before end for time blocks.
- Supports cross-day ranges only when the domain and UX explicitly allow it.
- Does not schedule alarms directly; it edits domain schedule data.

### 5. Projects / Goals Overview

Prompt:

Create the DayLoop Projects and Goals overview. Use a pale mint background. Top title is "Projects" with a small See all action. Horizontal project cards show project name, task count, percent, and a small lime progress bar. Goals section has vertical list rows with circular icon badge, goal title, progress fraction, progress bar, and percent. Bottom has a near-black "+ Add goal" pill button. Show bottom navigation with Goals active.

Functionality:

- Secondary planning surface.
- Projects and goals feed tasks into Today later.
- Progress is derived, not manually duplicated.
- Goal icons can use provided SVGs or badge assets.
- Keep Today more prominent than Projects/Goals in the app hierarchy.

### 6. Reminder Repair / Focus

Prompt:

Create DayLoop's reminder repair and focus screen. Top bar: Back, title "Repair reminders". First card is a near-black warning banner with a warm alert icon, text "3 reminders need attention", and a lime Review button. Below are reminder rows with checkbox/status, task title, scheduled time, Reschedule or Done actions, and overflow menu. Lower Focus section uses a large lime card with "Focus session", huge timer "25:00", black circular play button, and mode chips: Pomodoro, Short break, Long break. Show bottom navigation with Focus active only when this screen is reached as a main Focus tab rather than a blocking repair flow.

Functionality:

- Shows degraded or blocked reminder states.
- Lets the user review, reschedule, repair, or mark done.
- Focus timer remains separate from canonical alarm delivery.
- Exact alarm repair must remain deterministic and permission-aware in later phases.
- Warm warning accents are used sparingly.

### 7. Idle State / Ambient Auto-Start

Prompt:

Create DayLoop's idle state screen inspired by a minimal calendar display. Use a pale warm-mint full-screen background. Place a very large bold day number at upper left, with month and year stacked beneath. Put the weekday on the right. Below, show a compact month-dot calendar grid: small circles for days, black filled circles for completed or active days, pale circles for empty days, and one bright orange or lime dot for today. Do not use cards, clutter, or bottom navigation. Add an optional subtle bottom hint: "Tap to start today". The screen should feel like a calm always-ready clock/calendar surface.

Functionality:

- Ambient state when the app is open but inactive.
- Can later auto-start into Today after a timeout, morning routine, configured start window, or tap.
- Shows date, day status, and monthly rhythm.
- No bottom nav until the user enters app mode.
- Background auto-start must be configurable and respectful of Android battery, permissions, and user expectations.

## Implementation Guardrails

- Build reusable components before duplicating screen-specific styling.
- Keep main screen navigation separate from modal editor flow navigation.
- Do not let generated image mock-ups override domain rules already accepted in `STATE.md`.
- Do not make AI, alarms, calendar sync, cloud, or projects/goals primary before their planned phases.
- Treat provided icons/SVGs as replaceable assets behind stable component slots.
- Keep accessibility labels explicit even when visuals become more playful.
- Continue using Compose state hoisting: UI emits events; canonical task state belongs above the composable.

## Verification Expectations

For each future UI implementation slice:

- Add or update Compose previews for the relevant screen states.
- Add focused JVM tests for derived UI state when behaviour is data-driven.
- Add or update Compose Android tests for visible labels, primary actions, and validation states.
- Compile `:app:assembleDebugAndroidTest` after adding or changing instrumentation tests.
- Preview on an emulator before declaring major visual alignment complete.
- Check large-font behaviour for text-heavy cards, chips, and bottom navigation.
