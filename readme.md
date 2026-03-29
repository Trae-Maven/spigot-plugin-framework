# Spigot-Plugin-Framework

A Spigot/Paper plugin framework providing structured command systems, event utilities, and lifecycle integration built on the [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework).

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
UtilMessage.message(player, "Factions", "<green>You have joined the faction!");

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

## Utilities

| Utility | Description |
|---|---|
| `UtilEvent` | Synchronous and asynchronous event dispatch with supply variants |
| `UtilTask` | Task scheduling — immediate, synchronous, asynchronous, and repeating with ChronoUnit-to-tick conversion |
| `UtilMessage` | MiniMessage-based messaging with configurable prefixes, broadcasting, filtering, and ignore lists |
| `UtilPlugin` | Plugin lookup — internal by name or class |

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
