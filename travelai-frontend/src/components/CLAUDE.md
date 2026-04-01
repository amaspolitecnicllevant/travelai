# Components — Guia d'implementació

## common/
```
BaseButton.vue       Props: label, variant(primary|secondary|danger), loading, disabled, size
BaseInput.vue        Props: modelValue, label, placeholder, error, type, required
BaseModal.vue        Props: show, title. Slots: default, footer
BaseCard.vue         Wrapper .card, slot default
BaseAvatar.vue       Props: src, name, size. Inicials si no hi ha imatge
BaseBadge.vue        Props: label, color
LoadingSpinner.vue   Props: size, fullscreen
EmptyState.vue       Props: title, description, actionLabel, actionTo
```

## trip/
```
TripCard.vue         Props: trip(TripSummaryDTO)
TripForm.vue         Props: initialData, loading. Emits: submit(formData)
DayEditor.vue        Props: day, tripId. Inclou AiPromptInput
ActivityCard.vue     Props: activity
RatingStars.vue      Props: score, interactive, count. Emits: rate(score)
```

## ai/
```
AiPromptInput.vue    Props: placeholder, loading. Emits: submit(prompt)
StreamingText.vue    Props: text, streaming. Cursor parpadeant
AiStatusBadge.vue    Props: status(idle|generating|complete|error)
```

## legal/ (GDPR — tots obligatoris)
```
CookieBanner.vue     Bàner inferior cookies. Usa consentStore
ConsentCheckbox.vue  Props: modelValue, label, linkText, linkTo, required, version
GdprFooter.vue       Footer amb tots els enllaços legals
```
