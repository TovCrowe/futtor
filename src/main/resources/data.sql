-- Sample seed data for development (in-memory H2, recreated on every startup).
-- One tournament with 4 teams — enough to exercise the standings table.

insert into tournament (name, is_generated) values
  ('Sample Tournament', false);

insert into team (name, tournament_id) values
  ('Team A', 1),
  ('Team B', 1),
  ('Team C', 1),
  ('Team D', 1);
