# Domini AI — Guia d'implementació

## Arxius a crear
```
ai/
├── OllamaService.java        Ja creat — client Ollama base
├── AiException.java          Ja creat
├── AiController.java         Endpoints SSE
├── ItineraryAgent.java       Genera itinerari complet
├── DayRefinerAgent.java      Refina un dia amb prompt
├── BudgetAgent.java          Estima pressupost
├── ActivityAgent.java        Suggereix activitats
└── ItineraryParser.java      Parseja JSON del model
```

## Endpoints SSE
```
POST /api/v1/ai/trips/{tripId}/generate
POST /api/v1/ai/trips/{tripId}/days/{dayNumber}/refine  { prompt }
POST /api/v1/ai/trips/{tripId}/refine-all               { prompt }
GET  /api/v1/ai/trips/{tripId}/budget-estimate
```

## Format SSE
```
data: {"type":"start","message":"Generant..."}
data: {"type":"chunk","content":"text parcial"}
data: {"type":"day_complete","dayNumber":1,"day":{DayDTO}}
data: {"type":"complete"}
data: {"type":"error","message":"..."}
```

## System prompt crític (Ollama necessita instruccions explícites)
```
Ets un planificador de viatges expert.
NOMÉS respons amb JSON vàlid, sense text addicional, sense markdown,
sense blocs de codi. La resposta comença amb { i acaba amb }.
Exemple d'activitat correcta:
{"time":"10:00","title":"...","lat":35.67,"lng":139.65,"type":"SIGHTSEEING","estimatedCost":0,"currency":"EUR"}
```

## Temperatura per agent
| Agent | Temperatura | Motiu |
|---|---|---|
| ItineraryAgent | 0.7 | Varietat |
| DayRefinerAgent | 0.5 | Mantenir estructura |
| BudgetAgent | 0.2 | Precisió numèrica |
| ActivityAgent | 0.8 | Creativitat |

## GDPR a la capa AI
- Ollama és LOCAL → les dades dels usuaris NO surten del servidor
- No guardar prompts a la BD (dades personals implícites)
- Si en el futur es migra a Claude API: afegir DPA i actualitzar política de privacitat
