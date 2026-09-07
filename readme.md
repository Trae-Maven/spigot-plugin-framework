# Spigot-Plugin-Framework

A Spigot/Paper plugin framework providing structured command systems, event utilities, packet-based sidebars, tablists and teams, a custom item system, an inventory window system, and lifecycle integration built on the [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework).

Spigot-Plugin-Framework bridges the Bukkit plugin lifecycle with the component-based hierarchy architecture, automatically handling registration and teardown of listeners, commands, and subcommands as components are initialized and shut down.

---

## Features

- Automatic Bukkit registration, so listeners, commands, and subcommands are registered and unregistered through hierarchy lifecycle callbacks
- Type-safe command system with sender validation for Player, Console, or any CommandSender
- Built-in subcommand routing with automatic argument stripping and tab completion delegation
- Cancellable command events at every execution stage, both execute and tab-complete
- Thread-safe event dispatch utilities, synchronous and asynchronous with `CompletableFuture` support
- Task scheduling with ChronoUnit-to-tick conversion: synchronous, asynchronous, and repeating with cancellation suppliers
- MiniMessage-based messaging with configurable prefixes, broadcasting, filtering, and ignore lists
- Packet-based sidebar system with priority resolution, where only changed lines and titles produce packets, giving zero flicker and dynamic animated titles
- Tablist system with priority resolution for per-player header and footer content
- Packet-based team system with per-viewer prefix and suffix resolution, giving relation-aware nametag colours through priority-sorted `Team` subclasses
- Declarative item system with identity stamping and automatic version reconciliation, so stacks in player inventories update themselves when the definition changes
- Opt-in item activation, so a custom item gains a click action with its own gate, cancellable events, and control over the vanilla behaviour it replaces
- Inventory window system with slot-bound buttons, open and close gating, and full click and drag protection
- NMS utilities for direct packet sending and Adventure-to-vanilla component conversion
- Custom event base classes with cancellation reasons
- Opt-in subsystems through `@Scan`, so a plugin enables only the packages it wants
- Compatible with Bukkit, Spigot, and Paper
- Designed for modern Java (Java 21+)

---

## Hierarchy

```
SpigotPlugin (extends JavaPlugin, implements Plugin)
  └─ Manager
       └─ BaseCommand (Node under the Manager)
            └─ BaseSubCommand (Node under the command)
```

Commands and subcommands integrate directly into the hierarchy as Nodes, each with typed access to its parent:

| Component | Hierarchy Role | Bukkit Integration |
|---|---|---|
| `SpigotPlugin` | Plugin | `JavaPlugin` lifecycle, component registration |
| `Manager` | Manager | Organizational grouping |
| `BaseCommand` | Node under a Manager | Registered with `CommandMap` |
| `BaseSubCommand` | Node under a command | Attached to parent command |

