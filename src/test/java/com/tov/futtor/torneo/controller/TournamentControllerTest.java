package com.tov.futtor.torneo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tov.futtor.torneo.TournamentService;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.dto.TournamentDto;
import com.tov.futtor.torneo.exception.TeamInvalidException;
import com.tov.futtor.torneo.exception.TournamentNotFoundException;

@WebMvcTest(TournamentController.class)
class TournamentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TournamentService tournamentService;

    // --- GET standings -----------------------------------------------------

    @Test
    void getStandingsReturnsOkWithBody() throws Exception {
        StandingsRowDto row = new StandingsRowDto(1L, "A", 3, 2, 0, 2, 1, 1, 0, 0);
        when(tournamentService.calculateStandings(1L)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/tournaments/1/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamName").value("A"))
                .andExpect(jsonPath("$[0].points").value(3));
    }

    @Test
    void getStandingsWhenTournamentNotFoundReturns404() throws Exception {
        when(tournamentService.calculateStandings(99L))
                .thenThrow(new TournamentNotFoundException(99L));

        mockMvc.perform(get("/api/tournaments/99/standings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturns500() throws Exception {
        when(tournamentService.calculateStandings(1L))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/tournaments/1/standings"))
                .andExpect(status().isInternalServerError());
    }

    // --- POST create team --------------------------------------------------

    @Test
    void createTeamValidReturns200() throws Exception {
        mockMvc.perform(post("/api/tournaments/1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}"))
                .andExpect(status().isOk());

        verify(tournamentService).createTeam(eq(1L), any());
    }

    @Test
    void createTeamWithBlankNameReturns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/tournaments/1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(tournamentService, never()).createTeam(anyLong(), any());
    }

    @Test
    void createTeamWithDuplicateNameReturns400() throws Exception {
        doThrow(new TeamInvalidException("A team with that name already exists in this tournament"))
                .when(tournamentService).createTeam(eq(1L), any());

        mockMvc.perform(post("/api/tournaments/1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET tournaments ---------------------------------------------------

    @Test
    void getTournamentsReturnsOkWithList() throws Exception {
        when(tournamentService.getTournaments()).thenReturn(List.of(
                new TournamentDto(1L, "Liga", false),
                new TournamentDto(2L, "Copa", true)));

        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[1].name").value("Copa"))
                .andExpect(jsonPath("$[1].generated").value(true));
    }
}
