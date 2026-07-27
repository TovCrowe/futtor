package com.tov.futtor.torneo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tov.futtor.torneo.dto.CreatePlayerRequest;
import com.tov.futtor.torneo.dto.CreateTeamRequest;
import com.tov.futtor.torneo.dto.CreateTournamentRequest;
import com.tov.futtor.torneo.dto.MatchDTO;
import com.tov.futtor.torneo.dto.PlayerDto;
import com.tov.futtor.torneo.dto.ScorerDto;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.dto.TournamentDto;
import com.tov.futtor.torneo.dto.UpdateScorersRequest;
import com.tov.futtor.torneo.mapper.TournamentMapper;
import com.tov.futtor.torneo.dto.UpdateMatchRequest;
import com.tov.futtor.torneo.dto.UpdatePlayerRequest;
import com.tov.futtor.torneo.dto.UpdateTeamRequest;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Player;
import com.tov.futtor.torneo.entity.Scorer;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;
import com.tov.futtor.torneo.exception.badrequest.MatchInvalidException;
import com.tov.futtor.torneo.exception.badrequest.PlayerInvalidException;
import com.tov.futtor.torneo.exception.badrequest.ScorerInvalidException;
import com.tov.futtor.torneo.exception.badrequest.TeamInvalidException;
import com.tov.futtor.torneo.exception.conflict.PlayerHasScorerRecordsException;
import com.tov.futtor.torneo.exception.notfound.PlayerNotFoundException;
import com.tov.futtor.torneo.exception.notfound.TeamNotFoundException;
import com.tov.futtor.torneo.exception.notfound.TournamentNotFoundException;
import com.tov.futtor.torneo.repository.MatchRepository;
import com.tov.futtor.torneo.repository.PlayerRepository;
import com.tov.futtor.torneo.repository.ScorerRepository;
import com.tov.futtor.torneo.repository.TeamRepository;
import com.tov.futtor.torneo.repository.TournamentRepository;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final ScorerRepository scorerRepository;
    
    @Transactional
    public Tournament createTournament(CreateTournamentRequest request) {
        Tournament tournament = new Tournament(request.getName());
        return tournamentRepository.save(tournament);
    }

    /**
     * Calculates the standings for a given tournament.
     * It retrieves all teams and matches for the tournament, accumulates the results for each team
     * @param tournamentId
     * @return
     */

    public List<StandingsRowDto> calculateStandings(Long tournamentId) {
        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        Map<Long, Accumulator> accumulators = new HashMap<>();
        for (Team team : teams) {
            accumulators.put(team.getId(), new Accumulator(team.getId(), team.getName()));
        }

        for (Match match : matches) {
            Accumulator homeTeam = accumulators.get(match.getHomeTeam().getId());
            Accumulator awayTeam = accumulators.get(match.getAwayTeam().getId());

            if (homeTeam == null || awayTeam == null) {
                throw new IllegalStateException("Match contains a team that is not part of the tournament");
            }

            if (!match.getIsPlayed()) {
                continue;
            }

            homeTeam.addMatchResult(match.getHomeGoals(), match.getAwayGoals());
            awayTeam.addMatchResult(match.getAwayGoals(), match.getHomeGoals());
        }

        return accumulators.values().stream()
                .map(a -> new StandingsRowDto(a.getTeamId(), a.getTeamName(), a.getPoints(),
                        a.getGoalsFor(), a.getGoalsAgainst(), a.getGoalDifference(),
                        a.getMatchesPlayed(), a.getWins(), a.getDraws(), a.getLosses()))
                .sorted(Comparator
                        .comparingInt((StandingsRowDto row) -> row.getPoints())
                        .thenComparingInt((StandingsRowDto row) -> row.getGoalDifference())
                        .thenComparingInt((StandingsRowDto row) -> row.getGoalsFor())
                        .reversed()
                        .thenComparing((StandingsRowDto row) -> row.getTeamName()))
                .toList();
    }

    @Transactional
    public void createTeam(Long tournamentId, CreateTeamRequest teamRequest) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
                
        if (teamRepository.existsByTournamentIdAndNameIgnoreCase(tournamentId, teamRequest.getName())) {
            throw new TeamInvalidException("A team with that name already exists in this tournament");
        }
                
        Team team = new Team(teamRequest.getName(), tournament);
        teamRepository.save(team);
    }

    @Transactional
    public void updateTeam(Long tournamentId, Long teamId, UpdateTeamRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        if (!team.getTournament().getId().equals(tournament.getId())) {
            throw new TeamInvalidException("Team does not belong to the specified tournament");
        }

        if (teamRepository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(tournamentId, request.getName(), teamId)) {
            throw new TeamInvalidException("A team with that name already exists in this tournament");
        }

        team.setName(request.getName());
        teamRepository.save(team);
    }

    @Transactional
    public void deleteTeam(Long tournamentId, Long teamId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        if (!team.getTournament().getId().equals(tournament.getId())) {
            throw new TeamInvalidException("Team does not belong to the specified tournament");
        }

        if (tournament.isGenerated()) {
            throw new TeamInvalidException("Cannot delete a team after the fixture has been generated");
        }

        teamRepository.delete(team);
    }

    @Transactional
    public void createMatchGamesAutomatically(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (tournament.isGenerated()) {
            throw new MatchInvalidException("Matches have already been generated for this tournament");
        }

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        if (teams.size() < 2) {
            throw new MatchInvalidException("Not enough teams to create matches");
        }

        List<Match> existingMatches = matchRepository.findByTournamentId(tournamentId);
        for (int i = 0; i < teams.size(); i++) {
            for (int j = 0; j < teams.size(); j++) {
                if (j == i) {
                    continue;
                }
                Match match = new Match(tournament, teams.get(i), teams.get(j), null, null);
                existingMatches.add(match);
            }
        }

        tournament.setGenerated(true);
        tournamentRepository.save(tournament);
        matchRepository.saveAll(existingMatches);
    }

    @Transactional
    public void updateMatch(Long tournamentId, Long matchId, UpdateMatchRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchInvalidException("Match not found"));

        if (!match.getTournament().getId().equals(tournament.getId())) {
            throw new MatchInvalidException("Match does not belong to the specified tournament");
        }

        match.setHomeGoals(request.getHomeTeamGoals());
        match.setAwayGoals(request.getAwayTeamGoals());
        matchRepository.save(match);
    }

    public List<MatchDTO> getMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        return matches.stream().map(TournamentMapper::toMatchDto).toList();
    }

    /**
     * Devuelve la plantilla de ambos equipos del partido con los goles que cada
     * jugador lleva anotados en él. Los jugadores sin goles vienen en cero, para
     * que la pantalla del partido pueda listarlos a todos.
     */
    @Transactional(readOnly = true)
    public List<ScorerDto> getMatchScorers(Long tournamentId, Long matchId) {
        Match match = findMatchInTournament(tournamentId, matchId);

        Map<Long, Integer> goalsByPlayer = scorerRepository.findByMatchId(matchId).stream()
                .collect(Collectors.toMap(scorer -> scorer.getPlayer().getId(), Scorer::getGoals));

        return squadOf(match).stream()
                .map(player -> new ScorerDto(
                        player.getId(),
                        player.getName(),
                        player.getTeam().getId(),
                        goalsByPlayer.getOrDefault(player.getId(), 0)))
                .toList();
    }

    /**
     * Guarda el goleo del partido y deriva el marcador de ahí: los goles de los
     * jugadores son la única fuente de verdad y el marcador es su suma por equipo.
     * Se recalcula en la misma transacción para que la tabla general (que lee
     * homeGoals/awayGoals) nunca quede desincronizada.
     */
    @Transactional
    public void updateMatchScorers(Long tournamentId, Long matchId, UpdateScorersRequest request) {
        Match match = findMatchInTournament(tournamentId, matchId);

        Map<Long, Player> squad = squadOf(match).stream()
                .collect(Collectors.toMap((Player player) -> player.getId(), player -> player));

        int homeGoals = 0;
        int awayGoals = 0;
        List<Scorer> scorers = new ArrayList<>();
        Set<Long> seenPlayers = new HashSet<>();

        for (UpdateScorersRequest.ScorerEntry entry : request.getScorers()) {
            Player player = squad.get(entry.getPlayerId());
            if (player == null) {
                throw new ScorerInvalidException(
                        "Player " + entry.getPlayerId() + " does not play for either team in this match");
            }
            if (!seenPlayers.add(entry.getPlayerId())) {
                throw new ScorerInvalidException("Player " + entry.getPlayerId() + " is repeated in the request");
            }

            if (player.getTeam().getId().equals(match.getHomeTeam().getId())) {
                homeGoals += entry.getGoals();
            } else {
                awayGoals += entry.getGoals();
            }

            if (entry.getGoals() > 0) {
                scorers.add(new Scorer(player, match, entry.getGoals()));
            }
        }

        // El request trae el goleo completo del partido, así que reemplazamos el anterior.
        scorerRepository.deleteByMatchId(matchId);
        scorerRepository.saveAll(scorers);

        match.setHomeGoals(homeGoals);
        match.setAwayGoals(awayGoals);
        matchRepository.save(match);
    }

    private Match findMatchInTournament(Long tournamentId, Long matchId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchInvalidException("Match not found"));

        if (!match.getTournament().getId().equals(tournament.getId())) {
            throw new MatchInvalidException("Match does not belong to the specified tournament");
        }
        return match;
    }

    /** Jugadores de los dos equipos que disputan el partido. */
    private List<Player> squadOf(Match match) {
        List<Player> squad = new ArrayList<>(playerRepository.findByTeamId(match.getHomeTeam().getId()));
        squad.addAll(playerRepository.findByTeamId(match.getAwayTeam().getId()));
        return squad;
    }

    public List<TournamentDto> getTournaments() {
        return tournamentRepository.findAll().stream()
                .map(TournamentMapper::toTournamentDto)
                .toList();
    }

    // add player to team
    @Transactional
    public void addPlayerToTeam(Long tournamentId, Long teamId, CreatePlayerRequest request) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
                
        Player player = new Player(request.getName(), team);
        playerRepository.save(player);
    }

    //get all players by tournament
    @Transactional
    public List<PlayerDto> getPlayersByTournament(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        return playerRepository.findByTeamTournamentId(tournamentId).stream()
                .map(TournamentMapper::toPlayerDto)
                .toList();
    }

    //get player by id
    @Transactional
    public PlayerDto getPlayerById(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        return TournamentMapper.toPlayerDto(player);
    }

    //get players by team
    @Transactional
    public List<PlayerDto> getPlayersByTeam(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        return playerRepository.findByTeamId(teamId).stream()
                .map(TournamentMapper::toPlayerDto)
                .toList();
    }

    //edit playerasd
    @Transactional
    public void editPlayer(Long tournamentId, Long teamId, Long playerId, UpdatePlayerRequest request) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        if (!player.getTeam().getId().equals(team.getId())) {
            throw new PlayerInvalidException("Player does not belong to the specified team");
        }
        player.setName(request.getName());
        playerRepository.save(player);
    }

    //delete player
    @Transactional
    public void deletePlayer(Long tournamentId, Long teamId, Long playerId) {   
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        if (!player.getTeam().getId().equals(team.getId())) {
            throw new PlayerInvalidException("Player does not belong to the specified team");
        }
        long goals = scorerRepository.countByPlayerId(playerId);
        if (goals > 0) {
            throw new PlayerHasScorerRecordsException(playerId, goals);
        }
        playerRepository.delete(player);
    }

    /**
     * Tabla de goleo del torneo. Los goles se suman desde los Scorer (fuente de
     * verdad), nunca desde un contador guardado en Player: mismo criterio que la
     * tabla general, que se calcula desde Match.
     */
    @Transactional(readOnly = true)
    public List<ScorerDto> getTopScorersPerTournament(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        Map<Long, Integer> goalsByPlayer = new HashMap<>();
        for (Scorer scorer : scorerRepository.findByMatchTournamentId(tournamentId)) {
            goalsByPlayer.merge(scorer.getPlayer().getId(), scorer.getGoals(), Integer::sum);
        }

        return playerRepository.findByTeamTournamentId(tournamentId).stream()
                .map(player -> new ScorerDto(
                        player.getId(),
                        player.getName(),
                        player.getTeam().getId(),
                        goalsByPlayer.getOrDefault(player.getId(), 0)))
                .sorted(Comparator.comparingInt(ScorerDto::getGoals).reversed()
                        .thenComparing(ScorerDto::getPlayerName))
                .toList();
    }
}