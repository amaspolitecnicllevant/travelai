#!/usr/bin/env bash
# =============================================================
# TravelAI — Llança agents Claude Code en paral·lel
# v2 — Inclou agent LEGAL/GDPR com a 7è panell
# =============================================================
set -e

GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${GREEN}✓${NC} $1"; }
info() { echo -e "${CYAN}→${NC} $1"; }

SESSION="travelai-agents"
ROOT="$(pwd)"
BACK="$ROOT/travelai-backend"
FRONT="$ROOT/travelai-frontend"

tmux kill-session -t "$SESSION" 2>/dev/null || true

info "Creant sessió tmux: $SESSION"

# Layout 7 panells:
#  ┌──────────────┬──────────────┬──────────────┐
#  │ 0: AUTH      │ 1: TRIP      │ 2: AI        │
#  ├──────────────┼──────────────┼──────────────┤
#  │ 3: LEGAL     │ 4: FRONTEND  │ 5: INFRA     │
#  ├──────────────┴──────────────┴──────────────┤
#  │ 6: MONITOR (logs Docker)                   │
#  └────────────────────────────────────────────┘

tmux new-session -d -s "$SESSION" -x 240 -y 60

# Fila superior: 3 panells horitzontals
tmux split-window -h -t "$SESSION:0"
tmux split-window -h -t "$SESSION:0.1"

# Fila mitja: duplicar cada columna verticalment
tmux split-window -v -t "$SESSION:0.0"
tmux split-window -v -t "$SESSION:0.2"
tmux split-window -v -t "$SESSION:0.4"

# Fila inferior: un panell ample per al monitor
tmux split-window -v -t "$SESSION:0.0"

tmux select-layout -t "$SESSION:0" tiled

# ── Panell 0 — AUTH ──────────────────────────────────────────
tmux select-pane -t "$SESSION:0.0" -T "AUTH"
tmux send-keys -t "$SESSION:0.0" "cd '$BACK/src/main/java/com/travelai/domain/auth'" Enter
tmux send-keys -t "$SESSION:0.0" "claude" Enter
log "Panell 0: Agent AUTH"
sleep 1

# ── Panell 1 — TRIP ──────────────────────────────────────────
tmux select-pane -t "$SESSION:0.1" -T "TRIP"
tmux send-keys -t "$SESSION:0.1" "cd '$BACK/src/main/java/com/travelai/domain/trip'" Enter
tmux send-keys -t "$SESSION:0.1" "claude" Enter
log "Panell 1: Agent TRIP"
sleep 1

# ── Panell 2 — AI ────────────────────────────────────────────
tmux select-pane -t "$SESSION:0.2" -T "AI"
tmux send-keys -t "$SESSION:0.2" "cd '$BACK/src/main/java/com/travelai/domain/ai'" Enter
tmux send-keys -t "$SESSION:0.2" "claude" Enter
log "Panell 2: Agent AI"
sleep 1

# ── Panell 3 — LEGAL/GDPR ────────────────────────────────────
tmux select-pane -t "$SESSION:0.3" -T "LEGAL"
tmux send-keys -t "$SESSION:0.3" "cd '$BACK/src/main/java/com/travelai/domain/legal'" Enter
tmux send-keys -t "$SESSION:0.3" "claude" Enter
log "Panell 3: Agent LEGAL/GDPR"
sleep 1

# ── Panell 4 — FRONTEND ──────────────────────────────────────
tmux select-pane -t "$SESSION:0.4" -T "FRONTEND"
tmux send-keys -t "$SESSION:0.4" "cd '$FRONT/src'" Enter
tmux send-keys -t "$SESSION:0.4" "claude" Enter
log "Panell 4: Agent FRONTEND"
sleep 1

# ── Panell 5 — INFRA ─────────────────────────────────────────
tmux select-pane -t "$SESSION:0.5" -T "INFRA"
tmux send-keys -t "$SESSION:0.5" "cd '$BACK/src/main/resources'" Enter
tmux send-keys -t "$SESSION:0.5" "claude" Enter
log "Panell 5: Agent INFRA"
sleep 1

# ── Panell 6 — MONITOR ───────────────────────────────────────
tmux select-pane -t "$SESSION:0.6" -T "MONITOR"
tmux send-keys -t "$SESSION:0.6" "cd '$ROOT'" Enter
tmux send-keys -t "$SESSION:0.6" "docker-compose logs -f backend 2>/dev/null || echo 'Docker no actiu encara'" Enter
log "Panell 6: Monitor logs"

tmux set-option -t "$SESSION" pane-border-status top
tmux set-option -t "$SESSION" pane-border-format " #{pane_title} "
tmux select-pane -t "$SESSION:0.0"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  7 agents preparats en tmux                ${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  Connectar-se:  ${CYAN}tmux attach -t $SESSION${NC}"
echo ""
echo -e "  Navegació:"
echo -e "    ${CYAN}Ctrl+b → fletxes${NC}   moure entre panells"
echo -e "    ${CYAN}Ctrl+b z${NC}            zoom panell actual"
echo -e "    ${CYAN}Ctrl+b d${NC}            desconnectar"
echo ""
echo -e "  Panells:"
echo -e "    ${CYAN}0${NC} AUTH    — domain/auth/    (primer!)"
echo -e "    ${CYAN}1${NC} TRIP    — domain/trip/"
echo -e "    ${CYAN}2${NC} AI      — domain/ai/"
echo -e "    ${CYAN}3${NC} LEGAL   — domain/legal/   (GDPR)"
echo -e "    ${CYAN}4${NC} FRONT   — frontend/src/"
echo -e "    ${CYAN}5${NC} INFRA   — resources/"
echo -e "    ${CYAN}6${NC} MONITOR — docker logs"
echo ""
echo -e "  Ordre: llança AUTH primer, la resta en paral·lel."
echo ""
