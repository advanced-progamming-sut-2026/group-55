# A8 leaderboard integration

This change keeps the A7 readable-fonts source as its baseline.

- LocalLeaderboardDataSource projects completed snapshot-based quests from current user metrics, even before Travel Log synchronization. Existing lifetime counts are preserved; a completion is never counted twice. Projection does not grant rewards or reset daily cycles. Daily inference requires a stored baseline for today's cycle. Event-only objectives still require QuestService.recordEvent/recordEvents from gameplay.
- ScoreRecordService(UserManager).recordFinishedRun(user, score) is the local integration point for a valid finished score-mode run. It persists only an improved score and restores the previous value on save failure. It is intentionally not called from ordinary Adventure or placeholder minigames.
- Adventure: retain completeLevel on successful completion; register future chapters/levels in LevelCatalog so their standing resolves correctly.
- Minigames: use the existing MinigameProgressService.recordSuccessfulCompletion API. The board counts distinct completed stages, now explicitly labelled MINIGAME STAGES.
- Phase 3: implement LeaderboardDataSource with authenticated server snapshots and inject it into LeaderboardService. Validate gameplay outcomes and persist authoritative scores on the server; the local score service is not server validation.
- Current-player YOU marker has its own cell. Hovering a player cell displays full username and nickname.

Validation: 6 focused JUnit tests passed for quest projection, daily-cycle preservation, duplicate prevention, score persistence and failed-save rollback. Full graphical application execution was not performed.
