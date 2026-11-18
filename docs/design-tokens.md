# Ghost Town Recomp UI design tokens

The console preview uses a dark storm-grey base and one teal accent. All Compose
screens consume the constants in
`android/app/src/main/java/com/overcookedrecomp/app/ui/theme/GhostTownTheme.kt`.

| Token | Value | Purpose |
|---|---:|---|
| Storm | #0B1118 | Main background |
| Storm Raised | #141E29 | Elevated background |
| Surface | #182431 | Cards and panels |
| Surface Elevated | #202F3D | Focused/raised cards |
| Accent | #54D6C5 | Active tab, focus, primary action |
| Accent Deep | #1E8F85 | Accent gradient/depth |
| Text Primary | #F3F7F8 | Main text |
| Text Secondary | #B4C2CA | Supporting text |
| Text Muted | #82939E | Disabled/tertiary text |
| Warning | #FFC66D | Runtime and device warnings |
| Danger | #FF8585 | Errors |

Spacing is based on 4dp increments: page 28dp, section 20dp, card gap 14dp,
small gap 8dp, minimum touch target 48dp. Focus transitions use 190ms.
The default Android sans-serif font (Roboto) is used; no proprietary font asset
is bundled. System font scaling remains enabled.
