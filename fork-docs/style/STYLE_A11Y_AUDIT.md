# Violence Style: accessibility audit (S6)

Method: code review of every skin file, the unit tests of the colour maths, and the S1 to S5 reviews by the owner. **No TalkBack run, no 200 percent font run and no high-contrast run was possible on the build PC** (no device tooling): those rows say UNCLEAR and are on the final QA checklist (`STYLE_FINAL_QA.md`). Severity: High (blocks use), Medium (hard to use), Low (minor).

| # | Finding | Severity | Status |
|---|---|---|---|
| 1 | The 9 colour preset swatches in Settings were 28 dp touch targets and announced only by colour name | High | **Fixed**: each has a 48 dp target (`minimumInteractiveComponentSize`, wraps in a `FlowRow`), role radio button and `selected` semantics |
| 2 | The three colour-picker sliders (hue, saturation, lightness) had no accessible name | Medium | **Fixed**: content descriptions added (new strings `vs_hue`, `vs_saturation`, `vs_lightness`) |
| 3 | The system high-contrast text setting was read but ignored | Medium | **Fixed**: while it is on, the skin uses solid surfaces (reduce transparency); the key is undocumented, so a device without it simply reads as off (UNCLEAR on devices) |
| 4 | Toggling the style rebuilt the app's UI subtree and could drop open pages, scroll positions and dialogs | Medium (usability) | **Fixed**: the app content now sits in a movable slot so its state survives a toggle |
| 5 | Glow over text: contrast of headings, rows, scrims | High if wrong | **Verified by test**: the contrast guard (upper 30 percent of the screen: white 3:1; the rest: the app's `#8E8E93` secondary text 4.5:1) for every preset and extreme colour, style, intensity and motion; hero card scrim (white labels 4.5:1, titles 3:1 on pure white artwork); selected capsule (white 4.5:1 for every preset); switch thumb 3:1; chips 4.4:1; dialog panel tint 12:1; detail glow 3:1 |
| 6 | Heading semantics | Medium | **Done in S3**: every skinned section title is a TalkBack heading; hairline and dot are not announced. Not verified with TalkBack (UNCLEAR) |
| 7 | Selected state conveyed by more than colour | Medium | **Done in S4**: capsule shape plus `Role.Tab` and `selected` on the expanded tabs and the plain bar's tabs. **Open, Low:** the inline (folded) tab and the standalone Search tab have no `selected` semantics |
| 8 | Font scaling to 200 percent | Medium | Designed: serif capped at 1.3x, titles wrap to two lines then ellipsize, the hairline shortens then disappears, "Show all" has a 48 dp target. Not verified at 200 percent on a device (UNCLEAR) |
| 9 | Touch targets of the skinned controls | Medium | Three-dot row button is at least 48 dp (S3; its icon may sit up to 6 dp left of before, UNCLEAR). Existing 44 dp top controls were left as they are (changing them would alter the layout) |
| 10 | Reduce motion | Medium | The background clock stops under the system animator scale of 0 and the in-app Reduce animation; the S1 crossfade becomes a cut; S4 added no animation. The nav capsule's travel is the app's existing spring and is unchanged |
| 11 | Reduce transparency | Medium | Follows the in-app "Reduce dynamic blur", a skin switch, and now system high contrast: panels, nav, mini player and top pill become near-solid dark with rims and accent kept. Android has no public system reduce-transparency setting that was found |
| 12 | Light theme | Low | Per the decision, the skin applies its dark look in any theme and forces light status-bar icons; the previous theme choice is stored untouched and returns when the skin is off |
| 13 | Colour-only information | Medium | The accent never carries information alone: the selected tab has the capsule and semantics, the selected preset has a ring and `selected`, errors have text |
| 14 | TalkBack labels | Low | Unchanged for every existing control (the skin adds no labels it removes); new settings rows are text rows |
| 15 | Focus order | Low | Not changed by the skin (layout untouched). UNCLEAR on a device |
| 16 | System bold text | Low | Compose applies the system font weight adjustment to text; not verified with the Inter variable font (UNCLEAR) |
| 17 | Text over artwork in the full player | Medium | Not changed by the skin (the player keeps its own colouring and contrast logic) |

Counts: 17 findings; 4 fixed in S6, 5 verified or done in earlier stages with unit tests, 1 open Low (item 7), the rest need a device (UNCLEAR).
