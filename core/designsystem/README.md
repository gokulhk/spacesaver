# `:core:designsystem`

Theme and shared stateless components (plan Section 6).

## Entry points

- `SpaceSaverTheme(themeMode)`: wrap every screen and preview. `SpaceSaverTheme.colors` gives the extended colors (`savings`, `videoCategory`, `imageCategory`, `otherCategory`, `warning`).
- `Spacing`: spacing tokens (4, 8, 12, 16, 24, 32 dp). Feature code uses these instead of raw dp.
- `SpaceSaverIcons`: every icon the app uses.
- `@PreviewLightDark`, `@PreviewComponents`: multipreview annotations (light, dark, and 200% font).
- Components: `SavingsBanner`, `StorageBar`, `PlanSummaryCard`, `SuggestionCard`, `MediaListRow`, `BeforeAfterSlider`, `BatchProgressRow`, `PrimaryActionButton`, `ConfirmDialog`, `EmptyState`, `PermissionRationale`.

## Conventions

- Components are stateless. They take display-ready text: sizes as `SizeText(display, spoken)` so TalkBack reads "12.4 gigabytes", not "12.4 GB".
- Images (thumbnails, before/after) are composable slots, so this module never depends on an image loader.
- Color is never the only signal: categories and statuses also have icons and labels.
- Components that paint their own background use a `Surface` so text gets a matching content color.

## Dependencies

- **May depend on:** `:core:model`, Compose, Material 3, Material icons.
- **Must not depend on:** `:core:domain`, `:core:data`, or any feature module.

## Tests

`PaletteTest`, `ContrastTest`, theme tests, component behavior tests, and Roborazzi screenshots in `src/test/screenshots/`. See [`docs/testing.md`](../../docs/testing.md).
