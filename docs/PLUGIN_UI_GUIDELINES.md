# Plugin UI Guidelines

Shared RuneLite plugin UI/design conventions for this repository.

## Design Philosophy

- Make the plugin feel native to RuneLite rather than like a custom web app.
- Optimize common actions for clarity and low friction.
- Keep visual hierarchy obvious: users should quickly see what the plugin does, what state it is in, and what they can do next.
- Favor restraint. Extra controls, colors, and copy should earn their place.

## Sidepanel Conventions

- Use sidepanels for interactive workflows, browsing, editing, or tool-driven tasks.
- Keep the primary task visible near the top.
- Prefer a small number of clearly separated sections over deep nesting.
- If a panel can become long, group content into sections with stable headings.
- Avoid layouts that require users to hunt across the panel for the next required action.

## Buttons And Actions

- Make the primary action obvious.
- Keep destructive or hard-to-reverse actions visually separate from routine actions.
- Use consistent button ordering throughout the panel.
- Prefer explicit labels over vague verbs like `Do`, `Run`, or `Apply`.
- Disable or hide actions only when the behavior is obvious to the user; otherwise explain why the action is unavailable.

## Tabs And Modes

- Use tabs only when the content naturally splits into a few stable modes or categories.
- Keep tab names short and concrete.
- Do not use tabs when a simple vertical layout is clearer.
- Preserve the current tab or mode when practical so repeated workflows stay efficient.

## Typography And Copy

- Prefer short labels and short helper text.
- Write labels in terms of user intent, not internal implementation.
- Use helper text to explain consequences or edge cases, not to restate the label.
- Avoid walls of text inside the main workflow. Put longer explanations in tooltips, docs, or separate help text when necessary.

## Layout And Spacing

- Keep spacing consistent within and across sections.
- Group related controls tightly enough that they read as one unit, but leave enough separation between groups.
- Avoid oversized controls that make the panel feel noisy or unbalanced.
- Align labels, inputs, and action rows consistently.

## Configuration Vs Sidepanel

- Put persistent preferences and infrequently changed settings in config.
- Put workflow controls, browsing, editing, and immediate actions in the sidepanel.
- Keep developer-only or diagnostic controls out of the main user workflow unless the repo-specific rules explicitly require otherwise.

## Input And Interaction

- Respect RuneLite input expectations and login-state safety rules.
- Do not make normal workflows depend on unusual key combinations unless the speed gain is meaningful.
- If a selection or targeting mode is active, make its state obvious and provide a clear exit path.
- Prefer fewer clicks and clearer feedback for repeated actions.

## Visual Restraint

- Use color to communicate state, priority, or warnings — not decoration.
- Avoid unnecessary borders, heavy emphasis, or competing focal points.
- When multiple statuses are present, keep the meaning of each color and emphasis level consistent.

## Feedback And Empty States

- Empty states should tell the user what to do next.
- Success, failure, and in-progress states should be easy to distinguish.
- Do not rely on logs as the only user-facing feedback for normal workflows.

## Review Standard

Before shipping a UI change, check:

- Is the main task obvious within a few seconds?
- Are the most common actions near where the user needs them?
- Are labels written for users rather than implementation details?
- Are advanced controls separated from everyday usage?
- Does the panel still feel like RuneLite?
