# Simple Game Development Engine

Simple Game Development Engine is a terminal application for creating and playing custom, turn-based Action and Horror scenarios. Choose a genre, dimension, project name, and enemy settings, then generate a randomized scenario and play it through to victory or defeat. You can experiment with different configurations and replay them without writing code.

Both 2D and 3D modes use the terminal. The dimension changes your available gameplay choices; the project currently provides text-based simulations without graphical rendering.

## How it works

1. **Create a project:** select Action or Horror, choose 2D or 3D, and enter a name.
2. **Customize enemies:** enter names, behaviors, quantities, and the settings required by the genre.
3. **Review and generate:** view your configuration or generate a randomized preview. Your configured statistics and quantities are preserved.
4. **Start a simulation:** play the previewed layout, or start directly to generate a new scenario. Choose from the actions displayed each turn.
5. **Review results:** see the outcome, actual gameplay statistics, and event log. Generate a fresh replay, return to the project menu, or exit.

Each generation displays its seed. The same configuration and seed reproduce the initial placement. Different seeds can still produce the same layout by chance. Each session starts with fresh runtime state: damage, room progress, and statistics do not carry into a replay.

## Current features

### Action

Configure player maximum HP from **1–200** and **1–5 enemy types**, with exactly **five enemy instances** in total. Each type has a unique name, maximum HP of **1–200**, damage of **1–20**, an aggressive or cautious behavior, and a quantity.

The engine distributes the enemies between two nonempty buildings. Enter either building, then defeat all its occupants before leaving.

- **Normal attack:** 25 base damage, available in both dimensions.
- **Moving attack:** 15 base damage, available only in 3D. Each incoming attack gets an independent 40% dodge chance.
- **Aggressive behavior:** attacks on every surviving enemy turn.
- **Cautious behavior:** alternates between defending and attacking, starting in defense. Defense reduces incoming damage to 70%, rounded up. Each enemy tracks its own phase.
- **Progression:** dead enemies stop retaliating and disappear from attack choices. Clearing one building unlocks the elevator.
- **Victory:** use the elevator after one building for an early escape, or clear both buildings before escaping for a full clear.
- **Defeat:** reaching zero player HP ends the session. Damage statistics count actual HP lost.

Results show enemies defeated, buildings cleared, damage received, remaining HP, and combat turns.

### Horror

Configure **3–5 total enemies**, including one global Pursuer. Encounter enemies must include at least one Watcher and one Distractor. Each encounter instance becomes one room; the Pursuer follows you across the scenario and does not occupy a room.

- **Watcher:** starts alert and requires three successful advances. Hide safely before each advance. In 2D, use Hide / Wait. In 3D, choose between a safe closet and an exposed table. Advancing while watched or choosing the exposed hiding spot causes defeat.
- **Distractor:** blocks the passage until you find and use a metal can. Each of the first two searches has a 50% discovery chance; the third guarantees discovery. Passing without distracting the enemy causes defeat.
- **Pursuer:** has a custom name and countdown. Each time-consuming action, including a fatal encounter choice, spends one countdown unit.
- **Free actions:** inspection and status do not consume time.
- **Escape:** clear every room, then spend one final turn to escape. The final escape may use the last countdown unit; reaching zero inside a room causes defeat.

The minimum countdown is calculated from the configured quantities:

```text
minimum = 6 × Watcher rooms + 3 × Distractor rooms + 1 final escape
```

The console recommends enough time for three searches per Distractor plus a 2–4 turn margin. One Watcher and one Distractor therefore require at least 10 turns, with a recommended countdown of 14–16.

Results show rooms completed, enemies avoided, turns used, remaining countdown, and the cause of defeat or successful escape.

### Validation and replay

Numeric and menu input is checked before an action is performed. Invalid choices do not consume gameplay turns. Behavior menus accept numbered choices or the displayed behavior names; `BOTH` is not supported.

Project and enemy names must be nonblank. Enemy names must be unique within the project, including the Pursuer; names are case-sensitive. Invalid whole-project configurations restart configuration. Malformed numbers and too-short countdowns are retried locally.

Configurations are immutable snapshots. The console validates countdown feasibility before accepting a playable Horror project, and the facade checks it again at session startup. The Builder also supports generation-only configurations with any positive countdown; these cannot start a simulation if their countdown is below the playable minimum.

