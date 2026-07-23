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

  const removeTeam = async (teamId) => {
    try {
      await api.deleteTeam(id, teamId)
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

      {!generated && (
        <section className="card">
          <h3>Equipos</h3>
          <form className="row" onSubmit={addTeam}>
            <input
              value={teamName}
              onChange={(e) => setTeamName(e.target.value)}
              placeholder="Nombre del equipo"
            />
            <button type="submit">Agregar</button>
          </form>

          {standings.length > 0 && (
            <ul className="list">
              {standings.map((row) => (
                <li key={row.teamId} className="list-item">
                  <span>{row.teamName}</span>
                  <button className="danger" onClick={() => removeTeam(row.teamId)}>
                    Quitar
                  </button>
                </li>
              ))}
            </ul>
          )}

          <button
            className="primary generate"
            onClick={generate}
            disabled={standings.length < 2}
          >
            Generar fixture (ida y vuelta)
          </button>
          {standings.length < 2 && (
            <p className="muted">Necesitás al menos 2 equipos para generar el fixture.</p>
          )}
        </section>
      )}

      {generated && (
        <section className="card">
          <h3>Partidos</h3>
          {matches.length === 0 ? (
            <p className="muted">No hay partidos.</p>
          ) : (
            <div className="matches">
              {matches.map((m) => (
                <MatchRow
                  key={m.id}
                  tournamentId={id}
                  match={m}
                  onSaved={loadAll}
                  onError={onError}
                />
              ))}
            </div>
          )}
        </section>
      )}

      <section className="card">
        <h3>Tabla de posiciones</h3>
        <StandingsTable rows={standings} />
      </section>
    </main>
  )
}

function MatchRow({ tournamentId, match, onSaved, onError }) {
  const [home, setHome] = useState(match.homeTeamGoals ?? '')
  const [away, setAway] = useState(match.awayTeamGoals ?? '')

  const save = async () => {
    if (home === '' || away === '') {
      onError(new Error('Cargá ambos marcadores.'))
      return
    }
    try {
      await api.updateMatch(tournamentId, match.id, Number(home), Number(away))
      onSaved()
    } catch (e) {
      onError(e)
    }
  }

  const played = match.homeTeamGoals != null && match.awayTeamGoals != null

  return (
    <div className={`match ${played ? 'played' : ''}`}>
      <span className="team home">{match.homeTeamName}</span>
      <input
        className="score"
        type="number"
        min="0"
        value={home}
        onChange={(e) => setHome(e.target.value)}
      />
      <span className="vs">-</span>
      <input
        className="score"
        type="number"
        min="0"
        value={away}
        onChange={(e) => setAway(e.target.value)}
      />
      <span className="team away">{match.awayTeamName}</span>
      <button onClick={save}>Guardar</button>
    </div>
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
