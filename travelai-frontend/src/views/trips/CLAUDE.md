# Vistes de Trips

## TripPlannerView.vue — vista central
Layout:
- Header: títol + destí + dates + botons (Generar IA, Publicar, Editar)
- Sidebar: llista de dies
- Panel: DayEditor + AiPromptInput + mapa bàsic

## CreateTripView.vue — 2 passos
1. Formulari metadades (visibility = PRIVATE per defecte — GDPR)
2. Redirect al planner amb botó "Generar amb IA"

## TripDetailView.vue — vista pública
- Itinerari complet (sense edició)
- RatingStars interactiu (si autenticat i no és el propietari)
- Botó "Desar en favorits"
