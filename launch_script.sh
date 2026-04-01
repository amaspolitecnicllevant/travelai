#!/usr/bin/env bash
# =============================================================
# TravelAI — Script de llançament complet
# Executar des de la carpeta PARE del projecte
# (la carpeta que CONTÉ la carpeta travelai/)
#
# Ús:
#   ./launch.sh              Primera vegada (instal·la tot)
#   ./launch.sh --start      Només arrancar (ja instal·lat)
#   ./launch.sh --stop       Parar tots els contenidors
#   ./launch.sh --restart    Parar i tornar a arrancar
#   ./launch.sh --logs       Veure logs en temps real
#   ./launch.sh --status     Estat dels contenidors
# =============================================================
set -e

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'
RED='\033[0;31m'; BOLD='\033[1m'; NC='\033[0m'

log()     { echo -e "${GREEN}✓${NC} $1"; }
info()    { echo -e "${CYAN}→${NC} $1"; }
warn()    { echo -e "${YELLOW}!${NC} $1"; }
error()   { echo -e "${RED}✗${NC} $1"; exit 1; }
title()   { echo -e "\n${BOLD}${CYAN}$1${NC}"; }

PROJECT_DIR="$(pwd)/travelai"
MODE="${1:---setup}"

# =============================================================
# BANNER
# =============================================================
echo ""
echo -e "${CYAN}  ████████╗██████╗  █████╗ ██╗   ██╗███████╗██╗      █████╗ ██╗${NC}"
echo -e "${CYAN}     ██║   ██╔══██╗██╔══██╗██║   ██║██╔════╝██║     ██╔══██╗██║${NC}"
echo -e "${CYAN}     ██║   ██████╔╝███████║██║   ██║█████╗  ██║     ███████║██║${NC}"
echo -e "${CYAN}     ██║   ██╔══██╗██╔══██║╚██╗ ██╔╝██╔══╝  ██║     ██╔══██║██║${NC}"
echo -e "${CYAN}     ██║   ██║  ██║██║  ██║ ╚████╔╝ ███████╗███████╗██║  ██║██║${NC}"
echo -e "${CYAN}     ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚══════╝╚═╝  ╚═╝╚═╝${NC}"
echo -e "  Launcher — mode: ${YELLOW}$MODE${NC}"
echo ""

# =============================================================
# FUNCIONS
# =============================================================

check_prerequisites() {
  title "Comprovant prerequisits..."

  command -v docker      >/dev/null 2>&1 || error "Docker no instal·lat. Baixa'l de https://docker.com"
  command -v docker-compose >/dev/null 2>&1 || error "docker-compose no instal·lat."
  command -v node        >/dev/null 2>&1 || error "Node.js no instal·lat. Baixa'l de https://nodejs.org"
  command -v tmux        >/dev/null 2>&1 || {
    warn "tmux no instal·lat. Els agents no podran obrir-se."
    warn "Instal·la'l amb: brew install tmux (Mac) / apt install tmux (Linux)"
  }

  # Comprovar que Docker està corrent
  docker info >/dev/null 2>&1 || error "Docker no està corrent. Obre Docker Desktop."

  log "Tots els prerequisits OK"
}

check_project_exists() {
  if [ ! -d "$PROJECT_DIR" ]; then
    error "No s'ha trobat la carpeta 'travelai/' en el directori actual.\nAssegura't d'executar aquest script des de la carpeta PARE del projecte."
  fi
  if [ ! -f "$PROJECT_DIR/docker-compose.yml" ]; then
    error "No s'ha trobat docker-compose.yml dins de travelai/.\nExecuta primer init-script.sh des de dins de travelai/"
  fi
}

setup_env() {
  title "Comprovant .env..."
  if [ ! -f "$PROJECT_DIR/.env" ]; then
    warn ".env no trobat. Creant-lo des de .env.example..."
    if [ -f "$PROJECT_DIR/.env.example" ]; then
      cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
      warn "Revisa i edita $PROJECT_DIR/.env abans de continuar."
    else
      error "Tampoc hi ha .env.example. Executa primer init-script.sh"
    fi
  fi
  log ".env trobat"
}

