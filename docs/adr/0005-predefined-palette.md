# ADR-0005: Predefined brand palette, no dynamic color in the MVP

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

Material You dynamic color derives the app's colors from the wallpaper. SpaceSaver relies on color for meaning: a "savings" green, distinct video and image category colors in the storage bar, and a warning amber. With dynamic color, those roles could clash with or blend into wallpaper-derived colors, and contrast could not be verified ahead of time.

## Decision

Ship a fixed, hand-picked palette (calm teal primary, indigo secondary, amber tertiary) with light and dark schemes, plus semantic extended colors (`savings`, `videoCategory`, `imageCategory`, `otherCategory`, `warning`) exposed through a `SpaceSaverColors` CompositionLocal. The exact values are listed in the plan, Section 6.2.

The theme mode (System / Light / Dark) is user-selectable; dynamic color is off.

A unit test asserts every token's hex value, and a contrast test checks WCAG AA for every on-color pair in both schemes (plan Tasks 1.1 and 1.2). If a pair fails, the hex is adjusted minimally and the change is recorded in the "Palette adjustments" section below.

## Consequences

- Consistent brand and verified contrast in both themes.
- The app does not follow the user's wallpaper colors. An optional dynamic color toggle is on the post-MVP roadmap.
- Color is never the only signal: category segments also carry labels and icons.

## Derived color roles

The plan specifies 24 roles per scheme. Material 3 components also read roles the plan does not list (cards, navigation bars, dialogs, and sheets use the surface containers). Left unset, these fall back to Material's purple baseline and clash with the teal brand, so they are derived as neutral teal-greys between `surface` and `surfaceVariant`:

| Role | Light | Dark |
|---|---|---|
| surfaceTint | `#0F766E` (primary) | `#5EEAD4` (primary) |
| inverseSurface | `#2D3534` | `#DDE4E2` |
| inverseOnSurface | `#ECF2F0` | `#2B3231` |
| inversePrimary | `#5EEAD4` | `#0F766E` |
| scrim | `#000000` | `#000000` |
| surfaceBright | `#FFFFFF` | `#333C3B` |
| surfaceDim | `#D8E0DE` | `#121A19` |
| surfaceContainerLowest | `#FFFFFF` | `#0B1110` |
| surfaceContainerLow | `#F4F8F7` | `#172120` |
| surfaceContainer | `#EEF3F2` | `#1B2524` |
| surfaceContainerHigh | `#E9EFEE` | `#252F2E` |
| surfaceContainerHighest | `#E3ECEA` (surfaceVariant) | `#303A39` |

`PaletteTest` pins these values, and `ContrastTest` checks `onSurface` and `onSurfaceVariant` on every one of them (all pass AA).

In the light theme, `savings` and `warning` text reach 4.5:1 only on `surface`, `surfaceContainerLowest`, and `surfaceContainerLow`. Components that show them use the shared card (`surface` with an `outlineVariant` border) for that reason.

## Palette adjustments

None. Every specified pair passed WCAG AA as given.
