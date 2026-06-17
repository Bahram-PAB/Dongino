# Development Guidelines for Dong (دنگ)

This document outlines the design conventions and technical constraints for future modifications to the Dong group expense sharing applet.

## Technical Constraints & Boundaries

- **Offline-Only Operation**: The application operates strictly offline. All data is persisted locally using a Room Database (`DongDatabase`). Do not introduce cloud synchronizations, authentication servers, logins, or telemetry scripts unless explicitly requested by the user.
- **RTL Support (Right-To-Left)**: The application serves Persian (Farsi) speaking users. All screens, forms, dialogs, and summaries must remain fully Right-to-Left aligned. Global direction is enforced via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
- **System Currency**: The currency unit is fixed to **Toman (تومان)**. Avoid adding options to change currency symbols. All prices must format dynamically via the Persian locale format `formatToman(Long)`.
- **Minimalistic Assets**: No decorative emojis, avatars, or excessive external illustrations should be assigned.

## UI Design & Aesthetic Palette

- **Color Scheme**: Uses a premium material design 3 palette with a Mint Green and deep Slate focus:
  - Primary Theme Main Color: `#10B981` (Beautiful Mint Green)
  - Secondary Main Color: `#0F172A` (Deep Slate)
  - Dark Mode Accent background: `#0B1329`
- **Dynamic Adaptive Icons**: Configured via `ic_launcher_background` and `ic_launcher_foreground` for adaptive launch visuals showing split peer connections.
