-- Sample seed data for development (in-memory H2, recreated on every startup).
-- One tournament with 4 teams and 4 matches — enough to exercise the standings
-- table and the goal-difference tie-breaker.

insert into tournament (id, name, is_generated) values
  (1, 'Sample Tournament', false);

insert into team (id, name, tournament_id) values
  (1, 'Team A', 1),
  (2, 'Team B', 1),
  (3, 'Team C', 1),
  (4, 'Team D', 1);
