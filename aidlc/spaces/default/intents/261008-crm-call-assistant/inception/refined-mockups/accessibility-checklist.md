# Accessibility Checklist — CRM VoIP Call Assistant (WCAG 2.1 AA target)

Demo-level WCAG 2.1 AA pass over the 3 RM screens [mockups]. Vietnamese UI; the
`lang` attribute is set to `vi`.

## Perceivable

- [ ] Speaker turns labeled in text ("RM" / "KH"), never by color alone (S3 transcript)
- [ ] Lead status and priority conveyed by text + icon, not color alone (S1/S2 badges)
- [ ] Text contrast ≥ 4.5:1 (normal) / 3:1 (large) against MB surfaces — verify Figma tokens
- [ ] Audio player exposes a text transcript (the transcript panel itself serves this)
- [ ] Score/criteria shown as number + label, not color-only (S3 scorecard)

## Operable

- [ ] All actions keyboard-operable: start call, end call, play/seek audio, edit form, sync, download .wav
- [ ] Visible focus indicator on every interactive element (no `outline:none` without replacement)
- [ ] Logical tab order per screen (header → panels left-to-right, top-to-bottom)
- [ ] Audio player controllable by keyboard; clicking a transcript turn keeps focus and seeks
- [ ] Touch targets ≥ 44×44px (phone-call link, end button, download)

## Understandable

- [ ] `lang="vi"` declared; UI copy in Vietnamese
- [ ] Form fields have visible labels above inputs (AutoKeyinForm), not placeholder-only
- [ ] Error messages specific and in Vietnamese ("Số tiền không hợp lệ", "Không tải được bản ghi")
- [ ] End-call and CRM-sync are explicit user actions (no auto-submit of consequential actions)
- [ ] Consistent navigation (`Quay lại` back control on every screen)

## Robust

- [ ] Semantic HTML (headings in order, lists for lists, native `<audio>`, `<button>` over div)
- [ ] `aria-live="polite"` on copilot answers (S2) and on the S3 processing→results transition
- [ ] ARIA labels on icon-only controls (play, download, call)

## AI-specific (content safety / clarity)

- [ ] AI-extracted form values are editable before CRM sync (user stays in control, FR5.2)
- [ ] Processing and error states are explicit while the pipeline runs (FR1.3)

## Assumptions & Open Questions

- Contrast verification depends on the final Figma hex values; flagged for the
  implementation step [assumption].
