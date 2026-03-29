# Spigot-Plugin-Framework

A Spigot/Paper plugin framework providing structured command systems, event utilities, packet-based scoreboards, and lifecycle integration built on the [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework).

Spigot-Plugin-Framework bridges the Bukkit plugin lifecycle with the component-based hierarchy architecture, automatically handling registration and teardown of listeners, commands, and subcommands as components are initialized and shut down.

---

## Features

- Automatic Bukkit registration — listeners, commands, and subcommands are registered/unregistered through hierarchy lifecycle callbacks
- Type-safe command system with sender validation — Player, Console, or any CommandSender
- Built-in subcommand routing with automatic argument stripping and tab completion delegation
- Cancellable command events at every execution stage — pre-execute, execute, and tab-complete
- Pluggable command settings via `ICommandSettings` — permission checks and messaging resolved through the dependency injector
- Thread-safe event dispatch utilities — synchronous and asynchronous with `CompletableFuture` support
- Task scheduling with ChronoUnit-to-tick conversion — synchronous, asynchronous, and repeating with cancellation suppliers
- MiniMessage-based messaging — configurable prefixes, broadcasting, filtering, and ignore lists
- Asynchronous packet-based scoreboard system with priority resolution — only changed lines produce packets, zero flicker
- NMS utilities for direct packet sending and Adventure-to-vanilla component conversion
- Custom event base classes with cancellation reasons
- Compatible with Bukkit, Spigot, and Paper
- Designed for modern Java (Java 21+)

---

## Hierarchy
```
SpigotPlugin (extends JavaPlugin, implements Plugin)
  └─ Manager
       └─ AbstractCommand / Module
            └─ AbstractSubCommand / SubModule
```

Commands integrate directly into the hierarchy as Modules, and subcommands as SubModules:

| Component | Hierarchy Role | Bukkit Integration |
|---|---|---|
| `SpigotPlugin` | Plugin | `JavaPlugin` lifecycle, component registration |
| `Manager` | Manager | Organizational grouping |
| `AbstractCommand` | Module | Registered with `CommandMap` |
| `AbstractSubCommand` | SubModule | Attached to parent command |

---

## Requirements

Spigot-Plugin-Framework requires Java 21+ and a Paper API environment.

### NMS Access (paper-nms-maven-plugin)

