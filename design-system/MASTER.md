# SketchPilot Design System (MASTER)

Product: developer tool / AI-agent IDE for Sketchware Pro. Android native, Material 3, adaptive,
dark-and-light, content-dense but calm. Audience: technical users who need fast, trustworthy control.

## Visual direction
Premium developer tool: restrained, high-contrast, information-first. Distinctive through typography,
spacing rhythm, tonal surfaces and status signaling — not through gradients, glass or decoration.

## Color
- Dynamic color on Android 12+ (light and dark), otherwise custom M3 light/dark tonal schemes.
- Semantic roles only: primary/onPrimary, surface/onSurface, surfaceVariant/onSurfaceVariant,
  primaryContainer/onPrimaryContainer, error/onError, outline.
- Status is always text + icon/color; never color alone.
- Contrast: body >= 4.5:1, large glyphs >= 3:1 in both themes.

## Typography
- M3 roles: headlineSmall screen title, titleMedium list item title, bodyMedium secondary text,
  bodySmall metadata, labelLarge buttons.
- Bengali and English share the same scale; system font scaling respected.

## Shape & elevation
- Shapes: extraSmall 4dp, small 8dp (fields), medium 12dp (cards/dialogs), large 16dp (sheets).
- Elevation: tonal elevation 0–3dp. No arbitrary shadows, no gradients.

## Spacing & layout
- 4/8dp rhythm: 8/12/16/24/32. Phone content inset 16dp, tablet 24dp.
- Compact width: bottom navigation. Medium/Expanded: navigation rail. No horizontal scroll.
- Scaffold insets + list bottom padding so no content hides behind bars.

## Motion
- Nav transitions: 200–250ms fade/slide, directional. Respect reduced motion (no decorative motion).
- Loading: inline progress on the acting control; disabled state while busy.

## States
Loading, empty, error (+retry), success, disabled, pressed, focused — all designed and implemented.

## Anti-patterns
Emoji icons, icon-only nav, placeholder-only labels, hardcoded hex in screens, repeated generic cards
without hierarchy, instant unexplained state changes, blank empty states, visible API keys.