install_frontend() {
  title "Instal·lant dependències del frontend..."
  if [ ! -d "$PROJECT_DIR/travelai-frontend/node_modules" ]; then
    info "Instal·lant node_modules (primera vegada)..."
    cd "$PROJECT_DIR/travelai-frontend"
    npm install
    cd - > /dev/null
    log "Dependències del frontend instal·lades"
  else
    log "node_modules ja existeix, saltant npm install"
  fi
}

start_infra() {
  title "Arrancant infraestructura..."
  cd "$PROJECT_DIR"

  info "Arrancant postgres, redis, minio, ollama..."
  docker-compose up -d postgres redis minio ollama

  info "Esperant que els serveis estiguin llestos..."
  local retries=0
  until docker-compose exec -T postgres pg_isready -U travelai -d travelai >/dev/null 2>&1; do
    retries=$((retries + 1))
    [ $retries -gt 20 ] && error "PostgreSQL no arranca. Revisa els logs: docker-compose logs postgres"
    printf "."
    sleep 2
  done
  echo ""
  log "PostgreSQL llest"

  until docker-compose exec -T redis redis-cli -a "${REDIS_PASSWORD:-redis_dev}" ping >/dev/null 2>&1; do
    retries=$((retries + 1))
    [ $retries -gt 20 ] && error "Redis no arranca."
    printf "."
    sleep 2
  done
  log "Redis llest"

  log "MinIO arrancant en segon pla..."
  log "Ollama arrancant en segon pla..."

  cd - > /dev/null
}

pull_ollama_model() {
  title "Comprovant model Ollama..."
  cd "$PROJECT_DIR"

  local model
  model=$(grep OLLAMA_MODEL .env 2>/dev/null | cut -d= -f2 || echo "qwen2.5:7b")
  model="${model:-qwen2.5:7b}"

  info "Esperant que Ollama estigui llest..."
  local retries=0
  until docker-compose exec -T ollama curl -sf http://localhost:11434/api/tags >/dev/null 2>&1; do
    retries=$((retries + 1))
    [ $retries -gt 30 ] && error "Ollama no arranca. Revisa: docker-compose logs ollama"
    printf "."
    sleep 3
  done
  echo ""
  log "Ollama llest"

  # Comprovar si el model ja està descarregat
  if docker-compose exec -T ollama ollama list 2>/dev/null | grep -q "${model%%:*}"; then
    log "Model $model ja descarregat"
  else
    warn "Model $model NO trobat. Descarregant (~4.5 GB, pot trigar uns minuts)..."
    docker-compose exec -T ollama ollama pull "$model"
    log "Model $model descarregat"
  fi

  cd - > /dev/null
}

start_app() {
  title "Arrancant backend i frontend..."
  cd "$PROJECT_DIR"

  docker-compose up -d backend frontend nginx

  info "Esperant que el backend arranqui (primera vegada pot trigar 2-3 min)..."
  local retries=0
  until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
    retries=$((retries + 1))
    if [ $retries -gt 60 ]; then
      warn "El backend triga molt. Comprova els logs:"
      warn "docker-compose logs -f backend"
      break
    fi
    printf "."
    sleep 3
  done
  echo ""
  log "Backend llest (o comprova els logs si ha trigat)"

  cd - > /dev/null
}

launch_agents() {
  title "Llançant agents Claude Code..."

  if ! command -v tmux >/dev/null 2>&1; then
    warn "tmux no instal·lat — saltant llançament d'agents"
    warn "Pots llançar-los manualment des de VSCode: Ctrl+Shift+P → Tasks: Run Task"
    return
  fi

  if ! command -v claude >/dev/null 2>&1; then
    warn "Claude Code no instal·lat. Instal·la'l amb:"
    warn "  npm install -g @anthropic-ai/claude-code"
    warn "Saltant llançament d'agents..."
    return
  fi

  cd "$PROJECT_DIR"
  bash parallel_agents.sh
  cd - > /dev/null
  log "Agents llançats. Connecta't amb: tmux attach -t travelai-agents"
}

stop_all() {
  title "Parant tots els contenidors..."
  cd "$PROJECT_DIR"
  docker-compose down
  cd - > /dev/null
  log "Tots els contenidors aturats"
}

show_logs() {
  cd "$PROJECT_DIR"
  docker-compose logs -f backend frontend
}