## Build and run

Use a **JDK 17 or newer**, with `javac` and `java` available on PATH. Only the Java standard library is required; no Maven, Gradle, or external dependencies are needed.

From the project root in PowerShell:

```powershell
$sources = @(Get-ChildItem src,test -Recurse -Filter *.java | ForEach-Object FullName)
javac --release 17 -encoding UTF-8 -Xlint:all -d out @sources
java -cp out Main
```

Run the three test runners:

```powershell
java -cp out com.team.game.ProjectTests
java -cp out GameplayTests
java -cp out AcceptanceTests
```

The latest recorded verification passed **6,863 checks**: 6,688 configuration/generation checks, 129 gameplay checks, and 46 acceptance checks. Compilation targeted Java 17; execution used JDK 27. A native Java 17 runtime was not tested.

See [Test_Report.md](Test_Report.md) for the acceptance checklist and verification details.

## System design

The system separates configuration, scenario generation, dimension rules, gameplay, event handling, and console interaction. Immutable project settings describe what to create; mutable scenario and engine state describe what happens during play. The console collects input and displays results, while the facade coordinates the services needed to run a session.

### How patterns work and what problems they solve

**Builder** assembles a project step by step through `GameProjectBuilder`. Calling `build()` creates and validates a configuration snapshot. This avoids a large constructor with many genre-specific arguments and prevents later builder changes from altering an existing project.

**Factory Method** creates scenarios through `ScenarioCreator`. Its public creation workflow validates the project, checks the dimension, and prepares seeded randomness, then invokes the abstract `createScenario` method. `ActionScenarioCreator` and `HorrorScenarioCreator` implement that method for their respective products. This keeps genre-specific construction out of the console and shares the common creation workflow.

**Bridge** separates scenario genre from dimension capabilities. `GameScenario` holds a `DimensionRules` implementation. Its Action and Horror subclasses delegate interaction capabilities to `Rules2D` or `Rules3D`. This avoids separate scenario subclasses for every genre/dimension combination, such as Action2D and Action3D. Gameplay uses those capabilities to determine legal choices.

**Strategy** separates enemy algorithms from gameplay coordination. Each `ActionCombatant` delegates defense and retaliation to its own `EnemyBehavior`. `HorrorEngine` delegates each room's encounter rules to its own `HorrorBehavior`. The aggressive, cautious, Watcher, and Distractor implementations hold their specific logic and state. This keeps behavior algorithms out of one large engine conditional and prevents enemies from accidentally sharing turn phases.

**Observer** connects gameplay events to statistics and logging. Engines publish enemy defeats, cleared locations, player defeat, and scenario completion through `GameEventPublisher`. `StatisticsTracker` and `ResultLogger` receive notifications through `GameEventListener`. Engines can announce what happened without directly updating each reporting component. Duplicate subscriptions are prevented, and completed sessions reject further actions.

**Facade** gives the console a common entry point through `GameEngineFacade`. It selects dimension rules and creators, constructs the appropriate engine, attaches observers, and exposes actions, status, results, and restart. This prevents console code from having to assemble and coordinate the gameplay subsystems itself.

### Classes grouped by pattern

Some classes participate in more than one pattern. Supporting collaborators are listed separately from core pattern roles.

| Pattern | Core participants | Supporting collaborators and clients |
| --- | --- | --- |
| Builder | `GameProjectBuilder` — builder; `GameProject` — product | `ActionConfig`, `HorrorConfig`, `ActionEnemySpec`, `HorrorEnemySpec` — immutable product data; `ProjectValidator` — validation; `ProjectEditor` — client |
| Factory Method | `ScenarioCreator` — abstract creator; `ActionScenarioCreator`, `HorrorScenarioCreator` — concrete creators; `GameScenario` — product abstraction; `ActionScenario`, `HorrorScenario` — concrete products | `ScenarioSpawner` — instance creation and placement; `GameEngineFacade` — creator selection/client |
| Bridge | `GameScenario` — abstraction; `ActionScenario`, `HorrorScenario` — refined abstractions; `DimensionRules` — implementor; `Rules2D`, `Rules3D` — concrete implementors | Engines and Horror strategies consume the interaction capabilities |
| Strategy — Action | `EnemyBehavior` — strategy contract; `AggressiveBehavior`, `CautiousBehavior` — concrete strategies; `ActionCombatant` — context | `EnemyStance` — phase values; `ActionEngine` — combat coordinator |
| Strategy — Horror | `HorrorBehavior` — strategy contract; `WatcherBehavior`, `DistractorBehavior` — concrete strategies; `HorrorEngine` — context | `EncounterStep` — strategy result |
| Observer | `GameEventPublisher` — publisher; `GameEventListener` — observer contract; `StatisticsTracker`, `ResultLogger` — concrete observers | `GameEvent`, `EventType` — notifications; engines — event sources; facade — observer registration |
| Facade | `GameEngineFacade` — facade | Creators, dimension rules, engines and observers — subsystems; `ConsoleController`, `GameConsole` — clients |