The sidebar, tablist, team, item, and window systems sit outside this hierarchy. Their managers and listeners are framework-owned singletons, discovered through `@Scan` rather than declared per plugin. See [Enabling Subsystems](#enabling-subsystems).

---

## Requirements

Spigot-Plugin-Framework requires Java 21+ and a Paper API environment.

### NMS Access (paper-nms-maven-plugin)

The sidebar and team systems and `UtilNms` use NMS (net.minecraft.server) classes directly. To compile against NMS with Maven, the framework uses the [paper-nms-maven-plugin](https://github.com/Alvinn8/paper-nms-maven-plugin).

Add `.paper-nms` to your `.gitignore`, as it contains locally generated dependencies.

After cloning, run the init goal once to generate the NMS dependency in your local `.m2` repository:

```bash
mvn ca.bkaw:paper-nms-maven-plugin:1.5:init -pl .
```

> **Note:** If `mvn` is not on your PATH, you can run it through IntelliJ's Maven tool window: expand Plugins → `paper-nms` → double-click `paper-nms:init`.

> **Note:** The init goal requires your `JAVA_HOME` to point to JDK 21. If it fails with a Java version error, set it before running:
> ```bash
> # PowerShell
> $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
> mvn ca.bkaw:paper-nms-maven-plugin:1.5:init -pl .
> ```

The following is only needed at compile time for annotation processing:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.46</version>
    <scope>provided</scope>
</dependency>
```

---

## Built-in Dependencies

Spigot-Plugin-Framework depends on the following libraries, which are included automatically through Maven:

- [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework): Plugin, Manager, and Node hierarchy with lifecycle management.
- [Dependency Injector](https://github.com/Trae-Maven/dependency-injector): Container management, classpath scanning, and component wiring.
- [Utilities](https://github.com/Trae-Maven/utilities): Generic type resolution, string utilities, and casting helpers.

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

## Enabling Subsystems

The sidebar, tablist, team, item, and window systems each ship their own manager and listener as framework-owned singletons. They are not active by default: the dependency injector only constructs components in packages it has been told to scan.

Declare the packages you want with `@Scan` on your `@Application` class, or on any interface or superclass in its hierarchy. The `ScanResolver` walks the full type graph of the bootstrap class and collects every `@Scan` it finds, so each layer can declare what it owns.

### Enabling One Subsystem

```java
@Application
@Scan("io.github.trae.spigot.framework.window")
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

### Enabling Several

```java
@Application
@Scan({
        "io.github.trae.spigot.framework.item",
        "io.github.trae.spigot.framework.window",
        "io.github.trae.spigot.framework.sidebar"
})
public class CorePlugin extends SpigotPlugin {
}
```

### Enabling Everything

Scanning the framework root picks up every subsystem, since a base package is treated as a prefix and all subpackages are included:

```java
@Application
@Scan("io.github.trae.spigot.framework")
public class CorePlugin extends SpigotPlugin {
}
```

### Available Packages

| Package | Provides |
|---|---|
| `io.github.trae.spigot.framework.item` | `ItemManager`, `ItemApplyListener`, `ItemActivateListener` |
| `io.github.trae.spigot.framework.window` | `WindowManager`, `WindowListener` |
| `io.github.trae.spigot.framework.sidebar` | `SidebarManager`, `SidebarListener` |
| `io.github.trae.spigot.framework.tablist` | `TablistManager`, `TablistListener` |
| `io.github.trae.spigot.framework.team` | `TeamManager`, `TeamListener` |
| `io.github.trae.spigot.framework.blocking` | `SwordBlockListener` |

Your own `@Application` class's package is always scanned, so the sidebars, items, windows, and teams you define alongside it are discovered without any extra declaration. `@Scan` is only for pulling in packages you do not own.

Components discovered through `@Scan` are system-scoped: they are registered once by the first application whose hierarchy resolves the package, shared across every application after that, and torn down only when the last application shuts down. Two plugins scanning the same package therefore share one manager instance rather than each getting their own.

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

Extend `BaseCommand` with the appropriate sender type. The second type parameter names the owning Manager, which the command resolves through `getParent()`. Permission is passed via the constructor:

```java
@Singleton
public class AccountCommand extends BaseCommand<CorePlugin, AccountManager, CommandSender> {

    public AccountCommand() {
        super("account", "Account management", List.of("acc", "client"), "core.commands.account");
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        sender.sendMessage("Account command executed!");
    }

    @Override
    public List<String> getTabComplete(final CommandSender sender, final String[] args) {
        return Collections.emptyList();
    }
}
```

### Defining a SubCommand

The second type parameter names the parent command. Subcommands are attached to that parent automatically as each component is initialized:

```java
@Singleton
public class AdminSubCommand extends BaseSubCommand<CorePlugin, AccountCommand, Player> {

    public AdminSubCommand() {
        super("admin", "Toggle Admin Mode", Collections.emptyList(), "core.commands.account.admin");
    }

    @Override
    public void execute(final Player player, final String[] args) {
        this.getParent().getParent().getAccountByPlayer(player).ifPresent(account -> {
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
    public List<String> getTabComplete(final Player player, final String[] args) {
        return Collections.emptyList();
    }
}
```

This registers `/account admin` automatically: the parent `AccountCommand` routes the `admin` argument to `AdminSubCommand` with the remaining args.

### Command Execution Flow

```
/account admin
  │
  ├─ Sender type validation (Player)
  ├─ Permission check (core.commands.account.admin)
  ├─ CommandExecuteEvent (cancellable)
  └─ AdminSubCommand.execute(player, new String[0])
```

### Event Dispatch

Use `UtilEvent` for thread-safe event dispatch:

```java
// Synchronous, fire and inspect
MyEvent event = UtilEvent.supply(new MyEvent());
if (event.isCancelled()) {
        return;
        }

// Asynchronous, fire and forget
        UtilEvent.dispatchAsynchronous(new MyAsyncEvent());

// Asynchronous, fire and chain
        UtilEvent.supplyAsynchronous(new MyAsyncEvent()).thenAccept(event -> System.out.println("Done: " + event.isCancelled()));
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
UtilMessage.message(playerList, "Punish", "<yellow>%s</yellow> has banned <yellow>%s</yellow> for <light_purple>%s</light_purple>.".formatted(sender.getName(), target.getName(), duration), player -> player.isOp(), Collections.singletonList(target.getUniqueId()));

// Broadcast to all online players
UtilMessage.broadcast("Server", "<red><bold>Restarting</bold></red> in <yellow>5 minutes</yellow>.");

// Broadcast with ignore list
UtilMessage.broadcast("Alert", "<red>PvP is now enabled!</red>", List.of(excludedPlayerUUID));

// Log to console
UtilMessage.log("Core", "Plugin loaded successfully!");
```

---

## Item System

The framework provides a declarative item system. An item describes what a stack should look like, and the framework turns that description into an `ItemStack` and keeps existing stacks in line with it.

`Item` is the plain description, producing stacks with no identity, suited to transient things such as window icons. `CustomItem` stamps an identifier and a version hash onto every stack it produces, so the stack can be recognised later and brought back in line when the definition changes. `ActivatableCustomItem` and its subclasses add click behaviour on top.

A stack the framework does not recognise still passes through, under a `DefaultItem` for its material. Nothing is written to it, but the update events fire, so a listener can apply something uniformly across every stack rather than only the custom ones.

Requires `@Scan("io.github.trae.spigot.framework.item")`.

### Defining a Plain Item

Extend `Item` when the stack is throwaway and never needs to be recognised again:

```java
public class BackIcon extends Item {

    public BackIcon() {
        super(Material.RED_WOOL);
    }

    @Override
    public Color getColor() {
        return ChatColor.RED.getColor();
    }

    @Override
    public String getDisplayName() {
        return "Back";
    }

    @Override
    public List<String> getLore() {
        return List.of("Return to the previous window.");
    }
}
```

```java
final ItemStack itemStack = new BackIcon().create();
```

### Defining a Custom Item

Extend `CustomItem` and register it as a component. `ItemManager` discovers every subclass through the dependency injector on first use and registers it under its identifier:

```java
@Singleton
public class MinersPickaxe extends CustomItem {

    public MinersPickaxe() {
        super(Material.IRON_PICKAXE, "2f9c1e04-7a13-4f60-9d2b-5c81ab3e7f10", "MINERS_PICKAXE");
    }

    @Override
    public String getDisplayName() {
        return "Miner's Pickaxe";
    }

    @Override
    public List<String> getLore() {
        return List.of(
                "Mines a little faster than it should.",
                "",
                "Right-Click to toggle vein mining."
        );
    }

    @Override
    public NamespacedKey getModel() {
        return new NamespacedKey("custom", "miners_pickaxe");
    }
}
```

### Identifier and Namespace

The two arguments after the material do different jobs, and only one of them is ever written to a stack.

| Value | Purpose |
|---|---|
| Identifier | Opaque and permanent. Stamped onto every stack, so the item can be renamed, restyled or moved between packages without orphaning stacks already in circulation. |
| Namespace | The readable key. Never stamped, used for commands, configuration and search. |

A UUID makes a good identifier precisely because it means nothing: there is no temptation to change it when the item's name changes. Renaming the namespace costs nothing, since no stack refers to it.

### Creating Stacks

```java
// Single undamaged stack
final ItemStack single = minersPickaxe.create();

// Five of them
final ItemStack five = minersPickaxe.create(5);

// With a damage value
final ItemStack damaged = minersPickaxe.create(1, 250);

// Taking amount and durability from an existing stack
final ItemStack converted = minersPickaxe.create(existingItemStack);

// A display-only stack carrying no identity, for a menu icon
final ItemStack icon = minersPickaxe.createView();
```

`createView` skips the identity stamp, so the stack has no identifier and no version and `ItemManager` will never recognise it. Use it anywhere a player looks at an item rather than owns it: a stack from `create` is indistinguishable from a real item were it ever to escape into an inventory, where a view stack plainly is not one.

### Typed Meta

Override `editMeta(ItemMeta)` for anything the declarative description does not cover. Cast the meta to the type the material actually produces and set what you need:

```java
@Override
protected void editMeta(final ItemMeta itemMeta) {
    if (itemMeta instanceof final LeatherArmorMeta leatherArmorMeta) {
        leatherArmorMeta.setColor(org.bukkit.Color.fromRGB(0x228B22));
    }
}
```

It runs after the display options, so an option set here overrides the equivalent one, and an item writing a display name in both places keeps the one written here.

> **Note:** `editMeta` exists separately from `stamp(ItemMeta)` because `CustomItem` marks that method final to write its identifier and version, leaving subclasses no other way to reach the meta. Anything set here is also invisible to the version hash, so fold the state behind it into `generateVersionEntries()` if existing stacks should be reconciled when it changes.

### Styles

An `ItemStyle` carries a colour, a tooltip style key and a tag glyph, so an item makes one decision rather than three:

```java
@Override
protected ItemStyle getStyle() {
    return ItemQuality.LEGENDARY;
}
```

The style supplies the display name colour, sets the tooltip frame, and appends its tag beneath the item's lore separated by a blank line. An item declaring one writes no colour, no tooltip style and no tag line of its own.

The framework attaches no meaning to a style beyond those three values. Grouping them into rarities, tiers or anything else is a decision for the plugin that defines them:

```java
@UtilityClass
public class ItemQuality {

    public static final ItemStyle LEGENDARY = ItemStyle.of(
            "Legendary", // Name
            ChatColor.GOLD.getColor(), // Color
            new NamespacedKey("custom", "legendary"), // Tooltip Style
            "\uE005" // Tag
    );
}
```

### Naturally Obtainable Items

An item declaring `naturallyObtainable()` is registered under its material as well as its identifier. Any vanilla stack of that material a player mines, crafts, smelts, or picks up is converted into the custom item automatically:

```java
@Singleton
public class RawIron extends CustomItem {

    public RawIron() {
        super(Material.RAW_IRON, "raw_iron");
    }

    @Override
    public Color getColor() {
        return ChatColor.WHITE.getColor();
    }

    @Override
    public String getDisplayName() {
        return "Raw Iron";
    }

    @Override
    public List<String> getLore() {
        return List.of("Smelt in a furnace to refine.");
    }

    @Override
    protected boolean naturallyObtainable() {
        return true;
    }
}
```

Only one item may claim a given material. Registering two throws at server load.

### Activatable Items

Extend `ActivatableCustomItem` for an item that does something when clicked. `ItemActivateListener` resolves the item behind the clicked stack and calls `onActivate` once the click has survived the cancellable `ItemPreActivateEvent` and passed the item's own `canActivate`:

```java
@Singleton
public class MinersPickaxe extends SingleActivatableCustomItem {

    public MinersPickaxe() {
        super(Material.IRON_PICKAXE, "2f9c1e04-7a13-4f60-9d2b-5c81ab3e7f10", "MINERS_PICKAXE", ActivateType.RIGHT_CLICK);
    }

    @Override
    public String getDisplayName() {
        return "Miner's Pickaxe";
    }

    @Override
    public List<String> getLore() {
        return List.of("Mines a little faster than it should.");
    }

    @Override
    public void onActivate(final Player player, final ItemStack itemStack) {
        UtilMessage.message(player, "Items", "Vein mining <green>enabled</green>.");
    }
}
```

An item extending `CustomItem` directly is never invoked, so the capability is opt-in per item rather than a hook every custom item overrides.

### Single-Click Items

Most items respond to one kind of click and branch on nothing. `SingleActivatableCustomItem` fixes the click type at construction and drops the `ActivateType` parameter from every hook:

```java
@Singleton
public class WarpStone extends SingleActivatableCustomItem {

    public WarpStone() {
        super(Material.AMETHYST_SHARD, "7e51c3b8-2d94-4a06-b83f-1ac6d095e274", "WARP_STONE", ActivateType.RIGHT_CLICK);
    }

    @Override
    public String getDisplayName() {
        return "Warp Stone";
    }

    @Override
    public List<String> getLore() {
        return List.of("Warm to the touch, and always pointing home.");
    }

    @Override
    public boolean canActivate(final Player player, final ItemStack itemStack) {
        return !player.isInsideVehicle();
    }

    @Override
    public void onActivate(final Player player, final ItemStack itemStack) {
        player.teleport(player.getWorld().getSpawnLocation());
    }

    @Override
    public String getCooldownName() {
        return "Warp";
    }

    @Override
    public long getCooldownDuration() {
        return TimeUnit.MINUTES.toMillis(5);
    }
}
```

The cooldown hooks lose their `ActivateType` parameter too, since the click type is already fixed.

Any other click type is refused before the item's own checks run, so a left click never reaches `onActivate` and the item writes no click-type check. Every parameterised hook is final, so a subclass cannot accidentally override the wrong overload.

### Channelled Items

`ChannelCustomItem` is for an item that does something continuously while a player holds right click, rather than once when they press it:

```java
@Singleton
public class DiviningRod extends ChannelCustomItem {

    public DiviningRod() {
        super(Material.STICK, "0d4f8a21-6b3c-4e79-8f15-c2a70b9e4d33", "DIVINING_ROD");
    }

    @Override
    public String getDisplayName() {
        return "Divining Rod";
    }

    @Override
    public List<String> getLore() {
        return List.of("It only has opinions underground.");
    }

    @Override
    public boolean canChannel(final Player player, final ItemStack itemStack) {
        return player.getLocation().getBlockY() < 40;
    }

    @Override
    public void onStart(final Player player, final ItemStack itemStack) {
        UtilMessage.message(player, "Items", "The rod begins to twitch.");
    }

    @Override
    public void onStop(final Player player, final ItemStack itemStack) {
        UtilMessage.message(player, "Items", "The rod falls still.");
    }

    @Override
    public void onChannel(final Player player, final ItemStack itemStack) {
        player.getWorld().spawnParticle(Particle.WAX_OFF, player.getLocation().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.01);
    }
}
```

The right click starts a channel and a scheduler ticks it from there. `onChannel` runs every tick until the player lets go, swaps items, logs out, an `ItemChannelEvent` is cancelled, or `canChannel` stops returning `true`. Whichever ends it, `onStop` fires exactly once.

Note `canChannel` is checked every tick rather than only at the start, so this rod stops on its own the moment the player climbs above ground.

| Hook | When |
|---|---|
| `onStart` | Once, when the channel begins |
| `onChannel` | Every tick the channel runs |
| `onStop` | Once, however the channel ended |
| `canChannel` | Every tick, before `onChannel` |

Holding right click requires the item to have a use action. Many materials have none, a sword and a stick among them, and the hold never registers for those. See [Sword Blocking](#sword-blocking) for the component that gives one to a sword.

### Activation Types

`ActivateType` groups the vanilla actions that mean the same thing to an item, so an implementation reacts to a left click without caring whether the player was aiming at a block or at air:

| Type | Covers |
|---|---|
| `LEFT_CLICK` | `LEFT_CLICK_AIR`, `LEFT_CLICK_BLOCK` |
| `RIGHT_CLICK` | `RIGHT_CLICK_AIR`, `RIGHT_CLICK_BLOCK` |

Actions with no matching type, such as physical pressure plate triggers, activate nothing. Only the main hand is handled, since the interaction event fires once per hand and an item would otherwise activate twice.

Branch on the type when an item does different things per click:

```java
@Override
public void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    switch (activateType) {
        case LEFT_CLICK -> this.cycleMode(player);
        case RIGHT_CLICK -> this.fire(player, itemStack);
    }
}
```

### Gating an Activation

`canActivate` is the item-level check, evaluated after the pre-activate event, for conditions the item itself owns such as a cooldown or a durability threshold:

```java
@Override
public boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    if (activateType != ActivateType.RIGHT_CLICK) {
        return false;
    }

    if (player.isInWater() || player.isInLava()) {
        UtilMessage.message(player, "Item", "You cannot use <green>%s</green> while in liquid.".formatted(this.getDisplayName()));
        return false;
    }

    return true;
}
```

`ItemPreActivateEvent` is the system-level equivalent, for conditions external to the item such as a region restriction or a global lockdown:

```java
@EventHandler
public void onItemPreActivate(final ItemPreActivateEvent event) {
    if (this.regionManager.isInSafezone(event.getPlayer())) {
        event.setCancelledWithReason("You cannot use items here.");
    }
}
```

`ItemPostActivateEvent` fires after a successful activation, for recording a statistic or logging. It is not cancellable, and never fires for an activation that was refused.

### Cooldowns

An item declares its own cooldown rather than each caller managing one:

```java
@Override
public String getCooldownName(final ActivateType activateType) {
    return this.getDisplayName();
}

@Override
public long getCooldownDuration(final ActivateType activateType) {
    return TimeUnit.SECONDS.toMillis(5);
}
```

The name is the key rather than the item, so two items returning the same name share a cooldown, and one item returning different names per click type gates each independently. `getCooldownName` defaults to the display name and `getCooldownDuration` to zero, meaning no cooldown.

A `SingleActivatableCustomItem` gets the parameterless overloads, since the click type is already fixed.

### Gating a Channel

A channel has the same split, checked every tick rather than once. `canChannel` is the item's own condition and `ItemChannelEvent` is the system-level one:

```java
@EventHandler
public void onItemChannel(final ItemChannelEvent event) {
    if (this.regionManager.isInSafezone(event.getPlayer())) {
        event.setCancelled(true);
    }
}
```

Cancelling ends the channel outright rather than pausing it: `onChannel` does not run for that tick, `onStop` fires, and the player is dropped from the item's active set. The event is evaluated first, so `canChannel` never runs in a context the server has already refused.

### Suppressing Vanilla Behaviour

An activation runs alongside whatever the material and the clicked block would normally do. Return `Event.Result.DENY` from either hook to suppress that:

```java
// Stop the material's own use, such as a right-clickable food item being eaten
@Override
public Event.Result useItemInHand(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    return Event.Result.DENY;
}

// Stop the clicked block responding, such as a chest opening
@Override
public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
    return Event.Result.DENY;
}
```

Both default to `Event.Result.DEFAULT`, leaving vanilla behaviour untouched. The block is passed so the decision can depend on what was clicked, and is `null` when the player clicked air:

```java
@Override
public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
    return block != null && block.getType() == Material.CHEST ? Event.Result.DENY : Event.Result.DEFAULT;
}
```

### Versioning and Reconciliation

Every stack a `CustomItem` produces carries a SHA-256 hash of the item's full description. Change the display name, lore, model, colour, or any other described property, and the hash changes, which marks every stack already in circulation as outdated.

`ItemManager#apply(ItemStack)` reads the stamped identifier, finds the owning item, and replaces the stack when its version no longer matches. Amount and durability are preserved, so a pickaxe a player has been using for weeks keeps its damage while gaining the new lore.

Reconciliation runs automatically at every point a stack enters a player's possession:

| Trigger | Handled By |
|---|---|
| Item pickup | `EntityPickupItemEvent` |
| Crafting result preview and craft | `PrepareItemCraftEvent` |
| Furnace smelt result | `FurnaceSmeltEvent` |
| Player join | `PlayerJoinEvent` |
| Every 30 seconds, all online inventories | Scheduler |

The scheduler covers the remaining case: a stack sitting untouched in an inventory when an item's definition changes at runtime.

### Extending the Version Hash

Override `generateVersionEntries()` to fold subclass state into the hash, so a change to that state also marks existing stacks outdated:

```java
@Override
protected List<String> generateVersionEntries() {
    return Stream.concat(
            super.generateVersionEntries().stream(),
            Stream.of(UtilString.pair("Reward-Amount", Integer.toString(this.rewardConfig.getAmount())))
    ).toList();
}
```

### Applying Manually

```java
// Reconcile a single stack
final ItemStack reconciled = this.itemManager.apply(itemStack);

// Reconcile a whole inventory
this.itemManager.updateInventory(player.getInventory());

// Look an item up by identifier
this.itemManager.getItemByIdentifier(identifier).ifPresent(item -> player.getInventory().addItem(item.create()));

// Resolve one from a name a player typed, matching on namespace
this.itemManager.searchItem(sender, input, true).ifPresent(item -> player.getInventory().addItem(item.create()));
```

`apply` takes one of three routes, and only one of them replaces the stack:

| Stack | Route |
|---|---|
| Known identifier, outdated version | `update`, rewriting the description in place |
| Known identifier, current version | `refresh`, dispatching the events and nothing else |
| No identifier, obtainable material | `create`, building a fresh stack |
| Anything else | `refresh` under its `DefaultItem` |

Only the obtainable route replaces a stack outright, and it does so deliberately: the material is being reinterpreted as a custom item, so enchantments and other data do not carry across. Every other route returns the input by reference, so an identity comparison tells a caller whether the stack was replaced rather than merely altered.

The refresh route exists so a listener still runs against a stack that needed no rewriting. An item's version hash covers its own description and knows nothing about what a listener adds on top, so version-gating alone would leave those stacks permanently missing it.

---

## Window System

The framework provides an inventory window system. A `Window` owns its own inventory and is composed of `Button`s bound to slots. Clicks are dispatched to the button in the clicked slot, and every click and drag on a window inventory is cancelled, so nothing can be moved into or out of one.

Requires `@Scan("io.github.trae.spigot.framework.window")`.

### Defining a Window

Extend `Window`, passing the title and row count, and register buttons in `populate`:

```java
public class ProfileWindow extends Window {

    private final AccountManager accountManager;

    public ProfileWindow(final AccountManager accountManager) {
        super(Component.text("Profile", NamedTextColor.GOLD), 3);

        this.accountManager = accountManager;
    }

    @Override
    protected void populate(final Player player) {
        this.addButton(new StatsButton(this, 11, player));
        this.addButton(new SettingsButton(this, 15));
    }
}
```

### Defining a Button

Extend `Button`, typed with the window it belongs to, and pass that window, the slot, and the stack it renders:

```java
public class SettingsButton extends Button<ProfileWindow> {

    public SettingsButton(final ProfileWindow window, final int slot) {
        super(window, slot, new SettingsIcon().createView());
    }

    @Override
    protected List<String> getLore() {
        return List.of("<green>Click to open settings.");
    }

    @Override
    public boolean canClick(final Player player, final ClickType clickType) {
        return clickType.isLeftClick();
    }

    @Override
    public void onClick(final Player player, final ClickType clickType) {
        UtilWindow.open(player, new SettingsWindow(this.getWindow()));
    }
}
```

The window is typed, so a button reaches its window's own state and methods without a cast: paging, toggling a filter, or triggering a re-render.

`getDisplayName` and `getLore` layer over the base stack rather than replacing it. The base is cloned first, so a button handed a shared stack never mutates it; a display name the button declares overrides the base's own, and lore is appended beneath the base's, separated by a blank line. That lets a button annotate an item with what clicking it does while leaving the item's own description intact.

Use `createView()` rather than `create()` for the base stack. A menu icon has no business carrying an item's identity, and a view stack cannot be mistaken for the real thing were it ever to escape the window.

### Opening a Window

Open through `UtilWindow` rather than `openInventory` directly, so the open event and `canOpen` gate are honoured and the window renders for the player being opened for:

```java
UtilWindow.open(player, new ProfileWindow(this.accountManager));
```

### Scope

A window owns one inventory, created in its constructor and reused for its whole lifetime. Scope therefore follows instance lifetime:

| Usage | Result |
|---|---|
| `new SomeWindow(...)` per open | Private to that player |
| Held as a `@Singleton` or a field | Shared by everyone who opens it |

Constructing per open is the normal case, and is what keeps one player's window contents from being visible to another.

### Render and Refresh

| Method | Effect |
|---|---|
| `render(Player)` | Clears the buttons, re-runs `populate`, and redraws. Use when the button set itself may have changed. |
| `refresh()` | Redraws the existing buttons without re-running `populate`. Use when only their rendered stacks changed. |

Because the inventory is reused, calling either on a window someone is currently viewing updates it in place without closing it. This is what makes paging and toggles work:

```java
@Override
public void onClick(final Player player, final ClickType clickType) {
    this.window.setPage(this.window.getPage() + 1);

    this.window.render(player);
}
```

### Gating and Hooks

| Hook | Effect |
|---|---|
| `canOpen(Player)` | Returning `false` aborts the open, leaving whatever the player has open in place |
| `canClose(Player)` | Returning `false` re-opens the inventory a tick later, holding the player in the window |
| `onOpen(Player)` | Called after the window has been shown |
| `onClose(Player)` | Called after the player closed it and tracking entries were dropped |
| `Button#canClick(Player, ClickType)` | Returning `false` suppresses the button's action |

`canOpen`, `canClose`, and `canClick` are the window-level checks, for conditions the window or button itself owns. `WindowOpenEvent`, `WindowCloseEvent`, and `ButtonPreClickEvent` are the system-level equivalents, for conditions external to it, such as a world restriction or a global lockdown.

A click passes `ButtonPreClickEvent` first, then `canClick`, and fires `ButtonPostClickEvent` once the action has run. The framework throttles nothing itself, so rate-limiting belongs in a `ButtonPreClickEvent` listener.

### Sub-Windows and Back Buttons

A sub-window holds its parent, and a back button opens it again. The parent still owns its inventory, so returning to it is just another open:

```java
public class SettingsWindow extends Window {

    private final Window parentWindow;

    public SettingsWindow(final Window parentWindow) {
        super(Component.text("Settings"), 3);

        this.parentWindow = parentWindow;
    }

    @Override
    protected void populate(final Player player) {
        this.addButton(new BackButton(22, this.parentWindow));
    }
}
```

`BackButton` is provided by the framework in `io.github.trae.spigot.framework.window.types.buttons`, with a default icon or one you supply.

### Querying

```java
// What is this player looking at
this.windowManager.getWindowByPlayer(player).ifPresent(window -> window.refresh());

// Which window owns this inventory
this.windowManager.getWindowByInventory(inventory);
```

Click dispatch never consults these maps. A window is its own `InventoryHolder`, so a click resolves straight off the event and a momentarily stale map can never misroute one.

---

## Sword Blocking

A sword has no use action of its own, so right-clicking one does nothing and `isBlocking` can never become true for it. `SwordBlockListener` attaches the `blocks_attacks` data component to every sword the item system produces, which gives it one.

Requires `@Scan("io.github.trae.spigot.framework.blocking")`.

That matters for two reasons. It is what lets a `ChannelCustomItem` on a sword material register the hold at all, and it drives the `minecraft:using_item` model condition a resource pack needs to swap in a blocking pose.

The component is applied with no damage reductions, so blocking is a pure gesture that reduces nothing, and with a disable cooldown scale of zero, so an axe hit cannot interrupt it. The block delay is one tick rather than the shield's quarter second, so the raise registers immediately.

It hooks `ItemStackUpdateEvent`, which fires on every route through `apply`, so custom swords, vanilla ones passing through their default definition, and swords already current by version are all covered.

> **Note:** the arm animation is the shield's, not 1.8's. A resource pack can change how the sword sits in the hand while blocking, but not how the arm moves.

---

## Sidebar System

The framework provides a packet-based sidebar (scoreboard) system with priority-based resolution. Multiple `Sidebar` subclasses can be registered, and the lowest priority one that passes all display checks is shown. Only changed lines and titles produce packets, eliminating flicker.

Requires `@Scan("io.github.trae.spigot.framework.sidebar")`.

### Defining a Sidebar

Extend `Sidebar`, passing an identifier and a priority, and register it as a component. `SidebarManager` discovers every subclass automatically through the dependency injector:

```java
@Singleton
public class HubSidebar extends Sidebar {

    private final PlayerManager playerManager;

    public HubSidebar(final PlayerManager playerManager) {
        super("HUB", 10);

        this.playerManager = playerManager;
    }

    @Override
    protected Component getTitle(final Player player) {
        return Component.text("MY SERVER", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    @Override
    protected List<Component> getLines(final Player player) {
        final PlayerData data = this.playerManager.getPlayerData(player);

        return List.of(
                Component.text("Online: ", NamedTextColor.GRAY).append(Component.text(Bukkit.getOnlinePlayers().size(), NamedTextColor.WHITE)),
                Component.text("Rank: ", NamedTextColor.GRAY).append(Component.text(data.getRank(), NamedTextColor.GOLD)),
                Component.text("Coins: ", NamedTextColor.GRAY).append(Component.text(data.getCoins(), NamedTextColor.YELLOW))
        );
    }
}
```

> **Note:** the manager's scheduler runs asynchronously, so `getTitle` and `getLines` may be called off the main thread. Build lines from cached or thread-safe state rather than reading live world or entity data inside them.

### Animated Title

Override `isStaticTitle()` to enable per-tick title updates driven by the manager's scheduler:

```java
private int tick = 0;

private static final List<TextColor> COLORS = List.of(
        NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW,
        NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE
);

@Override
protected boolean isStaticTitle() {
    return false;
}

@Override
protected Component getTitle(final Player player) {
    return Component.text("MY SERVER", COLORS.get(this.tick++ % COLORS.size()), TextDecoration.BOLD);
}
```

### Priority Resolution

Lower priority always wins. When the lowest-numbered sidebar becomes ineligible, the next one takes over automatically:

```java
@Singleton
public class FactionsSidebar extends Sidebar {

    private final FactionsManager factionsManager;

    public FactionsSidebar(final FactionsManager factionsManager) {
        super("FACTIONS", 0); // wins over HubSidebar at 10

        this.factionsManager = factionsManager;
    }

    @Override
    protected boolean canDisplay(final Player player) {
        return this.factionsManager.isInFaction(player);
    }

    @Override
    protected Component getTitle(final Player player) {
        return Component.text("FACTIONS", NamedTextColor.RED, TextDecoration.BOLD);
    }

    @Override
    protected List<Component> getLines(final Player player) {
        // faction-specific lines
    }
}
```

`canDisplay()` with no arguments is the global gate, for state independent of any player. Both it and the per-player variant must pass for a sidebar to be eligible.

### Updating a Sidebar

Fire `SidebarUpdateEvent` to trigger a refresh for a player:

```java
// Update whatever sidebar is currently active
UtilEvent.dispatch(new SidebarUpdateEvent(player));

// Only update if the active sidebar matches the given identifier
UtilEvent.dispatch(new SidebarUpdateEvent("HUB", player));
```

Cancelling the event clears the player's sidebar instead of refreshing it.

---

## Tablist System

The tablist system resolves a player's tab list header and footer the same way the sidebar system resolves a sidebar: the lowest priority `Tablist` passing all display checks wins. An asynchronous scheduler re-resolves and re-sends on a fixed interval, so dynamic content stays current without any manual dispatch.

Requires `@Scan("io.github.trae.spigot.framework.tablist")`.

### Defining a Tablist

Extend `Tablist`, passing a priority. There is no identifier, since only one tablist applies at a time and updates are never scoped:

```java
@Singleton
public class HubTablist extends Tablist {

    public HubTablist() {
        super(10);
    }

    @Override
    protected Component getHeader(final Player player) {
        return Component.text("MY SERVER", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    @Override
    protected Component getFooter(final Player player) {
        return Component.text("play.myserver.net", NamedTextColor.GRAY);
    }
}
```

### Priority Resolution

Same rules as the sidebar system: lower wins, and both `canDisplay()` and `canDisplay(Player)` must pass:

```java
@Singleton
public class EventTablist extends Tablist {

    private final EventManager eventManager;

    public EventTablist(final EventManager eventManager) {
        super(0); // wins over HubTablist at 10

        this.eventManager = eventManager;
    }

    @Override
    protected boolean canDisplay() {
        return this.eventManager.isEventRunning();
    }

    @Override
    protected Component getHeader(final Player player) {
        return Component.text("EVENT LIVE", NamedTextColor.RED, TextDecoration.BOLD);
    }

    @Override
    protected Component getFooter(final Player player) {
        return Component.text("Type /event to join", NamedTextColor.YELLOW);
    }
}
```

### Updating a Tablist

The scheduler dispatches `TablistUpdateEvent` for every online player on a fixed interval, so there is normally nothing to fire yourself. Cancelling the event clears that player's tablist, which is how another system suppresses the display for them:

```java
@EventHandler
public void onTablistUpdate(final TablistUpdateEvent event) {
    if (this.settingsManager.hasTablistHidden(event.getPlayer())) {
        event.setCancelled(true);
    }
}
```

The clearing packet is only sent once, on the transition away from an active tablist, rather than every tick.

---

## Team System

The framework provides a packet-based team system for per-viewer prefix and suffix resolution. Each online player has a team entry sent individually to every viewer, allowing relation-aware nametag colours such as faction ally versus enemy.

Requires `@Scan("io.github.trae.spigot.framework.team")`.

### Defining a Team

Extend `Team`, passing an identifier and a priority, and register it as a component. Lower priority wins when multiple are eligible:

```java
@Singleton
public class RankTeam extends Team {

    private final PlayerManager playerManager;

    public RankTeam(final PlayerManager playerManager) {
        super("RANK", 10); // fallback

        this.playerManager = playerManager;
    }

    @Override
    protected Component getPrefix(final Player player, final Player viewer) {
        final String rank = this.playerManager.getPlayerData(player).getRank();

        return Component.text("[" + rank + "] ", NamedTextColor.GOLD);
    }
}
```

```java
@Singleton
public class FactionsTeam extends Team {

    private final FactionsManager factionsManager;

    public FactionsTeam(final FactionsManager factionsManager) {
        super("FACTIONS", 0); // wins over RankTeam

        this.factionsManager = factionsManager;
    }

    @Override
    protected boolean canDisplay(final Player player, final Player viewer) {
        return this.factionsManager.isInFaction(player);
    }

    @Override
    protected Component getPrefix(final Player player, final Player viewer) {
        final FactionRelation relation = this.factionsManager.getRelation(viewer, player);

        return switch (relation) {
            case ALLY -> Component.text("[ALLY] ", NamedTextColor.GREEN);
            case ENEMY -> Component.text("[ENEMY] ", NamedTextColor.RED);
            default -> Component.text("[NEUTRAL] ", NamedTextColor.YELLOW);
        };
    }
}
```

Resolution happens per player and viewer pair, which is why `canDisplay` and every option hook take both. That is what lets the same target player present different decorations to different viewers.

### Available Options

Every option hook returns `null` by default, leaving the underlying vanilla value in place rather than overriding it:

| Hook | Controls |
|---|---|
| `getDisplayName(player, viewer)` | Team display name |
| `getPrefix(player, viewer)` | Nametag prefix |
| `getSuffix(player, viewer)` | Nametag suffix |
| `allowFriendlyFire(player, viewer)` | Friendly fire within the team |
| `seeFriendlyInvisibles(player, viewer)` | Visibility of friendly invisibles |
| `getNameTagVisibility(player, viewer)` | Nametag visibility rule |
| `getDeathMessageVisibility(player, viewer)` | Death message visibility rule |
| `getCollisionRule(player, viewer)` | Collision rule |
| `getColor(player, viewer)` | Team colour, which also sets the nametag name colour |

### Updating a Team

The team system has no scheduler, since resolving every player and viewer pair on a timer is quadratic in online players. Teams refresh on join, on quit, and whenever `TeamUpdateEvent` is fired, so whatever changes a relation should dispatch it:

```java
// Update this player's team for all viewers
UtilEvent.dispatch(new TeamUpdateEvent(player));

// Only apply for viewers whose eligible team matches the given identifier
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
| `UtilTask` | Task scheduling: immediate, synchronous, asynchronous, and repeating with ChronoUnit-to-tick conversion |
| `UtilMessage` | MiniMessage-based messaging with configurable prefixes, broadcasting, filtering, and ignore lists |
| `UtilPlugin` | Plugin lookup, internal by name or class |
| `UtilNms` | NMS packet sending and Adventure-to-vanilla component conversion |
| `UtilItemStack` | Persistent data reads and writes on an `ItemStack` |
| `UtilWindow` | Opening a `Window` for a player, honouring the open event and gate |
| `UtilServer` | Server and online player access |

---

## Base Types

| Type | Extend To |
|---|---|
| `Item` | Describe a stack with no identity, such as a window icon |
| `CustomItem` | Describe a stack that carries an identifier and version, and is reconciled automatically |
| `ActivatableCustomItem` | Add a click action, branching on the click type |
| `SingleActivatableCustomItem` | Add a click action for one fixed click type |
| `ChannelCustomItem` | Add an action that runs every tick while right click is held |
| `Window` | Define an inventory menu composed of buttons |
| `Button` | Define a clickable slot within a window |
| `Sidebar` | Define a priority-sorted scoreboard sidebar |
| `Tablist` | Define a priority-sorted tab list header and footer |
| `Team` | Define a priority-sorted, per-viewer nametag decoration |

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

## Item Events

| Event | Fired When |
|---|---|
| `ItemMetaUpdateEvent` | An item has written its description to a stack's meta, before that meta is applied |
| `ItemStackUpdateEvent` | An item has finished with a stack and its meta has been applied |
| `ItemPreActivateEvent` | A player activated an item, before the item's action runs |
| `ItemPostActivateEvent` | An item's activation has run |
| `ItemChannelEvent` | Every tick a player is channelling an item, before the per-tick action runs |

`ItemPreActivateEvent` and `ItemChannelEvent` are cancellable. Cancelling an activation suppresses it entirely; cancelling a channel tick ends the channel outright, firing `onStop`. The rest are not cancellable, and the post event only fires for an activation that actually ran.

The two update events differ by what they can safely touch. `ItemMetaUpdateEvent` fires while the meta is open, for anything belonging to the meta. `ItemStackUpdateEvent` fires after it has been applied, and is the only place a data component can be set, since applying a meta replaces the stack's whole component set.

---

## Window Events

| Event | Fired When |
|---|---|
| `WindowOpenEvent` | A window is about to be rendered and shown to a player |
| `WindowCloseEvent` | A player closed a window, before its tracking entries are dropped |
| `ButtonPreClickEvent` | A player clicked a button, before the button's action runs |
| `ButtonPostClickEvent` | A button's action has run |

All but the last are cancellable. Cancelling an open aborts it, cancelling a close re-opens the window a tick later, and cancelling a click suppresses the button's action. `ButtonPostClickEvent` only fires for a click that actually ran.

---

## Sidebar Events

| Event | Fired When |
|---|---|
| `SidebarUpdateEvent` | A sidebar update is requested for a player |

---

## Tablist Events

| Event | Fired When |
|---|---|
| `TablistUpdateEvent` | A tablist update is requested for a player, dispatched on a fixed interval |

---

## Team Events

| Event | Fired When |
|---|---|
| `TeamUpdateEvent` | A team prefix and suffix update is requested for a player |

---

## Interfaces

| Interface | Description |
|---|---|
| `SpigotPlugin` | Root plugin with automatic Bukkit registration callbacks |
| `Node` | Typed parent access for commands and subcommands (provided by Hierarchy-Framework) |
| `SharedBaseCommand` | Shared contract between commands and subcommands: sender validation, permission, execution, and tab-complete |
| `IBaseCommand` | Command contract with subcommand management |
| `Activatable` | Capability a `CustomItem` implements to gain a click action |
| `ICustomCancellableEvent` | Cancellable event with reason support |
