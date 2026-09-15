---
name: DriveMarket
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#006c4a'
  on-secondary: '#ffffff'
  secondary-container: '#82f5c1'
  on-secondary-container: '#00714e'
  tertiary: '#46566c'
  on-tertiary: '#ffffff'
  tertiary-container: '#5e6e85'
  on-tertiary-container: '#e9f0ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#85f8c4'
  secondary-fixed-dim: '#68dba9'
  on-secondary-fixed: '#002114'
  on-secondary-fixed-variant: '#005137'
  tertiary-fixed: '#d3e4fe'
  tertiary-fixed-dim: '#b7c8e1'
  on-tertiary-fixed: '#0b1c30'
  on-tertiary-fixed-variant: '#38485d'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers a modern, high-trust automotive marketplace optimized for Android handhelds and tablets. Drawing heavily from refined Material 3 principles infused with crisp European minimalism, the aesthetic focuses on utility, clarity, and structural balance. 

Automotive transactions require high transactional trust, dense technical information handling, and emotional excitement around vehicle ownership. The experience balances calm, airy white space with rigorous, data-rich specs:
- **Surface Quality:** Pure crisp whites layered against slate undertones create pristine, showroom-like clarity.
- **Voice & Tone:** Confident, objective, frictionless, and transparent.
- **Visual Weight:** Light surfaces punctuated by bold price markers, high-contrast badges for transparent market values, and fluid, responsive touch targets.

## Colors

The color palette is built around psychological certainty, legibility, and high visual hierarchy.

- **Primary (`#2563EB` - Cobalt Blue):** Anchors core actions, active selection states, key interaction points, and verified owner indicators.
- **Secondary (`#059669` - Emerald Green):** Exclusively reserved for high-value signals: great price evaluations, CARFAX-clean badges, instant financing pre-approvals, and deal ratings.
- **Neutral Primary (`#0F172A` - Deep Slate Navy):** High-contrast, glare-free ink for all primary typography, icons, and prominent headers.
- **Neutral Secondary / Tertiary (`#64748B` - Slate):** Supporting metadata, vehicle mileage, vin numbers, and unselected states.
- **Neutral Surface & Outlines:** Canvas base starts at pure `#FFFFFF`, stepping to `#F8FAFC` for secondary backgrounds, with hairline structural borders tinted in `#E2E8F0`.

## Typography

Typography relies on **Inter** across all tiers to provide uncompromised clarity, uniform tall x-height, and superior numeric legibility for spec grids and price points.

- Numbers and prices feature tabular figures (`tnum`) for instant scanning across list items.
- Dynamic mobile scaling switches large display headings down on viewports under 600dp to avoid awkward word wraps in multi-word vehicle names (e.g., "Mercedes-Benz AMG GT").
- Micro-labels (`label-sm`, `label-md`) employ medium-to-semibold weights with subtle letter-spacing (`+0.02em`) to guarantee quick glances in outdoor automotive conditions.

## Layout & Spacing

The layout conforms to a fluid 4-column structure on standard Android phones (0–599dp), expanding to an 8-column layout on foldables and tablets (600–839dp), and a 12-column fixed grid on large displays (840dp+).

- **Margins & Gutters:** Base margins strictly utilize `1rem` (16dp) on mobile viewports, scaling up to `1.5rem` (24dp) on tablets.
- **Rhythm:** Spacing follows a 4dp base scale. Dense informational areas (e.g., vehicle attribute badges) stay locked to `space-xs` (4dp) and `space-sm` (8dp), while screen sections separate through `space-lg` (24dp) and `space-xl` (32dp).
- **Safe Area Insets:** Layout respects Android system gesture areas, bottom navigation bars, and cutouts with automated bottom padding extensions.

## Elevation & Depth

This design system avoids heavy shadows, opting for subtle ambient occlusion paired with low-contrast outlines:

- **Surface Levels:** 
  - Level 0 (Base Canvas): `#FFFFFF` or subtle neutral tint `#F8FAFC`.
  - Level 1 (Cards, Floating Lists): `#FFFFFF` accompanied by a crisp `1px solid #E2E8F0` border and an ambient shadow: `0px 2px 6px -1px rgba(15, 23, 42, 0.04), 0px 4px 12px -2px rgba(15, 23, 42, 0.03)`.
  - Level 2 (Bottom Sheets, Elevated Drawers): Elevated background `#FFFFFF`, bordered with `#E2E8F0`, with `0px 8px 24px -4px rgba(15, 23, 42, 0.08)`.
  - Level 3 (Sticky Bottom App Bars, Toolbars): Hairline top/bottom divider `#E2E8F0` with translucent backdrop blur (`rgba(255, 255, 255, 0.94)` with `backdrop-filter: blur(12px)`).

## Shapes

The design system adopts **Level 2 (Rounded)** with a clear geometric scale:
- Standard UI elements (inputs, normal buttons, listing spec tags): `0.5rem` (8dp) to `0.75rem` (12dp).
- Core listing cards and hero containers use signature Material 3 `rounded-2xl` (`1rem` / 16dp).
- Filter chips, quick tags, and status deal pills use fully rounded caps (`pill-shaped` / 9999px).

## Components

### Buttons
- **Primary:** Solid `#2563EB` fill, white text, height 48dp, corner radius 12dp. Pressed state shifts to `#1D4ED8`.
- **Secondary:** Surface `#F1F5F9`, text `#0F172A`, height 48dp, radius 12dp.
- **Tertiary/Ghost:** Transparent surface, `#2563EB` or `#0F172A` text, 48dp touch container with 0dp border.

### Listing Cards (Vehicle Cards)
- Bounded inside `rounded-2xl` (`16dp`), surrounded by a `1px solid #E2E8F0` border.
- 16:9 ratio photo container at top with rounded top corners.
- Floating top badges for "Great Deal" (Emerald `#059669` pill with white text) or "New" status.
- Price displayed prominently using `headline-md` in `#0F172A`, alongside calculated monthly loan estimates in `body-sm` (`#64748B`).
- Structured 2x2 specification tag group (Mileage, Fuel Type, Transmission, Location) with `#F8FAFC` chip backings.

### Filter Chips
- Height 36dp, pill-shaped (`rounded-full`).
- **Default:** Background `#FFFFFF`, border `1px solid #E2E8F0`, text `#0F172A`.
- **Selected:** Background `#EFF6FF`, border `1.5px solid #2563EB`, text `#2563EB`, leading checkmark icon.

### Input Fields & Search
- Height 52dp, corner radius 12dp.
- Crisp white fill, border `1px solid #CBD5E1`. On focus: `2px solid #2563EB` with no offset outline.
- Clear button and trailing filter action buttons vertically centered.

### Selection Controls
- Checkboxes and radios use `#2563EB` active state with an intentional 2dp inner border ring for touch feedback.

### Bottom Navigation Bar
- Grounded at 64dp height + system navigation inset.
- `#FFFFFF` surface with top `1px solid #E2E8F0` rule.
- Active item marked with a soft `#DBEAFE` rounded pill indicator behind a `#2563EB` icon, paired with `label-sm` caption text.

### Verification & Deal Badges
- High-trust badges (e.g., "Verified Seller", "Clean History", "Great Deal"): Height 24dp, emerald tint `#ECFDF5`, text `#059669`, with a miniature verified check icon.