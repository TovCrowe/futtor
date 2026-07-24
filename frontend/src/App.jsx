import { useEffect, useState } from 'react'
import { api } from './api'

export default function App() {
  const [tournaments, setTournaments] = useState([])
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState(null)

  const loadTournaments = () =>
    api.listTournaments().then(setTournaments).catch((e) => setError(e.message))

  useEffect(() => {
    loadTournaments()
  }, [])

  return (
    <div className="app">
      <header className="topbar">
        <h1 className="brand" onClick={() => setSelected(null)}>⚽ Futtor</h1>
        <span className="subtitle">Liga de puntos</span>
      </header>

      {error && (
        <div className="error" onClick={() => setError(null)}>
          <span>{error}</span>
          <span className="dismiss">✕</span>
        </div>
      )}

      {selected ? (
        <TournamentView
          tournament={selected}
          onBack={() => {
            setSelected(null)
            loadTournaments()
          }}
          onError={(e) => setError(e.message)}
        />
      ) : (
        <Home
          tournaments={tournaments}
          onOpen={setSelected}
          onCreated={loadTournaments}
          onError={(e) => setError(e.message)}
        />
      )}
    </div>
  )
}

function Home({ tournaments, onOpen, onCreated, onError }) {
  const [name, setName] = useState('')

  const create = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    try {
      await api.createTournament(name.trim())
      setName('')
      onCreated()
    } catch (e) {
      onError(e)
    }
  }

  return (
    <main className="container">
      <section className="card">
        <h2>Crear torneo</h2>
        <form className="row" onSubmit={create}>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nombre del torneo"
          />
          <button type="submit">Crear</button>
        </form>
      </section>

      <section className="card">
        <h2>Torneos</h2>
        {tournaments.length === 0 ? (
          <p className="muted">Todavía no hay torneos. Creá el primero arriba.</p>
        ) : (
          <ul className="list">
            {tournaments.map((t) => (
              <li key={t.id} className="list-item" onClick={() => onOpen(t)}>
                <span>{t.name}</span>
                <span className={`badge ${t.generated ? 'badge-on' : 'badge-off'}`}>
                  {t.generated ? 'Fixture generado' : 'Sin fixture'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}

function TournamentView({ tournament, onBack, onError }) {
  const id = tournament.id
  const [standings, setStandings] = useState([])
  const [matches, setMatches] = useState([])
  const [generated, setGenerated] = useState(tournament.generated)
  const [teamName, setTeamName] = useState('')
  const [openMatch, setOpenMatch] = useState(null)
  const [openTeam, setOpenTeam] = useState(null)

  const loadAll = async () => {
    try {
      const [s, m] = await Promise.all([api.standings(id), api.matches(id)])
      setStandings(s || [])
      setMatches(m || [])
    } catch (e) {
      onError(e)
    }
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const addTeam = async (e) => {
    e.preventDefault()
    if (!teamName.trim()) return
    try {
      await api.createTeam(id, teamName.trim())
      setTeamName('')
      loadAll()
    } catch (e) {
      onError(e)
    }
  }

  const generate = async () => {
    try {
      await api.generateMatches(id)
      setGenerated(true)
      loadAll()
    } catch (e) {
      onError(e)
    }
  }

  if (openMatch) {
    return (
      <MatchView
        tournamentId={id}
        match={openMatch}
        onBack={() => {
          setOpenMatch(null)
          loadAll()
        }}
        onError={onError}
      />
    )
  }

  if (openTeam) {
    return (
      <TeamView
        tournamentId={id}
        team={openTeam}
        canDelete={!generated}
        onDeleted={() => {
          setOpenTeam(null)
          loadAll()
        }}
        onBack={() => {
          setOpenTeam(null)
          loadAll()
        }}
        onError={onError}
      />
    )
  }

  return (
    <main className="container">
      <button className="link-back" onClick={onBack}>
        ← Volver a torneos
      </button>

      <div className="detail-header">
        <h2>{tournament.name}</h2>
        <span className={`badge ${generated ? 'badge-on' : 'badge-off'}`}>
          {generated ? 'Fixture generado' : 'Sin fixture'}
        </span>
      </div>

      <section className="card">
        <h3>Equipos</h3>
        {!generated && (
          <form className="row" onSubmit={addTeam}>
            <input
              value={teamName}
              onChange={(e) => setTeamName(e.target.value)}
              placeholder="Nombre del equipo"
            />
            <button type="submit">Agregar</button>
          </form>
        )}

        {standings.length === 0 ? (
          <p className="muted">Todavía no hay equipos.</p>
        ) : (
          <ul className="list">
            {standings.map((row) => (
              <li
                key={row.teamId}
                className="list-item clickable"
                onClick={() => setOpenTeam({ id: row.teamId, name: row.teamName })}
              >
                <span>{row.teamName}</span>
                <span className="chevron">›</span>
              </li>
            ))}
          </ul>
        )}

        {!generated && (
          <>
            <button className="primary generate" onClick={generate} disabled={standings.length < 2}>
              Generar fixture (ida y vuelta)
            </button>
            {standings.length < 2 && (
              <p className="muted">Necesitás al menos 2 equipos para generar el fixture.</p>
            )}
          </>
        )}
      </section>

      {generated && (
        <section className="card">
          <h3>Partidos</h3>
          {matches.length === 0 ? (
            <p className="muted">No hay partidos.</p>
          ) : (
            <div className="matches">
              {matches.map((m) => (
                <MatchRow key={m.id} match={m} onOpen={() => setOpenMatch(m)} />
              ))}
            </div>
          )}
        </section>
      )}

      <section className="card">
        <h3>Tabla de posiciones</h3>
        <StandingsTable rows={standings} />
      </section>

      {generated && <TopScorers tournamentId={id} matches={matches} onError={onError} />}
    </main>
  )
}

// Goleo del torneo. Se recarga cuando cambian los partidos porque se calcula desde ellos.
function TopScorers({ tournamentId, matches, onError }) {
  const [rows, setRows] = useState([])

  useEffect(() => {
    api
      .topScorers(tournamentId)
      .then((data) => setRows((data || []).filter((r) => r.goals > 0)))
      .catch(onError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId, matches])

  return (
    <section className="card">
      <h3>Goleadores</h3>
      {rows.length === 0 ? (
        <p className="muted">Todavía no hay goles cargados.</p>
      ) : (
        <ul className="list">
          {rows.map((r, i) => (
            <li key={r.playerId} className="list-item">
              <span>
                {i + 1}. {r.playerName}
              </span>
              <span className="badge badge-on">{r.goals}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

// Pantalla de un equipo: su plantel. Sin jugadores no hay a quién asignarle goles.
function TeamView({ tournamentId, team, canDelete, onBack, onDeleted, onError }) {
  const [players, setPlayers] = useState([])
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)

  const load = () =>
    api
      .players(tournamentId, team.id)
      .then((rows) => setPlayers(rows || []))
      .catch(onError)
      .finally(() => setLoading(false))

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [team.id])

  const add = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    try {
      await api.createPlayer(tournamentId, team.id, name.trim())
      setName('')
      load()
    } catch (e) {
      onError(e)
    }
  }

  const remove = async (playerId) => {
    try {
      await api.deletePlayer(tournamentId, team.id, playerId)
      load()
    } catch (e) {
      onError(e)
    }
  }

  const removeTeam = async () => {
    try {
      await api.deleteTeam(tournamentId, team.id)
      onDeleted()
    } catch (e) {
      onError(e)
    }
  }

  return (
    <main className="container">
      <button className="link-back" onClick={onBack}>
        ← Volver al torneo
      </button>

      <div className="detail-header">
        <h2>{team.name}</h2>
        {canDelete && (
          <button className="danger" onClick={removeTeam}>
            Eliminar equipo
          </button>
        )}
      </div>

      <section className="card">
        <h3>Jugadores</h3>
        <form className="row" onSubmit={add}>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nombre del jugador"
          />
          <button type="submit">Agregar</button>
        </form>

        {loading ? (
          <p className="muted">Cargando…</p>
        ) : players.length === 0 ? (
          <p className="muted">Sin jugadores todavía. Agregá el primero arriba.</p>
        ) : (
          <ul className="list">
            {players.map((p) => (
              <li key={p.id} className="list-item">
                <span>{p.name}</span>
                <button className="danger" onClick={() => remove(p.id)}>
                  Quitar
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}

function MatchRow({ match, onOpen }) {
  const played = match.homeTeamGoals != null && match.awayTeamGoals != null

  return (
    <div className={`match clickable ${played ? 'played' : ''}`} onClick={onOpen}>
      <span className="team home">{match.homeTeamName}</span>
      <span className="score-box">{played ? match.homeTeamGoals : '-'}</span>
      <span className="vs">vs</span>
      <span className="score-box">{played ? match.awayTeamGoals : '-'}</span>
      <span className="team away">{match.awayTeamName}</span>
      <span className="chevron">›</span>
    </div>
  )
}

// Pantalla de un partido: plantilla de ambos equipos con sus goles.
// El marcador no se teclea, se deriva de la suma de goles de cada equipo.
function MatchView({ tournamentId, match, onBack, onError }) {
  const [scorers, setScorers] = useState([])
  const [goals, setGoals] = useState({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const rows = await api.matchScorers(tournamentId, match.id)
      setScorers(rows || [])
      setGoals(Object.fromEntries((rows || []).map((r) => [r.playerId, r.goals])))
    } catch (e) {
      onError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [match.id])

  const homeSquad = scorers.filter((s) => s.teamId === match.homeTeamId)
  const awaySquad = scorers.filter((s) => s.teamId !== match.homeTeamId)

  const sumOf = (squad) => squad.reduce((total, s) => total + (Number(goals[s.playerId]) || 0), 0)
  const homeGoals = sumOf(homeSquad)
  const awayGoals = sumOf(awaySquad)

  const setGoalsFor = (playerId, value) =>
    setGoals((prev) => ({ ...prev, [playerId]: value === '' ? '' : Math.max(0, Number(value)) }))

  const save = async () => {
    setSaving(true)
    try {
      await api.updateMatchScorers(
        tournamentId,
        match.id,
        scorers.map((s) => ({ playerId: s.playerId, goals: Number(goals[s.playerId]) || 0 })),
      )
      onBack()
    } catch (e) {
      onError(e)
      setSaving(false)
    }
  }

  return (
    <main className="container">
      <button className="link-back" onClick={onBack}>
        ← Volver al torneo
      </button>

      <section className="card scoreboard">
        <div className="side">
          <span className="team-name">{match.homeTeamName}</span>
          <span className="big-score">{homeGoals}</span>
        </div>
        <span className="dash">-</span>
        <div className="side">
          <span className="big-score">{awayGoals}</span>
          <span className="team-name">{match.awayTeamName}</span>
        </div>
      </section>
      <p className="muted center">El marcador se calcula con los goles de cada jugador.</p>

      {loading ? (
        <p className="muted">Cargando jugadores…</p>
      ) : scorers.length === 0 ? (
        <section className="card">
          <p className="muted">
            Ningún equipo tiene jugadores registrados. Agregalos desde la pantalla de equipos para
            poder cargar goles.
          </p>
        </section>
      ) : (
        <>
          <SquadGoals
            title={match.homeTeamName}
            squad={homeSquad}
            goals={goals}
            onChange={setGoalsFor}
          />
          <SquadGoals
            title={match.awayTeamName}
            squad={awaySquad}
            goals={goals}
            onChange={setGoalsFor}
          />
          <button className="primary generate" onClick={save} disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar goles'}
          </button>
        </>
      )}
    </main>
  )
}

function SquadGoals({ title, squad, goals, onChange }) {
  return (
    <section className="card">
      <h3>{title}</h3>
      {squad.length === 0 ? (
        <p className="muted">Sin jugadores registrados en este equipo.</p>
      ) : (
        <ul className="list">
          {squad.map((player) => (
            <li key={player.playerId} className="list-item">
              <span>{player.playerName}</span>
              <input
                className="score"
                type="number"
                min="0"
                value={goals[player.playerId] ?? 0}
                onChange={(e) => onChange(player.playerId, e.target.value)}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function StandingsTable({ rows }) {
  if (rows.length === 0) {
    return <p className="muted">Sin equipos todavía.</p>
  }
  return (
    <div className="table-wrap">
      <table className="standings">
        <thead>
          <tr>
            <th>#</th>
            <th className="left">Equipo</th>
            <th>PJ</th>
            <th>PG</th>
            <th>PE</th>
            <th>PP</th>
            <th>GF</th>
            <th>GC</th>
            <th>DG</th>
            <th>Pts</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.teamId}>
              <td>{i + 1}</td>
              <td className="left">{r.teamName}</td>
              <td>{r.matchesPlayed}</td>
              <td>{r.wins}</td>
              <td>{r.draws}</td>
              <td>{r.losses}</td>
              <td>{r.goalsFor}</td>
              <td>{r.goalsAgainst}</td>
              <td>{r.goalDifference}</td>
              <td className="pts">{r.points}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
