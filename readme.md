# Spigot-Plugin-Framework

A Spigot/Paper plugin framework providing structured command systems, event utilities, packet-based sidebars and teams, and lifecycle integration built on the [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework).

Spigot-Plugin-Framework bridges the Bukkit plugin lifecycle with the component-based hierarchy architecture, automatically handling registration and teardown of listeners, commands, and subcommands as components are initialized and shut down.

---

## Features

- Automatic Bukkit registration — listeners, commands, and subcommands are registered/unregistered through hierarchy lifecycle callbacks
- Type-safe command system with sender validation — Player, Console, or any CommandSender
- Built-in subcommand routing with automatic argument stripping and tab completion delegation
- Cancellable command events at every execution stage — execute and tab-complete
- Thread-safe event dispatch utilities — synchronous and asynchronous with `CompletableFuture` support
- Task scheduling with ChronoUnit-to-tick conversion — synchronous, asynchronous, and repeating with cancellation suppliers
- MiniMessage-based messaging — configurable prefixes, broadcasting, filtering, and ignore lists
- Packet-based sidebar system with priority resolution — only changed lines and title produce packets, zero flicker, dynamic animated titles
- Packet-based team system with per-viewer prefix/suffix resolution — relation-aware nametag colors via priority-sorted `Team` implementations
- NMS utilities for direct packet sending and Adventure-to-vanilla component conversion
- Custom event base classes with cancellation reasons
- Compatible with Bukkit, Spigot, and Paper
- Designed for modern Java (Java 21+)

---

## Hierarchy
```
SpigotPlugin (extends JavaPlugin, implements Plugin)
  └─ Manager
       └─ BaseCommand / Module
            └─ BaseSubCommand / SubModule
```

Commands integrate directly into the hierarchy as Modules, and subcommands as SubModules:

| Component | Hierarchy Role | Bukkit Integration |
|---|---|---|
| `SpigotPlugin` | Plugin | `JavaPlugin` lifecycle, component registration |
| `Manager` | Manager | Organizational grouping |
| `BaseCommand` | Module | Registered with `CommandMap` |
| `BaseSubCommand` | SubModule | Attached to parent command |

---

## Requirements

Spigot-Plugin-Framework requires Java 21+ and a Paper API environment.

### NMS Access (paper-nms-maven-plugin)

