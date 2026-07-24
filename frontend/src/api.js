// Cliente HTTP delgado sobre la API de Futtor.
// Todas las rutas son relativas (/api/...) y el proxy de Vite las manda al backend.
const BASE = '/api/tournaments'

async function handle(res) {
  if (!res.ok) {
    let message = `Error ${res.status}`
    try {
      const body = await res.json()
      if (body && body.error) message = body.error
    } catch {
      // respuesta sin cuerpo JSON; dejamos el mensaje por defecto
    }
    throw new Error(message)
  }
  // Varios endpoints (crear equipo, generar fixture, actualizar partido) responden vacío.
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

const jsonPost = (url, body) =>
  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(handle)

const jsonPut = (url, body) =>
  fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(handle)

export const api = {
  listTournaments: () => fetch(BASE).then(handle),

  createTournament: (name) => jsonPost(`${BASE}/create-tournament`, { name }),

  standings: (tournamentId) => fetch(`${BASE}/${tournamentId}/standings`).then(handle),

  matches: (tournamentId) => fetch(`${BASE}/${tournamentId}/matches`).then(handle),

  createTeam: (tournamentId, name) => jsonPost(`${BASE}/${tournamentId}/teams`, { name }),

  deleteTeam: (tournamentId, teamId) =>
    fetch(`${BASE}/${tournamentId}/teams/${teamId}`, { method: 'DELETE' }).then(handle),

  generateMatches: (tournamentId) =>
    jsonPost(`${BASE}/${tournamentId}/generate-matches`, {}),

  updateMatch: (tournamentId, matchId, homeTeamGoals, awayTeamGoals) =>
    jsonPost(`${BASE}/${tournamentId}/${matchId}/update-match`, { homeTeamGoals, awayTeamGoals }),

  // Plantilla de ambos equipos con los goles que cada uno lleva en ese partido.
  matchScorers: (tournamentId, matchId) =>
    fetch(`${BASE}/${tournamentId}/matches/${matchId}/scorers`).then(handle),

  // El marcador del partido se deriva de estos goles, por eso se manda la lista completa.
  updateMatchScorers: (tournamentId, matchId, scorers) =>
    jsonPut(`${BASE}/${tournamentId}/matches/${matchId}/scorers`, { scorers }),

  players: (tournamentId, teamId) =>
    fetch(`${BASE}/${tournamentId}/teams/${teamId}/players`).then(handle),

  createPlayer: (tournamentId, teamId, name) =>
    jsonPost(`${BASE}/${tournamentId}/teams/${teamId}/players`, { name }),

  deletePlayer: (tournamentId, teamId, playerId) =>
    fetch(`${BASE}/${tournamentId}/teams/${teamId}/players/${playerId}`, { method: 'DELETE' }).then(
      handle,
    ),

  topScorers: (tournamentId) => fetch(`${BASE}/${tournamentId}/top-scorers`).then(handle),
}