show_status() {
  title "Estat dels contenidors:"
  cd "$PROJECT_DIR"
  docker-compose ps
  echo ""
  info "Health backend:"
  curl -sf http://localhost:8080/actuator/health 2>/dev/null \
    && echo "" \
    || echo -e "${RED}No accessible${NC}"
  cd - > /dev/null
}

show_urls() {
  echo ""
  echo -e "${GREEN}============================================${NC}"
  echo -e "${GREEN}  TravelAI en marxa!                       ${NC}"
  echo -e "${GREEN}============================================${NC}"
  echo ""
  echo -e "  ${BOLD}App:${NC}        ${CYAN}http://localhost${NC}"
  echo -e "  ${BOLD}API:${NC}        ${CYAN}http://localhost/api/v1${NC}"
  echo -e "  ${BOLD}Health:${NC}     ${CYAN}http://localhost:8080/actuator/health${NC}"
  echo -e "  ${BOLD}MinIO:${NC}      ${CYAN}http://localhost:9001${NC}  (minioadmin / minioadmin123)"
  echo -e "  ${BOLD}Ollama:${NC}     ${CYAN}http://localhost:11434${NC}"
  echo ""
  echo -e "  ${BOLD}Credencials demo:${NC}"
  echo -e "    admin@travelai.local / Admin1234!"
  echo -e "    demo@travelai.local  / Demo1234!"
  echo ""
  echo -e "  ${BOLD}Pàgines legals:${NC}"
  echo -e "    ${CYAN}http://localhost/privacy${NC}"
  echo -e "    ${CYAN}http://localhost/terms${NC}"
  echo -e "    ${CYAN}http://localhost/cookies${NC}"
  echo ""
  echo -e "  ${BOLD}Agents tmux:${NC}"
  echo -e "    ${CYAN}tmux attach -t travelai-agents${NC}"
  echo ""
  echo -e "  ${BOLD}Logs en temps real:${NC}"
  echo -e "    ${CYAN}./launch.sh --logs${NC}"
  echo ""
  echo -e "  ${YELLOW}IMPORTANT: Els documents legals (/privacy, /terms, /cookies)${NC}"
  echo -e "  ${YELLOW}contenen plantilles. Cal substituir-les per text real${NC}"
  echo -e "  ${YELLOW}d'un advocat especialitzat en RGPD/LOPD.${NC}"
  echo ""
}

# =============================================================
# MODES D'EXECUCIÓ
# =============================================================

case "$MODE" in

  --setup | "")
    # Primera vegada: setup complet
    check_prerequisites
    check_project_exists
    setup_env
    install_frontend
    start_infra
    pull_ollama_model
    start_app
    launch_agents
    show_urls
    ;;

  --start)
    # Ja instal·lat, només arrancar
    check_project_exists
    cd "$PROJECT_DIR"
    info "Arrancant tots els contenidors..."
    docker-compose up -d
    cd - > /dev/null
    pull_ollama_model
    show_urls
    ;;

  --stop)
    check_project_exists
    stop_all
    ;;

  --restart)
    check_project_exists
    stop_all
    sleep 2
    cd "$PROJECT_DIR"
    docker-compose up -d
    cd - > /dev/null
    show_urls
    ;;

  --logs)
    check_project_exists
    show_logs
    ;;

  --status)
    check_project_exists
    show_status
    ;;

  --agents)
    check_project_exists
    cd "$PROJECT_DIR"
    launch_agents
    cd - > /dev/null
    ;;

  --help | -h)
    echo "Ús: ./launch.sh [opció]"
    echo ""
    echo "  (sense opció)   Setup complet + arrancar tot (primera vegada)"
    echo "  --start         Arrancar contenidors (ja instal·lat)"
    echo "  --stop          Parar tots els contenidors"
    echo "  --restart       Parar i tornar a arrancar"
    echo "  --logs          Veure logs en temps real"
    echo "  --status        Estat dels contenidors"
    echo "  --agents        Llançar agents Claude Code (tmux)"
    echo "  --help          Aquesta ajuda"
    ;;

  *)
    error "Opció desconeguda: $MODE\nExecuta ./launch.sh --help per veure les opcions."
    ;;

esac
