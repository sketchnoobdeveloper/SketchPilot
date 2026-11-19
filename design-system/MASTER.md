# SketchPilot Design System (MASTER)

Product: developer tool / AI-agent IDE for Sketchware Pro. Mobile-first, Android Material 3, dark-first,
content-dense but calm. Audience: technical users who need fast, trustworthy control over projects.

## Style
- Material 3 expressive-but-disciplined: dynamic color when available, otherwise a dark tonal scheme.
- No emoji as icons. Use Material vector icons from a single family (material-icons-core).
- One primary CTA per screen. Secondary actions are tonal or text buttons.
- Elevation: cards use `ElevatedCard`/surface tonal layering; avoid arbitrary shadows.

## Color (semantic tokens only)
- Use `MaterialTheme.colorScheme.*`: `primary`, `onPrimary`, `surface`, `onSurface`, `surfaceVariant`,
  `onSurfaceVariant`, `error`, `onError`, `secondaryContainer`, `onSecondaryContainer`.
- Never hardcode hex in screens. Status text uses `error` for failures, `secondary` for neutral status.
- Contrast: body >= 4.5:1, large text/icons >= 3:1. Dark mode must be checked independently.

## Typography
- Material 3 type roles: `headlineSmall` screen title, `titleMedium` card title, `bodyMedium` body,
  `bodySmall` metadata, `labelLarge` buttons. Never raw `sp` values in body text.
- Prefer wrapping; never truncate critical info without an expansion path.

## Spacing & layout
- 4/8dp rhythm: 8/12/16/24/32. Content padding 16dp on phones, 24dp on tablets/landscape.
- Scaffold handles top app bar and bottom bar insets; list content gets bottom padding so nothing is hidden.
- No horizontal scrolling. Long text wraps. Empty states replace blank screens.

## Interaction & accessibility
- All interactive targets >= 48dp height. Buttons provide ripple/press state by default.
- Loading: disable the acting button and show `CircularProgressIndicator` inline.
- Errors: show near the problem with semantic error color and a recovery action (retry/edit).
- Provider API key uses a password field with explicit Show/Hide control; never log or display the key.
- Navigation: bottom bar max 5 destinations, each with icon + label; active state uses M3 indicator.
- Every icon has `contentDescription`; status is not conveyed by color alone (text included).
- Respect system font scaling; no fixed-height text containers that clip.

## Animation
- No decorative animation. Any future motion: 150–300ms, transform/opacity only, respect reduced motion.

## Anti-patterns (avoid)
- Emoji icons, icon-only nav items, placeholder-only labels, hardcoded colors, instant unexplained state
  changes, disabled buttons that still look enabled, blank empty states, API keys in plaintext fields.