The scoreboard system and `UtilNms` use NMS (net.minecraft.server) classes directly. To compile against NMS with Maven, the framework uses the [paper-nms-maven-plugin](https://github.com/Alvinn8/paper-nms-maven-plugin).

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
        <version>1.0-SNAPSHOT</version>
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

### Implementing Command Settings

Create a concrete `ICommandSettings` component to define permission and messaging behaviour:
```java
@Component
public class CommandSettings implements ICommandSettings {

    @Override
    public boolean hasPermission(ISharedCommand<?> command, CommandSender sender) {
        return ICommandSettings.super.hasPermission(command, sender);
    }

    @Override
    public void sendInvalidCommandSenderMessage(ISharedCommand<?> command, CommandSender sender) {
        sender.sendMessage("Invalid Command Sender!");
    }

    @Override
    public void sendInsufficientPermissionMessage(ISharedCommand<?> command, CommandSender sender) {
        sender.sendMessage("You do not have permission to execute this command!");
    }
}
```

### Defining a Command

Choose a base type based on the required sender:
```java
@Component
public class AccountCommand extends Command<CorePlugin, AccountManager> {

    public AccountCommand() {
        super("account", "Account management", Collections.emptyList());
        
        this.setPermission("core.commands.account");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("Account command executed!");
    }

    @Override
    public List<String> getTabCompletion(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
```

### Defining a SubCommand

SubCommands are automatically attached to their parent command through the hierarchy:
```java
@AllArgsConstructor
@Component
public class AccountAdminSubCommand extends PlayerSubCommand<CorePlugin, AccountCommand> {
    
    private final AccountManager accountManager;

    public AccountAdminSubCommand() {
        super("admin", "Toggle Admin Mode");
        
        this.setPermission("core.commands.account.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        this.accountManager.getAccountByPlayer(player).ifPresent(account -> {
            if (account.isAdministrating()) {
                account.setAdministrating(false);
                
                player.sendMessage("Admin Mode: Disabled");
            } else {
                account.setAdministrating(true);
                
                player.sendMessage("Admin Mode: Enabled");
            }
        });
    }

    @Override
    public List<String> getTabCompletion(Player player, String[] args) {
        return Collections.emptyList();
    }
}
```

This registers `/account admin` automatically — the parent `AccountCommand` routes the `admin` argument to `AccountAdminSubCommand` with the remaining args.

### Command Execution Flow
```
/account admin
  │
  ├─ Sender type validation (any CommandSender)
  ├─ Permission check via ICommandSettings
  ├─ SubCommandExecuteEvent (cancellable)
  └─ AccountAdminSubCommand.execute(sender, new String[0])
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
UtilMessage.message(player, "Shop", "<gold>+50 coins <gray>from daily reward");

// Broadcast to all online players
UtilMessage.broadcast("Server", "<red><bold>Restarting</bold></red> in <yellow>5 minutes");

// Broadcast with ignore list
UtilMessage.broadcast("Alert", "<red>PvP is now enabled!", List.of(excludedPlayerUUID));

// Log to console
UtilMessage.log("Core", "Plugin loaded successfully");
```

---

## Scoreboard System

The framework provides an asynchronous packet-based scoreboard manager with priority-based resolution. Multiple systems can register scoreboards for the same player — the highest priority board is always displayed, and removing it falls back to the next highest automatically.

All diffing and packet construction runs off the main thread. Only changed lines and title produce packets, eliminating flicker and minimising bandwidth.

### Setting a Scoreboard

Use `ScoreboardManager.board()` to build the layout, then `set()` to register it with a key and priority:
```java
private final ScoreboardManager scoreboardManager;

// Lobby scoreboard at priority 0
this.scoreboardManager.set(player, "lobby", 0,
    Component.text("  MY SERVER  ", NamedTextColor.GOLD, TextDecoration.BOLD),
    ScoreboardManager.board()
        .pair(NamedTextColor.GRAY, "Server", "Lobby-1")
        .pair(NamedTextColor.GOLD, "Rank", "Owner")
        .pair(NamedTextColor.GREEN, "Gems", "1,500")
        .lineCompact(Component.text("play.myserver.com", NamedTextColor.RED, TextDecoration.BOLD))
);
```

### Priority Resolution

Higher priority always wins. When a higher priority board is removed, the next one takes over seamlessly:
```java
// Game system takes over at priority 1
this.scoreboardManager.set(player, "game", 1,
    Component.text("  DOMINATION  ", NamedTextColor.RED, TextDecoration.BOLD),
    ScoreboardManager.board()
        .pair(NamedTextColor.GRAY, "Map", "Desert Temple")
        .pair(NamedTextColor.AQUA, "Team", "Blue")
        .pair(NamedTextColor.GREEN, "Kills", "0")
        .pair(NamedTextColor.RED, "Deaths", "0")
        .lineCompact(Component.text("play.myserver.com", NamedTextColor.RED, TextDecoration.BOLD))
);

// Game ends — remove it, lobby board reappears automatically
this.scoreboardManager.remove(player, "game");
```

### Updating Lines

Re-calling `set()` with the same key diffs against what's currently rendered. Only changed lines produce packets:
```java
// Only the "Kills" and "Deaths" values changed — only those 2 lines send packets
scoreboardManager.set(player, "game", 1,
    Component.text("  DOMINATION  ", NamedTextColor.RED, TextDecoration.BOLD),
    ScoreboardManager.board()
        .pair(NamedTextColor.GRAY, "Map", "Desert Temple")     // unchanged — no packet
        .pair(NamedTextColor.AQUA, "Team", "Blue")              // unchanged — no packet
        .pair(NamedTextColor.GREEN, "Kills", "3")                // changed — packet sent
        .pair(NamedTextColor.RED, "Deaths", "1")                 // changed — packet sent
        .lineCompact(Component.text("play.myserver.com", NamedTextColor.RED, TextDecoration.BOLD))  // unchanged — no packet
);
```

### Custom Lines

Use `line()` for a component with a trailing spacer, or `lineCompact()` for no spacer:
```java
ScoreboardManager.board()
    .pair(NamedTextColor.GRAY, "Map", "Desert Temple")
    .line(Component.text("Time: ", NamedTextColor.YELLOW, TextDecoration.BOLD).append(Component.text("2:34", NamedTextColor.WHITE)))
    .pair(NamedTextColor.GREEN, "Kills", "3")
    .lineCompact(Component.text("play.myserver.com", NamedTextColor.RED, TextDecoration.BOLD))
```

### Checking Active Board

```java
if (this.scoreboardManager.isActive(player, "game")) {
    // The game board is currently the one being displayed
}
```

### Cleanup

Call `cleanup()` when a player disconnects to free all state:
```java
this.scoreboardManager.cleanup(player.getUniqueId());
```

### BoardBuilder API

| Method | Description |
|---|---|
| `pair(NamedTextColor, String, String)` | Bold coloured label + white value + blank spacer |
| `line(Component)` | Single component line + blank spacer |
| `lineCompact(Component)` | Single component line, no trailing spacer |
| `blank()` | Empty spacer line |

---

## NMS Utilities

`UtilNms` provides direct access to NMS operations without requiring each consumer to handle CraftBukkit casting:

```java
// Convert Adventure component to vanilla Minecraft component
net.minecraft.network.chat.Component nmsComponent = UtilNms.toNms(adventureComponent);

// Send a raw NMS packet to a player (safe from any thread)
UtilNms.sendPacket(player, packet);
```

Packet sending writes directly to the Netty channel pipeline, bypassing the main thread. This is what enables the scoreboard system to run entirely asynchronously.

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
| `Command` | `CommandSender` | Any sender |
| `PlayerCommand` | `Player` | Player-only commands |
| `ServerCommand` | `ConsoleCommandSender` | Console-only commands |

| SubCommand Type | Sender | Use Case |
|---|---|---|
| `SubCommand` | `CommandSender` | Any sender |
| `PlayerSubCommand` | `Player` | Player-only subcommands |
| `ServerSubCommand` | `ConsoleCommandSender` | Console-only subcommands |

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
| `CommandExecuteEvent` | Root command is about to execute |
| `CommandTabCompleteEvent` | Root command tab completion is requested |
| `SubCommandExecuteEvent` | Subcommand is about to execute |
| `SubCommandTabCompleteEvent` | Subcommand tab completion is requested |

All events are cancellable. Cancelling an execute event prevents execution; cancelling a tab complete event returns an empty list.

---

## Interfaces

| Interface | Description |
|---|---|
| `SpigotPlugin` | Root plugin with automatic Bukkit registration callbacks |
| `SpigotManager` | Spigot-bound manager within the hierarchy |
| `SpigotModule` | Spigot-bound module (commands, listeners) |
| `SpigotSubModule` | Spigot-bound sub-module (subcommands) |
| `ICommandSettings` | Pluggable permission checks and command messaging |
| `ISharedCommand` | Shared contract between commands and subcommands |
| `IAbstractCommand` | Command contract with subcommand management |
| `IAbstractSubCommand` | SubCommand contract with internal execution entry points |
| `ICustomCancellableEvent` | Cancellable event with reason support |
| `ICommandEvent` | Shared contract for command-related events |
| `IScoreboardManager` | Scoreboard lifecycle — set, remove, priority check, and cleanup |