The sidebar/team systems and `UtilNms` use NMS (net.minecraft.server) classes directly. To compile against NMS with Maven, the framework uses the [paper-nms-maven-plugin](https://github.com/Alvinn8/paper-nms-maven-plugin).

Add `.paper-nms` to your `.gitignore` — it contains locally generated dependencies.

After cloning, run the init goal once to generate the NMS dependency in your local `.m2` repository:
```bash
mvn ca.bkaw:paper-nms-maven-plugin:1.4.10:init -pl .
```

> **Note:** If `mvn` is not on your PATH, you can run it through IntelliJ's Maven tool window: expand Plugins → `paper-nms` → double-click `paper-nms:init`.

> **Note:** The init goal requires your `JAVA_HOME` to point to JDK 21. If it fails with a Java version error, set it before running:
> ```bash
> # PowerShell
> $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
> mvn ca.bkaw:paper-nms-maven-plugin:1.4.10:init -pl .
> ```

The following is only needed at compile time for annotation processing:
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.36</version>
    <scope>provided</scope>
</dependency>
```

---

## Built-in Dependencies

Spigot-Plugin-Framework depends on the following libraries, which are included automatically through Maven:

- [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework) – Plugin, Manager, Module, SubModule hierarchy with lifecycle management.
- [Dependency Injector](https://github.com/Trae-Maven/dependency-injector) – Container management, classpath scanning, and component wiring.
- [Utilities](https://github.com/Trae-Maven/utilities) – Generic type resolution, string utilities, and casting helpers.

---

## Installation

Add the dependency to your Maven project:
```xml
<dependencies>
    <dependency>
        <groupId>io.github.trae</groupId>
        <artifactId>spigot-plugin-framework</artifactId>
        <version>0.0.1</version>
    </dependency>

    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## Quick Start

### Defining the Plugin

Extend `SpigotPlugin` to get automatic listener, command, and subcommand registration:
```java
@Application
public class CorePlugin extends SpigotPlugin {

    @Override
    public void onEnable() {
        this.initializePlugin();
    }

    @Override
    public void onDisable() {
        this.shutdownPlugin();
    }
}
```

### Defining a Command

Extend `BaseCommand` with the appropriate sender type. Permission is passed via the constructor:
```java
@Component
public class AccountCommand extends BaseCommand<CorePlugin, AccountManager, CommandSender> {

    public AccountCommand() {
        super("account", "Account management", "core.commands.account", Collections.emptyList());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("Account command executed!");
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
```

### Defining a SubCommand

SubCommands are automatically attached to their parent command through the hierarchy:
```java
@Component
public class AccountAdminSubCommand extends BaseSubCommand<CorePlugin, AccountCommand, Player> {

    private final AccountManager accountManager;

    public AccountAdminSubCommand(AccountManager accountManager) {
        super("admin", "Toggle Admin Mode", "core.commands.account.admin", Collections.emptyList());

        this.accountManager = accountManager;
    }

    @Override
    public void execute(Player player, String[] args) {
        this.accountManager.getAccountByPlayer(player).ifPresent(account -> {
            if (account.isAdministrating()) {
                account.setAdministrating(false);

                UtilMessage.message(player, "Account", UtilString.pair("Admin Mode", "<red>Disabled</red>"));
            } else {
                account.setAdministrating(true);

                UtilMessage.message(player, "Account", UtilString.pair("Admin Mode", "<green>Enabled</green>"));
            }
        });
    }

    @Override
    public List<String> getTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
}
```

This registers `/account admin` automatically — the parent `AccountCommand` routes the `admin` argument to `AccountAdminSubCommand` with the remaining args.

### Command Execution Flow
```
/account admin
  │
  ├─ Sender type validation (Player)
  ├─ Permission check (core.commands.account.admin)
  ├─ CommandExecuteEvent (cancellable)
  └─ AccountAdminSubCommand.execute(player, new String[0])
```

### Event Dispatch

Use `UtilEvent` for thread-safe event dispatch:
```java
// Synchronous — fire and inspect
MyEvent event = UtilEvent.supply(new MyEvent());
if (event.isCancelled()) {
        return;
        }

// Asynchronous — fire and forget
        UtilEvent.dispatchAsynchronous(new MyAsyncEvent());

// Asynchronous — fire and chain
        UtilEvent.supplyAsynchronous(new MyAsyncEvent()).thenAccept(e -> System.out.println("Done: " + e.isCancelled()));
```

### Task Execution

Use `UtilTask` for scheduling across Bukkit's threading model:
```java
// Execute on the main server thread
UtilTask.executeSynchronous(() -> {
        player.teleport(spawn);
});

// Execute asynchronously off the main thread
        UtilTask.executeAsynchronous(() -> {
        // Heavy computation or I/O
        });

// Repeating task on the main thread with cancellation
        UtilTask.schedule(() -> {
        player.sendMessage("Tick!");
}, 0, 1, ChronoUnit.SECONDS, () -> !player.isOnline());

// Repeating async task
        UtilTask.scheduleAsynchronous(() -> {
        // Periodic background work
        }, 0, 5, ChronoUnit.SECONDS);
```

### Messaging

Use `UtilMessage` for MiniMessage-formatted messaging with configurable prefixes:
```java
// Prefixed message to a player
UtilMessage.message(player, "Factions", "You joined <aqua>Faction %s</aqua>.".formatted(faction.getName()));

// Prefixed message with MiniMessage tags
        UtilMessage.message(player, "Shop", "<gold>+50 coins</gold> from daily reward!");

// Message a Collection of Players with Predicate and Ignored
UtilMessage.message(players, "Punish", "<yellow>%s</yellow> has banned <yellow>%s</yellow> for <light_purple>%s</light_purple>.".formatted(sender.getName(), target.getName(), duration), player -> player.isOp(), Collections.singletonList(target.getUniqueId()));

// Broadcast to all online players
        UtilMessage.broadcast("Server", "<red><bold>Restarting</bold></red> in <yellow>5 minutes</yellow>.");

// Broadcast with ignore list
UtilMessage.broadcast("Alert", "<red>PvP is now enabled!</red>", List.of(excludedPlayerUUID));

// Log to console
        UtilMessage.log("Core", "Plugin loaded successfully!");
```

---

## Sidebar System

The framework provides a packet-based sidebar (scoreboard) system with priority-based resolution. Multiple `Sidebar` implementations can be registered — the lowest priority one that passes all display checks is shown. Only changed lines and title produce packets, eliminating flicker.

### Defining a Sidebar Manager

Extend `AbstractSidebarManager` in your plugin and register it as a service:
```java
@Service
public class SidebarManager extends AbstractSidebarManager<CorePlugin> {}
```

### Defining a Sidebar

Implement `Sidebar` and register it as a component. The manager discovers all implementations automatically via the dependency injector:

```java
@AllArgsConstructor
@Component
public class HubSidebar implements Sidebar {

    private final PlayerManager playerManager;

    @Override
    public String getIdentifier() {
        return "hub";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Component getTitle(final Player player) {
        return Component.text("MY SERVER", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    @Override
    public List<Component> getLines(final Player player) {
        final PlayerData data = this.playerManager.getPlayerData(player);

        return List.of(
                Component.text("Online: ", NamedTextColor.GRAY).append(Component.text(Bukkit.getOnlinePlayers().size(), NamedTextColor.WHITE)),
                Component.text("Rank: ", NamedTextColor.GRAY).append(Component.text(data.getRank(), NamedTextColor.GOLD)),
                Component.text("Coins: ", NamedTextColor.GRAY).append(Component.text(data.getCoins(), NamedTextColor.YELLOW))
        );
    }
}
```

### Animated Title

Override `isStaticTitle()` to enable per-tick title updates driven by the manager's scheduler:
```java
private int tick = 0;

private static final List<TextColor> COLORS = List.of(
    NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW,
    NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE
);

@Override
public boolean isStaticTitle() {
    return false;
}

@Override
public Component getTitle(final Player player) {
    return Component.text("MY SERVER", COLORS.get(this.tick++ % COLORS.size()), TextDecoration.BOLD);
}
```

### Priority Resolution

Lower priority always wins. When the lowest-numbered sidebar becomes ineligible, the next one takes over automatically:

```java
@AllArgsConstructor
@Component
public class FactionsSidebar implements Sidebar {
    
    private final FactionManager factionManager;

    @Override
    public String getIdentifier() {
        return "factions";
    }

    @Override
    public int getPriority() {
        return 0; // wins over HubSidebar at 10
    }

    @Override
    public boolean canDisplay(final Player player) {
        return this.factionsManager.isInFaction(player);
    }

    @Override
    public Component getTitle(final Player player) {
        return Component.text("FACTIONS", NamedTextColor.RED, TextDecoration.BOLD);
    }

    @Override
    public List<Component> getLines(final Player player) {
        // faction-specific lines
    }
}
```

### Updating a Sidebar

Fire `SidebarUpdateEvent` to trigger a line refresh for a player:
```java
// Update whatever sidebar is currently active
UtilEvent.dispatch(new SidebarUpdateEvent(player));

// Only update if the active sidebar matches the given identifier
UtilEvent.dispatch(new SidebarUpdateEvent("hub", player));
```

---

## Team System

The framework provides a packet-based team system for per-viewer prefix/suffix resolution. Each online player has a team entry sent individually to every viewer, allowing relation-aware nametag colors (e.g. faction ally vs enemy).

### Defining a Team Manager

Extend `AbstractTeamManager` in your plugin and register it as a service:
```java
@Service
public class TeamManager extends AbstractTeamManager<CorePlugin> {}
```

### Defining a Team

Implement `Team` and register it as a component. Lower priority teams win when multiple are eligible:

```java
@AllArgsConstructor
@Component
public class RankTeam implements Team {

    private final PlayerManager playerManager;

    @Override
    public String getIdentifier() {
        return "rank";
    }

    @Override
    public int getPriority() {
        return 10; // fallback
    }

    @Override
    public Component getPrefix(final Player player, final Player viewer) {
        final String rank = this.playerManager.getPlayerData(player).getRank();
        return Component.text("[" + rank + "] ", NamedTextColor.GOLD);
    }
}
```

```java
@AllArgsConstructor
@Component
public class FactionsTeam implements Team {

    private final FactionsManager factionsManager;

    @Override
    public String getIdentifier() {
        return "factions";
    }

    @Override
    public int getPriority() {
        return 0; // wins over RankTeam
    }

    @Override
    public boolean canDisplay(final Player player, final Player viewer) {
        return this.factionsManager.isInFaction(player);
    }

    @Override
    public Component getPrefix(final Player player, final Player viewer) {
        final FactionRelation relation = this.factionsManager.getRelation(viewer, player);
        return switch (relation) {
            case ALLY -> Component.text("[ALLY] ", NamedTextColor.GREEN);
            case ENEMY -> Component.text("[ENEMY] ", NamedTextColor.RED);
            default -> Component.text("[NEUTRAL] ", NamedTextColor.YELLOW);
        };
    }
}
```

### Updating a Team

Fire `TeamUpdateEvent` to push prefix/suffix updates to all viewers:
```java
// Update this player's team for all viewers
UtilEvent.dispatch(new TeamUpdateEvent(player));

// Only update if the active team matches the given identifier
UtilEvent.dispatch(new TeamUpdateEvent("factions", player));
```

---

## NMS Utilities

`UtilNms` provides direct access to NMS operations without requiring each consumer to handle CraftBukkit casting:

```java
// Convert Adventure component to vanilla Minecraft component
net.minecraft.network.chat.Component nmsComponent = UtilNms.toNms(adventureComponent);

// Send a raw NMS packet to a player (safe from any thread)
UtilNms.sendPacket(player, packet);
```

Packet sending writes directly to the Netty channel pipeline, bypassing the main thread. This is what enables the sidebar and team systems to run without blocking the main thread.

---

## Utilities

| Utility | Description |
|---|---|
| `UtilEvent` | Synchronous and asynchronous event dispatch with supply variants |
| `UtilTask` | Task scheduling — immediate, synchronous, asynchronous, and repeating with ChronoUnit-to-tick conversion |
| `UtilMessage` | MiniMessage-based messaging with configurable prefixes, broadcasting, filtering, and ignore lists |
| `UtilPlugin` | Plugin lookup — internal by name or class |
| `UtilNms` | NMS packet sending and Adventure-to-vanilla component conversion |

---

## Command Types

| Type | Sender | Use Case |
|---|---|---|
| `BaseCommand<Plugin, Manager, CommandSender>` | `CommandSender` | Any sender |
| `BaseCommand<Plugin, Manager, Player>` | `Player` | Player-only commands |
| `BaseCommand<Plugin, Manager, ConsoleCommandSender>` | `ConsoleCommandSender` | Console-only commands |

| SubCommand Type | Sender | Use Case |
|---|---|---|
| `BaseSubCommand<Plugin, Command, CommandSender>` | `CommandSender` | Any sender |
| `BaseSubCommand<Plugin, Command, Player>` | `Player` | Player-only subcommands |
| `BaseSubCommand<Plugin, Command, ConsoleCommandSender>` | `ConsoleCommandSender` | Console-only subcommands |

---

## Event Types

| Event Type | Description |
|---|---|
| `CustomEvent` | Base synchronous event with `Void` key type |
| `CustomAsyncEvent` | Base asynchronous event with `Void` key type |
| `CustomCancellableEvent` | Synchronous event with cancellation and reason |
| `CustomCancellableAsyncEvent` | Asynchronous event with cancellation and reason |

---

## Command Events

| Event | Fired When |
|---|---|
| `CommandExecuteEvent` | Any command or subcommand is about to execute |
| `CommandTabCompleteEvent` | Any command or subcommand tab completion is requested |

All events are cancellable. Cancelling an execute event prevents execution; cancelling a tab complete event returns an empty list.

---

## Sidebar Events

| Event | Fired When |
|---|---|
| `SidebarUpdateEvent` | A sidebar update is requested for a player |

---

## Team Events

| Event | Fired When |
|---|---|
| `TeamUpdateEvent` | A team prefix/suffix update is requested for a player |

---

## Interfaces

| Interface | Description |
|---|---|
| `SpigotPlugin` | Root plugin with automatic Bukkit registration callbacks |
| `SharedCommand` | Shared contract between commands and subcommands — sender validation, permission, execution, and tab-complete |
| `IBaseCommand` | Command contract with subcommand management |
| `Sidebar` | Contract for a priority-sorted sidebar implementation |
| `Team` | Contract for a priority-sorted, per-viewer team prefix/suffix implementation |
| `ICustomCancellableEvent` | Cancellable event with reason support |
