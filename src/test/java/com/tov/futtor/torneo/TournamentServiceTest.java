package com.tov.futtor.torneo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tov.futtor.torneo.dto.CreateMatchRequest;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;
import com.tov.futtor.torneo.exception.MatchInvalidException;
import com.tov.futtor.torneo.exception.TeamNotFoundException;
import com.tov.futtor.torneo.exception.TournamentNotFoundException;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    TeamRepository teamRepository;

    @Mock
    MatchRepository matchRepository;

    @Mock
    TournamentRepository tournamentRepository;

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

    // --- createMatch tests -------------------------------------------------

    @Test
    void createMatchSavesMatchWhenValid() {
        Tournament tournament = tournament(1L, "Liga");
        Team home = teamIn(10L, "A", tournament);
        Team away = teamIn(20L, "B", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(away));

        service.createMatch(1L, new CreateMatchRequest(10L, 20L, 2, 1));

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        Match saved = captor.getValue();
        assertThat(saved.getTournament()).isSameAs(tournament);
        assertThat(saved.getHomeTeam()).isSameAs(home);
        assertThat(saved.getAwayTeam()).isSameAs(away);
        assertThat(saved.getHomeGoals()).isEqualTo(2);
        assertThat(saved.getAwayGoals()).isEqualTo(1);
    }

    @Test
    void createMatchThrowsWhenTournamentNotFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMatch(1L, new CreateMatchRequest(10L, 20L, 2, 1)))
                .isInstanceOf(TournamentNotFoundException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void createMatchThrowsWhenHomeTeamNotFound() {
        Tournament tournament = tournament(1L, "Liga");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMatch(1L, new CreateMatchRequest(10L, 20L, 2, 1)))
                .isInstanceOf(TeamNotFoundException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void createMatchThrowsWhenTeamBelongsToAnotherTournament() {
        Tournament tournament = tournament(1L, "Liga");
        Tournament other = tournament(2L, "Otra Liga");
        Team home = teamIn(10L, "A", other); // pertenece a otro torneo
        Team away = teamIn(20L, "B", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(home));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(away));

        assertThatThrownBy(() -> service.createMatch(1L, new CreateMatchRequest(10L, 20L, 2, 1)))
                .isInstanceOf(MatchInvalidException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void createMatchThrowsWhenTeamPlaysAgainstItself() {
        Tournament tournament = tournament(1L, "Liga");
        Team team = teamIn(10L, "A", tournament);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.createMatch(1L, new CreateMatchRequest(10L, 10L, 2, 1)))
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
}
