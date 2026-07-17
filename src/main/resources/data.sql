-- Sample seed data for development (in-memory H2, recreated on every startup).
-- One tournament with 4 teams and 4 matches — enough to exercise the standings
-- table and the goal-difference tie-breaker.

insert into tournament (id, name) values
  (1, 'Sample Tournament');

insert into team (id, name, tournament_id) values
  (1, 'Team A', 1),
  (2, 'Team B', 1),
  (3, 'Team C', 1),
  (4, 'Team D', 1);

-- home_goals / away_goals
insert into match (id, tournament_id, home_team_id, away_team_id, home_goals, away_goals) values
  (1, 1, 1, 2, 3, 0),  -- Team A 3 - 0 Team B
  (2, 1, 3, 4, 1, 1),  -- Team C 1 - 1 Team D
  (3, 1, 1, 3, 2, 1),  -- Team A 2 - 1 Team C
  (4, 1, 2, 4, 2, 2);  -- Team B 2 - 2 Team D