### Class responsibilities

| Package / classes | Responsibility |
| --- | --- |
| `Main` | Start the application and handle closed input. |
| `console.ConsoleController` | Coordinate the project menu, previews, selected seed, and simulation entry. |
| `console.ProjectEditor` | Collect client settings through the Builder and validate a playable configuration. |
| `console.InputReader` | Read text, retry invalid numbers, and interpret behavior choices. |
| `console.ProjectPrinter` | Display project settings and generated layouts. |
| `console.GameConsole` | Display gameplay actions, submit choices through the facade, and handle results/replay. |
| `config.GameProjectBuilder`, `GameProject` | Assemble settings and retain the immutable project snapshot. |
| `config.ActionConfig`, `HorrorConfig` | Hold player/enemy settings or encounter/Pursuer settings. |
| `config.ActionEnemySpec`, `HorrorEnemySpec` | Describe one configured enemy type and its quantity. |
| `config.ProjectValidator` | Enforce names, ranges, quantities, genre compatibility, and supported behaviors. |
| `creation.ScenarioCreator` | Run shared validation and seeded generation, delegating construction to its factory method. |
| `creation.ActionScenarioCreator`, `HorrorScenarioCreator` | Construct the appropriate scenario with the selected dimension rules. |
| `creation.ScenarioSpawner` | Expand quantities into fresh instances and randomize buildings or room order. |
| `dimension.GameScenario` | Hold the source project and composed dimension rules. |
| `dimension.ActionScenario` | Hold player HP and buildings; nested `Building` and `EnemyInstance` hold occupants and individual enemy HP. |
| `dimension.HorrorScenario` | Hold rooms and the Pursuer countdown; nested `EncounterRoom` identifies its encounter specification. |
| `dimension.DimensionRules`, `Rules2D`, `Rules3D` | Define and supply dimension-specific interaction capabilities. |
| `action.ActionEngine` | Resolve attacks, retaliation, dodging, building progression, elevator escape, and Action results/events. |
| `action.ActionCombatant` | Combine an enemy instance, display label, and independent combat strategy. |
| `action.EnemyBehavior`, `AggressiveBehavior`, `CautiousBehavior`, `EnemyStance` | Define combat behavior and implement always-attack or alternating defense/attack logic. |
| `horror.HorrorEngine` | Coordinate room progression, countdown requirements/accounting, final escape, and Horror results/events. |
| `horror.HorrorBehavior`, `WatcherBehavior`, `DistractorBehavior` | Define encounter behavior and implement hide/advance or search/distraction sequences. |
| `horror.EncounterStep` | Return an encounter message, time cost, fatality, and clearance result to the engine. |
| `engine.GameEngine` | Define the common gameplay interface used by the facade for both genres. |
| `engine.GameAction`, `ActionType` | Identify an action, optional target, and display label. |
| `engine.TurnResult`, `GameOutcome` | Report acceptance, turn consumption, messages, and session outcome. |
| `events.GameEvent`, `EventType` | Describe a gameplay notification and its category. |
| `events.GameEventListener`, `GameEventPublisher` | Define observer notification and manage subscriptions/event delivery. |
| `events.StatisticsTracker` | Count actual defeated enemies and cleared buildings/rooms. |
| `events.ResultLogger` | Retain a chronological event log for the results display. |
| `facade.GameEngineFacade` | Assemble/manage sessions and expose generation, actions, status, results, and restart. |

The nested runtime holders remain small parts of their scenarios. Player HP and the Pursuer countdown also stay on their scenarios, so the current implementation does not need separate `Player` or `PursuerClock` classes.
