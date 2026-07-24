package com.tov.futtor.torneo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tov.futtor.torneo.dto.CreateTeamRequest;
import com.tov.futtor.torneo.dto.ScorerDto;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.dto.UpdateScorersRequest;
import com.tov.futtor.torneo.dto.TournamentDto;
import com.tov.futtor.torneo.dto.UpdateMatchRequest;
import com.tov.futtor.torneo.dto.UpdateTeamRequest;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Player;
import com.tov.futtor.torneo.entity.Scorer;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;
import com.tov.futtor.torneo.exception.MatchInvalidException;
import com.tov.futtor.torneo.exception.ScorerInvalidException;
import com.tov.futtor.torneo.exception.TeamInvalidException;
import com.tov.futtor.torneo.exception.TeamNotFoundException;
import com.tov.futtor.torneo.exception.TournamentNotFoundException;
import com.tov.futtor.torneo.repository.MatchRepository;
import com.tov.futtor.torneo.repository.PlayerRepository;
import com.tov.futtor.torneo.repository.ScorerRepository;
import com.tov.futtor.torneo.repository.TeamRepository;
import com.tov.futtor.torneo.repository.TournamentRepository;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    TeamRepository teamRepository;

    @Mock
    MatchRepository matchRepository;

    @Mock
    TournamentRepository tournamentRepository;

    @Mock
    PlayerRepository playerRepository;

    @Mock
    ScorerRepository scorerRepository;

    @InjectMocks
    TournamentService service;

    // --- helpers -----------------------------------------------------------

    private Team team(Long id, String name) {
        Team t = new Team();
        t.setId(id);
        t.setName(name);
        return t;
    }

    private Tournament tournament(Long id, String name) {
        Tournament t = new Tournament(name);
        t.setId(id);
        return t;
    }

    private Team teamIn(Long id, String name, Tournament tournament) {
        Team t = team(id, name);
        t.setTournament(tournament);
        return t;
    }

    private void stubTournament(Long tournamentId, List<Team> teams, List<Match> matches) {
        when(teamRepository.findByTournamentId(tournamentId)).thenReturn(teams);
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(matches);
    }

    private StandingsRowDto rowFor(List<StandingsRowDto> table, String teamName) {
        return table.stream()
                .filter(r -> r.getTeamName().equals(teamName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró la fila del equipo " + teamName));
    }

    // --- tests -------------------------------------------------------------

    @Test
    void localWinGivesThreePointsAndOrdersFirst() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        stubTournament(1L, List.of(a, b), List.of(
                new Match(null, a, b, 2, 0)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(table).hasSize(2);

        StandingsRowDto first = table.get(0);
        assertThat(first.getTeamName()).isEqualTo("A");
        assertThat(first.getPoints()).isEqualTo(3);
        assertThat(first.getWins()).isEqualTo(1);
        assertThat(first.getDraws()).isZero();
        assertThat(first.getLosses()).isZero();
        assertThat(first.getGoalsFor()).isEqualTo(2);
        assertThat(first.getGoalsAgainst()).isZero();
        assertThat(first.getGoalDifference()).isEqualTo(2);
        assertThat(first.getMatchesPlayed()).isEqualTo(1);

        StandingsRowDto second = table.get(1);
        assertThat(second.getTeamName()).isEqualTo("B");
        assertThat(second.getPoints()).isZero();
        assertThat(second.getLosses()).isEqualTo(1);
        assertThat(second.getGoalDifference()).isEqualTo(-2);
    }

    @Test
    void awayWinGivesThreePointsToVisitor() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        stubTournament(1L, List.of(a, b), List.of(
                new Match(null, a, b, 0, 1)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(rowFor(table, "B").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "B").getWins()).isEqualTo(1);
        assertThat(rowFor(table, "A").getPoints()).isZero();
        assertThat(rowFor(table, "A").getLosses()).isEqualTo(1);
        assertThat(table.get(0).getTeamName()).isEqualTo("B");
    }

    @Test
    void drawGivesOnePointToEach() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        stubTournament(1L, List.of(a, b), List.of(
                new Match(null, a, b, 1, 1)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(rowFor(table, "A").getPoints()).isEqualTo(1);
        assertThat(rowFor(table, "A").getDraws()).isEqualTo(1);
        assertThat(rowFor(table, "B").getPoints()).isEqualTo(1);
        assertThat(rowFor(table, "B").getDraws()).isEqualTo(1);
        assertThat(rowFor(table, "A").getGoalDifference()).isZero();
        assertThat(rowFor(table, "B").getGoalDifference()).isZero();
    }

    @Test
    void breaksPointTieByGoalDifference() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        Team c = team(3L, "C");
        // A y B empatan a 3 pts; A tiene mejor DG (+3 vs +1)
        stubTournament(1L, List.of(a, b, c), List.of(
                new Match(null, a, c, 3, 0),
                new Match(null, b, c, 1, 0)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(rowFor(table, "A").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "B").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "A").getGoalDifference()).isEqualTo(3);
        assertThat(rowFor(table, "B").getGoalDifference()).isEqualTo(1);
        // A debe ir antes que B por mejor diferencia de goles
        assertThat(table.get(0).getTeamName()).isEqualTo("A");
        assertThat(table.get(1).getTeamName()).isEqualTo("B");
    }

    @Test
    void breaksPointAndGoalDifferenceTieByGoalsFor() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        Team c = team(3L, "C");
        // A y B: mismos pts (3) y misma DG (+1), pero A marcó más (2 vs 1)
        stubTournament(1L, List.of(a, b, c), List.of(
                new Match(null, a, c, 2, 1),
                new Match(null, b, c, 1, 0)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(rowFor(table, "A").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "B").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "A").getGoalDifference()).isEqualTo(1);
        assertThat(rowFor(table, "B").getGoalDifference()).isEqualTo(1);
        assertThat(rowFor(table, "A").getGoalsFor()).isEqualTo(2);
        assertThat(rowFor(table, "B").getGoalsFor()).isEqualTo(1);
        // A antes que B por más goles a favor
        assertThat(table.get(0).getTeamName()).isEqualTo("A");
        assertThat(table.get(1).getTeamName()).isEqualTo("B");
    }

    @Test
    void teamWithoutMatchesAppearsWithZeros() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        Team c = team(3L, "C"); // C no juega ningún partido
        stubTournament(1L, List.of(a, b, c), List.of(
                new Match(null, a, b, 1, 0)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(table).hasSize(3);
        StandingsRowDto c1 = rowFor(table, "C");
        assertThat(c1.getMatchesPlayed()).isZero();
        assertThat(c1.getPoints()).isZero();
        assertThat(c1.getWins()).isZero();
        assertThat(c1.getDraws()).isZero();
        assertThat(c1.getLosses()).isZero();
        assertThat(c1.getGoalsFor()).isZero();
        assertThat(c1.getGoalsAgainst()).isZero();
        assertThat(c1.getGoalDifference()).isZero();
    }

    @Test
    void standingsCountOnlyPlayedMatches() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        stubTournament(1L, List.of(a, b), List.of(
                new Match(null, a, b, 2, 0),         // jugado
                new Match(null, a, b, null, null)));  // pendiente (sin marcador)

        List<StandingsRowDto> table = service.calculateStandings(1L);

        // Solo el partido jugado cuenta; el pendiente se ignora (y no debe romper con NPE)
        assertThat(rowFor(table, "A").getMatchesPlayed()).isEqualTo(1);
        assertThat(rowFor(table, "A").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "A").getGoalsFor()).isEqualTo(2);
        assertThat(rowFor(table, "B").getMatchesPlayed()).isEqualTo(1);
        assertThat(rowFor(table, "B").getPoints()).isZero();
    }

    // --- updateMatch tests -------------------------------------------------

    @Test
    void updateMatchSetsGoalsWhenValid() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Match match = new Match(tournament, home, away, null, null); // pendiente
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));

        service.updateMatch(1L, 100L, new UpdateMatchRequest(2, 1));

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();
        assertThat(saved.getHomeGoals()).isEqualTo(2);
        assertThat(saved.getAwayGoals()).isEqualTo(1);
        assertThat(saved.getIsPlayed()).isTrue();
    }

    @Test
    void updateMatchThrowsWhenTournamentNotFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMatch(1L, 100L, new UpdateMatchRequest(2, 1)))
                .isInstanceOf(TournamentNotFoundException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void updateMatchThrowsWhenMatchNotFound() {
        Tournament tournament = tournament(1L, "Liga");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMatch(1L, 100L, new UpdateMatchRequest(2, 1)))
                .isInstanceOf(MatchInvalidException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void updateMatchThrowsWhenMatchBelongsToAnotherTournament() {
        Tournament tournament = tournament(1L, "Liga");
        Tournament other = tournament(2L, "Otra Liga");
        Team home = teamIn(10L, "A", other);
        Team away = teamIn(20L, "B", other);
        Match match = new Match(other, home, away, null, null); // match de otro torneo
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.updateMatch(1L, 100L, new UpdateMatchRequest(2, 1)))
                .isInstanceOf(MatchInvalidException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void breaksPointTieByAlphabeticalOrder() {
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        Team c = team(3L, "C");
        // A y B empatan a 3 pts; A tiene mejor DG (+3 vs +1)
        stubTournament(1L, List.of(a, b, c), List.of(
                new Match(null, a, c, 1, 0),
                new Match(null, b, c, 1, 0)));

        List<StandingsRowDto> table = service.calculateStandings(1L);

        assertThat(rowFor(table, "A").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "B").getPoints()).isEqualTo(3);
        assertThat(rowFor(table, "A").getGoalDifference()).isEqualTo(1);
        assertThat(rowFor(table, "B").getGoalDifference()).isEqualTo(1);
        // A debe ir antes que B por orden alfabético
        assertThat(table.get(0).getTeamName()).isEqualTo("A");
        assertThat(table.get(1).getTeamName()).isEqualTo("B");
    }

    // --- createTeam tests --------------------------------------------------

    @Test
    void createTeamSavesWhenNameIsUnique() {
        Tournament tournament = tournament(1L, "Liga");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.existsByTournamentIdAndNameIgnoreCase(1L, "A")).thenReturn(false);

        service.createTeam(1L, new CreateTeamRequest("A"));

        verify(teamRepository).save(any());
    }

    @Test
    void createTeamThrowsWhenNameDuplicated() {
        Tournament tournament = tournament(1L, "Liga");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.existsByTournamentIdAndNameIgnoreCase(1L, "A")).thenReturn(true);

        assertThatThrownBy(() -> service.createTeam(1L, new CreateTeamRequest("A")))
                .isInstanceOf(TeamInvalidException.class);

        verify(teamRepository, never()).save(any());
    }

    @Test
    void createTeamThrowsWhenTournamentNotFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTeam(1L, new CreateTeamRequest("A")))
                .isInstanceOf(TournamentNotFoundException.class);

        verify(teamRepository, never()).save(any());
    }

    // --- updateTeam tests --------------------------------------------------

    @Test
    void updateTeamRenamesWhenValid() {
        Tournament tournament = tournament(1L, "Liga");
        Team team = teamIn(10L, "Old", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(1L, "New", 10L)).thenReturn(false);

        service.updateTeam(1L, 10L, new UpdateTeamRequest("New"));

        assertThat(team.getName()).isEqualTo("New");
        verify(teamRepository).save(team);
    }

    @Test
    void updateTeamThrowsWhenNameDuplicated() {
        Tournament tournament = tournament(1L, "Liga");
        Team team = teamIn(10L, "Old", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByTournamentIdAndNameIgnoreCaseAndIdNot(1L, "Taken", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateTeam(1L, 10L, new UpdateTeamRequest("Taken")))
                .isInstanceOf(TeamInvalidException.class);

        verify(teamRepository, never()).save(any());
    }

    @Test
    void updateTeamThrowsWhenTeamBelongsToAnotherTournament() {
        Tournament tournament = tournament(1L, "Liga");
        Tournament other = tournament(2L, "Otra Liga");
        Team team = teamIn(10L, "A", other);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.updateTeam(1L, 10L, new UpdateTeamRequest("B")))
                .isInstanceOf(TeamInvalidException.class);

        verify(teamRepository, never()).save(any());
    }

    @Test
    void updateTeamThrowsWhenTeamNotFound() {
        Tournament tournament = tournament(1L, "Liga");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTeam(1L, 10L, new UpdateTeamRequest("B")))
                .isInstanceOf(TeamNotFoundException.class);

        verify(teamRepository, never()).save(any());
    }

    // --- deleteTeam tests --------------------------------------------------

    @Test
    void deleteTeamRemovesWhenFixtureNotGenerated() {
        Tournament tournament = tournament(1L, "Liga"); // isGenerated == false
        Team team = teamIn(10L, "A", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        service.deleteTeam(1L, 10L);

        verify(teamRepository).delete(team);
    }

    @Test
    void deleteTeamThrowsWhenFixtureAlreadyGenerated() {
        Tournament tournament = tournament(1L, "Liga");
        tournament.setGenerated(true);
        Team team = teamIn(10L, "A", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.deleteTeam(1L, 10L))
                .isInstanceOf(TeamInvalidException.class);

        verify(teamRepository, never()).delete(any());
    }

    @Test
    void deleteTeamThrowsWhenTeamBelongsToAnotherTournament() {
        Tournament tournament = tournament(1L, "Liga");
        Tournament other = tournament(2L, "Otra Liga");
        Team team = teamIn(10L, "A", other);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.deleteTeam(1L, 10L))
                .isInstanceOf(TeamInvalidException.class);

        verify(teamRepository, never()).delete(any());
    }

    // --- generación de fixture (ida y vuelta) ------------------------------

    @Test
    void generatesHomeAndAwayForEachPair() {
        Tournament tournament = tournament(1L, "Liga");
        Team a = team(1L, "A");
        Team b = team(2L, "B");
        Team c = team(3L, "C");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(a, b, c));
        when(matchRepository.findByTournamentId(1L)).thenReturn(new ArrayList<>());

        service.createMatchGamesAutomatically(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Match>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        List<Match> saved = captor.getValue();

        // N*(N-1) = 3*2 = 6 partidos (ida y vuelta)
        assertThat(saved).hasSize(6);
        // todos nacen pendientes (sin marcador)
        assertThat(saved).allMatch(m -> !m.getIsPlayed());
        // ninguno contra sí mismo
        assertThat(saved).noneMatch(m -> m.getHomeTeam().getId().equals(m.getAwayTeam().getId()));
        // cada par ordenado exactamente una vez (local/visitante invertido)
        Set<String> pairs = saved.stream()
                .map(m -> m.getHomeTeam().getId() + "-" + m.getAwayTeam().getId())
                .collect(Collectors.toSet());
        assertThat(pairs).containsExactlyInAnyOrder(
                "1-2", "2-1", "1-3", "3-1", "2-3", "3-2");
        // el flag queda marcado como generado
        assertThat(tournament.isGenerated()).isTrue();
    }

    @Test
    void generateThrowsWhenLessThanTwoTeams() {
        Tournament tournament = tournament(1L, "Liga");
        Team a = team(1L, "A");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findByTournamentId(1L)).thenReturn(List.of(a));

        assertThatThrownBy(() -> service.createMatchGamesAutomatically(1L))
                .isInstanceOf(MatchInvalidException.class);

        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void generateThrowsWhenAlreadyGenerated() {
        Tournament tournament = tournament(1L, "Liga");
        tournament.setGenerated(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> service.createMatchGamesAutomatically(1L))
                .isInstanceOf(MatchInvalidException.class);

        verify(matchRepository, never()).saveAll(any());
    }

    // --- getTournaments tests ----------------------------------------------

    @Test
    void getTournamentsReturnsMappedList() {
        Tournament liga = tournament(1L, "Liga");
        Tournament copa = tournament(2L, "Copa");
        copa.setGenerated(true);
        when(tournamentRepository.findAll()).thenReturn(List.of(liga, copa));

        List<TournamentDto> result = service.getTournaments();

        assertThat(result).hasSize(2);
        TournamentDto first = result.get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getName()).isEqualTo("Liga");
        assertThat(first.isGenerated()).isFalse();
        TournamentDto second = result.get(1);
        assertThat(second.getId()).isEqualTo(2L);
        assertThat(second.getName()).isEqualTo("Copa");
        assertThat(second.isGenerated()).isTrue();
    }

    @Test
    void getTournamentsReturnsEmptyWhenNone() {
        when(tournamentRepository.findAll()).thenReturn(List.of());

        assertThat(service.getTournaments()).isEmpty();
    }

    // --- goleo por partido (marcador derivado) -----------------------------

    private Player player(Long id, String name, Team team) {
        Player p = new Player(name, team);
        p.setId(id);
        return p;
    }

    /** Arma un partido con plantillas de 2 jugadores por equipo y lo deja stubbeado. */
    private Match matchWithSquads(Tournament tournament, Team home, Team away, List<Player> homeSquad,
            List<Player> awaySquad) {
        Match match = new Match(tournament, home, away, null, null);
        match.setId(100L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(playerRepository.findByTeamId(home.getId())).thenReturn(homeSquad);
        when(playerRepository.findByTeamId(away.getId())).thenReturn(awaySquad);
        return match;
    }

    @Test
    void matchScoreIsDerivedFromPlayerGoals() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        Player juan = player(2L, "Juan", home);
        Player luis = player(3L, "Luis", away);
        Match match = matchWithSquads(tournament, home, away, List.of(pedro, juan), List.of(luis));

        service.updateMatchScorers(1L, 100L, new UpdateScorersRequest(List.of(
                new UpdateScorersRequest.ScorerEntry(1L, 2),
                new UpdateScorersRequest.ScorerEntry(2L, 1),
                new UpdateScorersRequest.ScorerEntry(3L, 1))));

        // 2 + 1 del local, 1 del visitante => 3-1
        assertThat(match.getHomeGoals()).isEqualTo(3);
        assertThat(match.getAwayGoals()).isEqualTo(1);
        assertThat(match.getIsPlayed()).isTrue();
        verify(matchRepository).save(match);
    }

    @Test
    void savingScorersReplacesPreviousOnesAndSkipsZeroes() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        Player luis = player(3L, "Luis", away);
        matchWithSquads(tournament, home, away, List.of(pedro), List.of(luis));

        service.updateMatchScorers(1L, 100L, new UpdateScorersRequest(List.of(
                new UpdateScorersRequest.ScorerEntry(1L, 2),
                new UpdateScorersRequest.ScorerEntry(3L, 0))));

        // el goleo anterior se borra: el request trae la foto completa del partido
        verify(scorerRepository).deleteByMatchId(100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Scorer>> captor = ArgumentCaptor.forClass(List.class);
        verify(scorerRepository).saveAll(captor.capture());
        // solo se guarda a quien anotó; los ceros no ensucian la tabla
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getPlayer().getId()).isEqualTo(1L);
        assertThat(captor.getValue().get(0).getGoals()).isEqualTo(2);
    }

    @Test
    void correctingScorersRecalculatesTheScore() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        Player luis = player(3L, "Luis", away);
        Match match = matchWithSquads(tournament, home, away, List.of(pedro), List.of(luis));
        match.setHomeGoals(2); // marcador previo
        match.setAwayGoals(0);

        // corrección: Pedro en realidad metió 1, no 2
        service.updateMatchScorers(1L, 100L, new UpdateScorersRequest(List.of(
                new UpdateScorersRequest.ScorerEntry(1L, 1))));

        assertThat(match.getHomeGoals()).isEqualTo(1);
        assertThat(match.getAwayGoals()).isZero();
    }

    @Test
    void updateScorersThrowsWhenPlayerIsNotInEitherSquad() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        matchWithSquads(tournament, home, away, List.of(pedro), List.of());

        // 99 no juega en ninguno de los dos equipos
        assertThatThrownBy(() -> service.updateMatchScorers(1L, 100L, new UpdateScorersRequest(List.of(
                new UpdateScorersRequest.ScorerEntry(99L, 1)))))
                .isInstanceOf(ScorerInvalidException.class);

        verify(matchRepository, never()).save(any());
        verify(scorerRepository, never()).saveAll(any());
    }

    @Test
    void updateScorersThrowsWhenPlayerIsRepeated() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        matchWithSquads(tournament, home, away, List.of(pedro), List.of());

        assertThatThrownBy(() -> service.updateMatchScorers(1L, 100L, new UpdateScorersRequest(List.of(
                new UpdateScorersRequest.ScorerEntry(1L, 1),
                new UpdateScorersRequest.ScorerEntry(1L, 2)))))
                .isInstanceOf(ScorerInvalidException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void matchScorersListsWholeSquadWithZeroesForNonScorers() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        Player juan = player(2L, "Juan", home);
        Player luis = player(3L, "Luis", away);
        Match match = matchWithSquads(tournament, home, away, List.of(pedro, juan), List.of(luis));
        when(scorerRepository.findByMatchId(100L)).thenReturn(List.of(new Scorer(pedro, match, 2)));

        List<ScorerDto> scorers = service.getMatchScorers(1L, 100L);

        // los tres aparecen, aunque solo Pedro haya anotado
        assertThat(scorers).hasSize(3);
        assertThat(scorers).anySatisfy(s -> {
            assertThat(s.getPlayerName()).isEqualTo("Pedro");
            assertThat(s.getGoals()).isEqualTo(2);
        });
        assertThat(scorers).anySatisfy(s -> {
            assertThat(s.getPlayerName()).isEqualTo("Juan");
            assertThat(s.getGoals()).isZero();
        });
    }

    @Test
    void topScorersSumsGoalsAcrossMatchesAndSortsDesc() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        Player pedro = player(1L, "Pedro", home);
        Player luis = player(3L, "Luis", away);
        Match match1 = new Match(tournament, home, away, null, null);
        Match match3 = new Match(tournament, home, away, null, null);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByTeamTournamentId(1L)).thenReturn(List.of(pedro, luis));
        when(scorerRepository.findByMatchTournamentId(1L)).thenReturn(List.of(
                new Scorer(pedro, match1, 2),
                new Scorer(luis, match1, 1),
                new Scorer(pedro, match3, 1)));

        List<ScorerDto> top = service.getTopScorersPerTournament(1L);

        // Pedro suma 2 + 1 de dos partidos distintos y encabeza
        assertThat(top.get(0).getPlayerName()).isEqualTo("Pedro");
        assertThat(top.get(0).getGoals()).isEqualTo(3);
        assertThat(top.get(1).getPlayerName()).isEqualTo("Luis");
        assertThat(top.get(1).getGoals()).isEqualTo(1);
    }
}
