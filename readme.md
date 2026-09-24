# Spigot-Plugin-Framework

A Spigot/Paper plugin framework providing structured command systems, event utilities, a staged damage and death pipeline, channel-based chat, packet-based sidebars, tablists, teams and holograms, real-entity NPCs, map-based pictures, configurable resource packs, a custom item system, an inventory window system, and lifecycle integration built on the [Hierarchy-Framework](https://github.com/Trae-Maven/hierarchy-framework).

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
- Packet-based hologram system built on text displays rather than armour stands, with per-player text, per-player visibility, and no entity in the world
- Hologram backgrounds drawn from a bitmap font glyph, for a textured or shaped panel behind the text
- Map-based picture system that renders a PNG or JPG across a grid of item frames, reusing the same maps across restarts and redrawing them only when the image changes
- Real-entity NPC system with typed backing entities, opt-in interaction and damage, respawn gating after death, and no duplicates across restarts or chunk reloads
- Resource pack system driven by a JSON list of packs, each with its own URL, hash, requirement, worlds and permission, sent in one request and swapped per pack on world change
- Staged damage pipeline that replaces vanilla damage entirely, with a gate stage, an ability stage and a reduction stage, each cancellable
- Damage modifiers filed under named keys so a weapon's damage, a critical multiplier and an armour reduction all compose instead of overwriting one another
- Vanilla-accurate defaults for armour, toughness, protection, resistance, strength, weakness, weapon damage, critical hits, knockback, durability and immunity windows, each replaceable per piece or per item through its own event
- Every combat figure tunable in one `Damage.json`, from critical multipliers and knockback to the armour formula, applied on the next hit after a reload
- Vanilla 1.21 combat by default, with melee damage scaled by attack charge and a shared PvP immunity window, and a config toggle for pre-1.9 combat, where the attack cooldown is removed and immunity is tracked per attacker
- Configurable immunity windows per damage cause, where poison, wither, burning, drowning, freezing and starvation hit at exactly their configured rate rather than vanilla's hardcoded one
- Death system that knows who killed whom and with what, including attributions that outlive the hit that set them, and works with or without the damage pipeline
- Death drops, experience and death sound rewritable per death, with resource pack sounds played in place of vanilla's
- Hit sounds carried on the damage pass, seeded from the entity being struck and replaceable by an ability
- Totem of undying and any other death protection item honoured under custom damage
- Channel-based chat that replaces vanilla chat, with the channel resolved from plugin-owned state on every message, and cancellable send and per-recipient receive events
- Three-part display names, so a consumer takes just the name where a prefix would be noise and the full thing where it would not
- Declarative item system with identity stamping and automatic version reconciliation, so stacks in player inventories and containers update themselves when the definition changes
- Orphaned stacks, whose item is no longer registered, reset to a plain stack or deleted outright per item
- Custom items protected from anvils and enchanting tables, as the item or as the reagent
- Opt-in item activation, so a custom item gains a click action with its own gate, cancellable events, and control over the vanilla behaviour it replaces
- Vanilla-aware activation defaults, so a chest still opens, a hoe still tills, and a consumable is not spent when its item activates
- Sound abstraction held by key rather than enum, so vanilla and resource pack sounds are played the same way
- Inventory window system with slot-bound buttons, open and close gating, and full click and drag protection
- Timed effect system for any living entity, with per-entity amplifier and duration, cancellable add, update and remove stages, and per-tick and expiry hooks
- Effects optionally bound to a vanilla potion effect, kept in sync across every transition, so a custom effect carries its vanilla presentation without the plugin tracking two things
- Duration changes that credit time already served, so refreshing an effect extends what is left instead of discarding it or granting the whole duration again
- NMS utilities for direct packet sending and Adventure-to-vanilla component conversion
- Custom event base classes with cancellation reasons
- Opt-in subsystems through `@Scan`, so a plugin enables only the packages it wants
- Compatible with Bukkit, Spigot, and Paper
- Designed for modern Java (Java 21+)

---

## Hierarchy

```text
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

The damage, death, chat, effect, sidebar, tablist, team, hologram, picture, NPC, resource pack, item, and window systems sit outside this hierarchy. Their managers and listeners are framework-owned singletons, discovered through `@Scan` rather than declared per plugin. See [Enabling Subsystems](#enabling-subsystems).

---

## Requirements

Spigot-Plugin-Framework requires Java 21+ and a Paper API environment.

### NMS Access (paper-nms-maven-plugin)

The sidebar, team, hologram and picture systems and `UtilNms` use NMS (net.minecraft.server) classes directly. To compile against NMS with Maven, the framework uses the [paper-nms-maven-plugin](https://github.com/Alvinn8/paper-nms-maven-plugin).

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

The damage, death, chat, effect, sidebar, tablist, team, hologram, picture, NPC, resource pack, item, and window systems each ship their own managers and listeners as framework-owned singletons. They are not active by default: the dependency injector only constructs components in packages it has been told to scan.

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
| `io.github.trae.spigot.framework.item` | `ItemManager`, `ItemApplyListener`, `ItemActivateListener`, `ItemPreventionListener` |
| `io.github.trae.spigot.framework.window` | `WindowManager`, `WindowListener` |
| `io.github.trae.spigot.framework.sidebar` | `SidebarManager`, `SidebarListener` |
| `io.github.trae.spigot.framework.tablist` | `TablistManager`, `TablistListener` |
| `io.github.trae.spigot.framework.team` | `TeamManager`, `TeamListener` |
| `io.github.trae.spigot.framework.hologram` | `HologramManager`, `HologramListener` |
| `io.github.trae.spigot.framework.picture` | `PictureManager`, `PictureListener` |
| `io.github.trae.spigot.framework.npc` | `NpcManager`, `NpcInteractListener`, `NpcDamageListener`, `NpcDeathListener`, `NpcStaleListener` |
| `io.github.trae.spigot.framework.resourcepack` | `ResourcePackManager`, `ResourcePackListener` |
| `io.github.trae.spigot.framework.damage` | `DamageManager`, `DamageListener`, `CustomDamageListener`, and the reduction, durability, knockback, critical, potion effect, delay, interval and attack-speed listeners |
| `io.github.trae.spigot.framework.death` | `DeathListener`, `DeathMessageListener` |
| `io.github.trae.spigot.framework.chat` | `ChatManager`, `PreChatListener`, `CustomChatListener` |
| `io.github.trae.spigot.framework.effect` | `EffectManager`, `EffectListener` |
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

```text
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
    public String getName() {
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
    public String getName() {
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

An `ItemStyle` carries a colour, a set of decorations, a tooltip style key and a tag glyph, so an item makes one decision rather than four:

```java
@Override
protected ItemStyle getStyle() {
    return ItemQuality.LEGENDARY;
}
```

The style supplies the display name colour and decorations, sets the tooltip frame, and appends its tag beneath the item's lore, separated by a blank line when there is lore above it. An item declaring one writes no colour, no decorations, no tooltip style and no tag line of its own.

The framework attaches no meaning to a style beyond those values. Grouping them into rarities, tiers or anything else is a decision for the plugin that defines them:

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
        super(Material.RAW_IRON, "9b3e6f12-4c85-4d27-a1f0-5e8d2c7b6a49", "RAW_IRON");
    }

    @Override
    public Color getColor() {
        return ChatColor.WHITE.getColor();
    }

    @Override
    public String getName() {
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

Only one item may claim a given material. If two do, only one of them is registered under it, and which one is not defined, so declare it on a single item.

### Activatable Items

Extend `ActivatableCustomItem` for an item that does something when clicked. `ItemActivateListener` resolves the item behind the clicked stack and calls `onActivate` once the click has survived the cancellable `ItemPreActivateEvent` and passed the item's own `canActivate`:

```java
@Singleton
public class MinersPickaxe extends ActivatableCustomItem {

    public MinersPickaxe() {
        super(Material.IRON_PICKAXE, "2f9c1e04-7a13-4f60-9d2b-5c81ab3e7f10", "MINERS_PICKAXE");
    }

    @Override
    public String getName() {
        return "Miner's Pickaxe";
    }

    @Override
    public List<String> getLore() {
        return List.of("Mines a little faster than it should.");
    }

    @Override
    public void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        UtilMessage.message(player, "Items", "Vein mining <green>enabled</green>.");
    }
}
```

An item extending `CustomItem` directly is never invoked, so the capability is opt-in per item rather than a hook every custom item overrides.

### Single-Click Items

Most items respond to one kind of click and branch on nothing. `SingleActivatableCustomItem` fixes the click type at construction and drops the `ActivateType` parameter from every hook that has an overload without it:

```java
@Singleton
public class WarpStone extends SingleActivatableCustomItem {

    public WarpStone() {
        super(Material.AMETHYST_SHARD, "7e51c3b8-2d94-4a06-b83f-1ac6d095e274", "WARP_STONE", ActivateType.RIGHT_CLICK);
    }

    @Override
    public String getName() {
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

Any other click type is refused before the item's own checks run, so a left click never reaches `onActivate` and the item writes no click-type check. Every hook with a click-type overload is final, so a subclass cannot accidentally override the wrong one. `activateOnItemUse` and `activateOnBlockUse` have no such overload and still take the click type.

### Channelled Items

`ChannelCustomItem` is for an item that does something continuously while a player holds right click, rather than once when they press it:

```java
@Singleton
public class DiviningRod extends ChannelCustomItem {

    public DiviningRod() {
        super(Material.BRUSH, "0d4f8a21-6b3c-4e79-8f15-c2a70b9e4d33", "DIVINING_ROD");
    }

    @Override
    public String getName() {
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

The right click starts a channel and a scheduler ticks it from there. `onChannel` runs every tick until the player lets go, swaps items, logs out, an `ItemChannelEvent` is cancelled, or `canChannel` stops returning `true`. Whichever ends it, `onStop` fires exactly once, except for a logout, where there is no player left to act on.

Note `canChannel` is checked every tick rather than only at the start, so this rod stops on its own the moment the player climbs above ground.

| Hook | When |
|---|---|
| `onStart` | Once, when the channel begins |
| `onChannel` | Every tick the channel runs |
| `onStop` | Once, however the channel ended, except a logout |
| `canChannel` | Every tick, before `onChannel` |

Holding right click requires the item to have a use action. Many materials have none, a sword and a stick among them, which is why the rod above is built on a brush, and the hold never registers for those. See [Sword Blocking](#sword-blocking) for the component that gives one to a sword.

### Activation Types

`ActivateType` groups the vanilla actions that mean the same thing to an item, so an implementation reacts to a left click without caring whether the player was aiming at a block or at air:

| Type | Covers |
|---|---|
| `LEFT_CLICK` | `LEFT_CLICK_AIR`, `LEFT_CLICK_BLOCK` |
| `RIGHT_CLICK` | `RIGHT_CLICK_AIR`, `RIGHT_CLICK_BLOCK` |

Actions with no matching type, such as physical pressure plate triggers, activate nothing. Only the main hand is handled, since the interaction event fires once per hand and an item would otherwise activate twice.

The client sends an arm swing in two places where no left click was meant: after a right click the server denied, and when a stack is dropped. The server reads both as `LEFT_CLICK_AIR`. A left click landing in the same or the next tick as a right click, or as a drop of a stack the framework recognises, is discarded, so a right click ability does not fire its left click twin and a dropped item does not activate on its way out.

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

`canActivate` is the item-level check, evaluated after the pre-activate event, for conditions the item itself owns such as a resource or a durability threshold. Cooldowns have their own hooks, covered below:

```java
@Override
public boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    if (activateType != ActivateType.RIGHT_CLICK) {
        return false;
    }

    if (player.isInWater() || player.isInLava()) {
        UtilMessage.message(player, "Item", "You cannot use <green>%s</green> while in liquid.".formatted(this.getName()));
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
    return this.getName();
}

@Override
public long getCooldownDuration(final ActivateType activateType) {
    return TimeUnit.SECONDS.toMillis(5);
}
```

The name is the key rather than the item, so two items returning the same name share a cooldown, and one item returning different names per click type gates each independently. `getCooldownName` defaults to the item's raw `getName()` and `getCooldownDuration` to zero, meaning no cooldown.

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

### Competing With Vanilla

A right click can mean something to the world as well as to the item. Two defaults decide who wins, and both are chosen so a custom item behaves the way a player expects without writing anything:

| Situation | Default | Example |
|---|---|---|
| The material has a use of its own | The item activates, and the material's use is denied | A custom ender pearl fires its ability and is not thrown |
| The clicked block responds on its own | The block wins, and the item does not activate | A chest opens, a lever toggles, a hoe tills dirt |
| Neither | The item activates | A custom sword clicked at air or stone |

A block counts as responding when it reacts to any click, such as a chest or a door, or when it reacts to the item being held, such as dirt under a hoe. A sneaking player is exempt from the block check, since vanilla skips the block's response when sneaking with a full hand. Left clicks compete with nothing and are never gated here.

Each side has a hook to flip its default:

```java
// Let the material win, so a custom golden apple is eaten rather than activated
@Override
public boolean activateOnItemUse(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    return false;
}

// Activate even when the clicked block would respond, such as a wand used on a chest
@Override
public boolean activateOnBlockUse(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
    return true;
}
```

`activateOnItemUse` defaults to `true` and `activateOnBlockUse` to `false`. The material and block classification behind both lives in `UtilMaterial`.

### Suppressing Vanilla Behaviour

Once an activation is going ahead, two more hooks decide what vanilla still does alongside it. Return `Event.Result.DENY` from either to suppress that:

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

`useItemInHand` denies a right click on a material that has a use of its own, which is what keeps the pearl in hand, and returns `Event.Result.DEFAULT` otherwise. Return `DEFAULT` from it for an item whose material should still be used, such as a pearl that fires an ability and is thrown as well. `useInteractedBlock` defaults to `Event.Result.DEFAULT`, leaving the block untouched, and is only consulted when a block was clicked. The block is passed so the decision can depend on what was clicked:

```java
@Override
public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final Block block, final ActivateType activateType) {
    return block.getType() == Material.CHEST ? Event.Result.DENY : Event.Result.DEFAULT;
}
```

### Anvils and Enchanting Tables

A custom item's tooltip is authored rather than generated, so an enchantment or a player-set name would not survive the next reconciliation. `ItemPreventionListener` refuses both outright:

| Station | Behaviour |
|---|---|
| Anvil | The output is cleared when either input slot holds a custom item, covering renaming, repairing and combining alike |
| Enchanting table | No offers are shown, and the enchantment itself is refused, when the item or the lapis slot holds a custom item |

The enchanting check runs twice on purpose. An offer suppressed at preparation can be restored by another plugin, and the slot can change between the offer being shown and the button being pressed, so the same check is repeated at the point the enchantment would be applied.

A custom item is refused as a reagent too, not only as the thing being worked on, so a custom item built on lapis or on an enchanted book is never consumed by a station.

### Versioning and Reconciliation

Every stack a `CustomItem` produces carries a SHA-256 hash of the item's full description. Change the display name, lore, model, colour, or any other described property, and the hash changes, which marks every stack already in circulation as outdated.

`ItemManager#apply(ItemStack)` reads the stamped identifier, finds the owning item, and updates the stack when its version no longer matches. Amount and durability are preserved, so a pickaxe a player has been using for weeks keeps its damage while gaining the new lore.

Reconciliation runs automatically at every point a stack enters a player's possession:

| Trigger | Handled By |
|---|---|
| Item pickup | `EntityPickupItemEvent` |
| Crafting result preview and craft | `PrepareItemCraftEvent` |
| Furnace smelt result | `FurnaceSmeltEvent` |
| Player join | `PlayerJoinEvent` |
| Opening a chest, double chest, barrel, shulker or other block-backed container | `InventoryOpenEvent` |
| Every 30 seconds, all online inventories | Scheduler |

The scheduler covers the remaining case: a stack sitting untouched in a player's inventory when an item's definition changes at runtime. Containers are reconciled as they are opened rather than on a timer, and crafting grids, anvils and the framework's own windows are left out, since their contents are transient or managed elsewhere.

### Removed Items

A stack still carrying an identifier or version from an item that is no longer registered is an orphan. By default it is rebuilt as a plain stack of its material, with the stale data stripped. An item built on a material that is meaningless without it can ask for its stacks to be deleted instead:

```java
@Override
protected boolean deleteIfRemoved() {
    return true;
}
```

The answer is stamped onto every stack rather than read from the item at the time, since by then there is no item left to ask. It is part of the version hash, so flipping it marks existing stacks outdated and they pick up the new answer on their next update, and the mark is cleared rather than left behind when an item stops asking for it.

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

`apply` takes one of these routes:

| Stack | Route |
|---|---|
| Known identifier, outdated version | `update`, rewriting the description in place, or rebuilding the stack if the item's material changed |
| Known identifier, current version | `refresh`, dispatching the events and nothing else |
| No identifier, obtainable material | `create`, building a fresh stack |
| Orphan marked deletable | `null`, removing the stack |
| Orphan otherwise | `create` under its `DefaultItem`, building a clean stack |
| Anything else | `refresh` under its `DefaultItem` |

Three routes replace a stack outright. The update route does so only when the item's material has changed, since the stack has to be rebuilt as the new material. The obtainable route reinterprets the material as a custom item, and the orphan route strips data that belongs to an item that no longer exists, so enchantments and other data do not carry across either of those two. Either an identifier or a version alone is enough to count as an orphan, since a half-stamped stack is as stale as a fully stamped one.

A `null` return means the stack should be removed, and every caller carries that out in whatever way its own context allows. `updateInventory` clears the slot. Every other route returns the input by reference, so an identity comparison tells a caller whether the stack was replaced rather than merely altered.

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

`getName` and `getLore` layer over the base stack rather than replacing it. The base is cloned first, so a button handed a shared stack never mutates it; a display name the button declares overrides the base's own, and lore is appended beneath the base's, separated by a blank line. That lets a button annotate an item with what clicking it does while leaving the item's own description intact.

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
this.windowManager.getWindowByInventory(inventory).ifPresent(window -> window.refresh());
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

### Cost

The scheduler runs four times a second, but nothing is sent unless the diff finds a change, so a static sidebar costs resolution time and no bandwidth at all. What the interval does cost is a `getTitle` and `getLines` call per player per pass, so keep those cheap and built from cached state rather than live lookups.

The registered sidebars are sorted by priority once on first use rather than on every lookup, so resolving a player's sidebar is a walk of an already-ordered list.

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

### Cost

Unlike the sidebar, nothing is cached to diff against, so every dispatch is a send. That is why the interval is a second rather than a tick: at a high player count, a faster interval is bandwidth spent re-sending content that has not changed.

The registered tablists are sorted by priority once on first use, same as the sidebar.

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

### Cost

Resolution is per player and viewer pair, so a full refresh at a hundred players is ten thousand lookups. The registered teams are therefore sorted by priority once on first use rather than scanned and sorted per lookup, which is the difference between a noticeable stall and nothing.

That same quadratic shape is why firing `TeamUpdateEvent` in a loop is worth avoiding. One player's relation change is one dispatch, not one per viewer.

---

## Hologram System

The framework provides a packet-based hologram system built on text displays. A `Hologram` describes where it sits and what it says, and the framework sends it to the players who should see it.

Nothing exists server-side. The backing entity is constructed but never added to a level, so the server never ticks it, no chunk persists it, and no other plugin can see it. That is what makes the text per-player: two players standing in the same place can be sent different lines from the same hologram, and a hologram can be shown to one of them and not the other.

Requires `@Scan("io.github.trae.spigot.framework.hologram")`.

### Defining a Hologram

Extend `Hologram`, passing a name, and register it as a component. `HologramManager` collects every subclass through the dependency injector, so a hologram never registers itself:

```java
@Singleton
public class SpawnHologram extends Hologram {

    private final SpawnConfig spawnConfig;

    public SpawnHologram(final SpawnConfig spawnConfig) {
        super("SPAWN");

        this.spawnConfig = spawnConfig;
    }

    @Override
    public Location getLocation() {
        return this.spawnConfig.getHologramLocation();
    }

    @Override
    protected List<String> getLines(final Player player) {
        return List.of(
                "<gold><bold>WELCOME</bold></gold>",
                "<gray>Right-Click the villager to begin.</gray>"
        );
    }
}
```

Lines are MiniMessage strings, deserialised and joined with newlines, which is how a single text display renders more than one line.

### Per-Player Text

`getLines` takes the player it is resolving for, so the text can name them, reflect their rank, or read their own progress. Declare `isDynamic()` when the text changes on its own and should be re-sent while they watch:

```java
@Singleton
public class StatsHologram extends Hologram {

    private final PlayerManager playerManager;

    public StatsHologram(final PlayerManager playerManager) {
        super("STATS");

        this.playerManager = playerManager;
    }

    @Override
    public Location getLocation() {
        return new Location(Bukkit.getWorld("world"), 0.5, 65.0, 0.5);
    }

    @Override
    protected List<String> getLines(final Player player) {
        final PlayerData data = this.playerManager.getPlayerData(player);

        return List.of(
                "<yellow>%s</yellow>".formatted(player.getName()),
                "<gray>Kills: <white>%s</white></gray>".formatted(data.getKills()),
                "<gray>Coins: <white>%s</white></gray>".formatted(data.getCoins())
        );
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
```

Leave `isDynamic()` false unless the text really does change. A dynamic hologram resolves its lines, deserialises them, and sends a metadata packet for every viewer on every pass, where a static one does that once at spawn.

### Per-Player Visibility

`canSee` is the hologram's own rule, checked after the world and distance tests pass:

```java
@Override
public boolean canSee(final Player player) {
    return this.questManager.hasStarted(player);
}
```

`HologramSpawnEvent` is the system-level equivalent, for conditions external to the hologram such as a global lockdown or another plugin hiding it. Both must pass.

Cancelling leaves the player out of the viewer set, so the next pass tries again. A listener that wants a hologram hidden keeps cancelling for as long as that holds, rather than cancelling once:

```java
@EventHandler
public void onHologramSpawn(final HologramSpawnEvent event) {
    if (this.settingsManager.hasHologramsHidden(event.getPlayer())) {
        event.setCancelled(true);
    }
}
```

### Facing and Appearance

Every appearance hook has a default, so a hologram overrides only what it cares about:

| Hook | Controls | Default |
|---|---|---|
| `getBillboard()` | How the display rotates to face viewers | `CENTER` |
| `getAlignment()` | How multiple lines align against each other | `CENTER` |
| `getBackgroundColor()` | The ARGB background behind the text | Fully transparent |
| `getBackground()` | A bitmap font glyph drawn behind the text, as MiniMessage | `null` |
| `getScale()` | Uniform scale | `1.0F` |
| `getTextOpacity()` | Text opacity, `-1` for fully opaque | `-1` |
| `getLineWidth()` | Pixel width at which text wraps | `200` |
| `isSeeThrough()` | Whether the text renders through blocks | `false` |
| `isShadowed()` | Whether the text is drawn with a shadow | `false` |
| `getViewDistance()` | How far a player may be and still be sent it | `48.0D` |

`Billboard.FIXED` is the one worth knowing. It does not rotate at all, facing whatever direction the location's yaw and pitch specify, which is what gives a hologram mounted flat against a wall or laid across the floor:

```java
@Override
protected Billboard getBillboard() {
    return Billboard.FIXED;
}

@Override
protected Color getBackgroundColor() {
    return Color.fromARGB(160, 0, 0, 0);
}
```

`Billboard.VERTICAL` turns to face the player but stays upright, and `Billboard.CENTER` pivots on both axes and tilts with their pitch.

The vanilla grey box is always disabled, so `getBackgroundColor` is the only flat background a viewer sees, and it is only ever a rectangle. A shaped or textured background comes from `getBackground`, covered below.

### Images and Backgrounds

There is no image support in a text display, and no packet that carries one. Both images and backgrounds go through a bitmap font provider in a resource pack: register a glyph mapping a private-use character to a PNG, then return that character in a line like any other text. The provider's height and ascent control how it sits against the text, and a negative-space provider backs the cursor up when something needs to sit behind rather than beside.

`getBackground` returns a glyph to draw behind every line, rendered as its own first line. The string must have a net advance of zero so the line stays centred: a negative space of half the glyph's rendered width, the glyph, then a negative space of half its width plus one, since a bitmap glyph advances one pixel past its width.

```json
{
  "providers": [
    {"type": "space", "advances": {"\uE001": -80, "\uE002": -81}},
    {"type": "bitmap", "file": "custom:font/hologram/background.png", "ascent": 7, "height": 96, "chars": ["\uE000"]}
  ]
}
```

```java
@Override
protected String getBackground() {
    return "<font:custom:hologram>\uE001\uE000\uE002</font>";
}
```

An `ascent` of 7 aligns the image's top with the first row, and `height` sets how far down it reaches behind the lines below, at roughly ten pixels per line. The rendered width is the PNG's width scaled by `height` over its own height, and the two advances follow from that. Keep `isShadowed()` false, or the image is drawn with a darkened copy behind it.

The glyph line adds one row of padding above the first line of text, so draw any padding a design wants into the PNG itself.

### Visibility Resolution

There is no registration call and no manual show or hide. A scheduler runs twice a second and reconciles each player's client against each hologram, spawning what has come into view, despawning what has left it, and re-sending the text of dynamic ones:

| State | Result |
|---|---|
| Visible, not currently viewing | Spawned, if `HologramSpawnEvent` is not cancelled |
| Not visible, currently viewing | Despawned |
| Visible, viewing, dynamic | Text re-sent |
| Visible, viewing, static | Nothing |

Visibility depends on the player's position, the hologram's position and `canSee`, and only the first of those produces an event, which is why the system polls rather than listening for movement. The pass is holograms outer and players inner, so a hologram resolves its location and settings once rather than once per player, and players in another world are rejected by a single comparison.

### Moving and Refreshing

Position and rotation live in the add packet and cannot be corrected by a metadata update, so a hologram that has moved is despawned and spawned again rather than updated in place:

```java
// After getLocation() already returns the new location
this.hologramManager.relocate(hologram);

// Push a settings change to current viewers, for a static hologram
this.hologramManager.refresh(hologram);

// Force a clean re-send by dropping every viewer
this.hologramManager.despawn(hologram);

// Look one up by name, case-insensitively
this.hologramManager.getHologramByName("SPAWN").ifPresent(hologram -> this.hologramManager.refresh(hologram));
```

`despawn` is not a way to hide a hologram, since the next pass spawns it again if it is still visible. Use `canSee` or a cancelled `HologramSpawnEvent` for that.

A hologram whose `getLocation()` returns `null`, or names a world that is not loaded, is treated as not ready rather than as an error. The scheduler retries it, so a hologram in a world that loads late starts working on its own.

---

## Picture System

The framework provides a map-based picture system. A `Picture` describes an image and where it hangs, and the framework renders it across a grid of maps in invisible item frames, one map per 128x128 tile. PNG, JPG and anything else `ImageIO` reads all work.

The pictures are real item frames rather than packets, so every player sees the same image at no per-player cost. What the framework keeps out of the world is the part that would otherwise pile up: maps are reused across restarts, and frames are never saved to the chunk.

Requires `@Scan("io.github.trae.spigot.framework.picture")`.

### Defining a Picture

Extend `Picture`, passing an identifier, the top-left frame location, the direction the frames face and the grid size, and register it as a component. `PictureManager` collects every subclass through the dependency injector:

```java
@Singleton
public class PracticeBannerPicture extends Picture {

    private final File file;

    public PracticeBannerPicture(final CorePlugin plugin) {
        super("practice_banner", new Location(Bukkit.getWorld("lobby"), 100.0D, 80.0D, 50.0D), BlockFace.SOUTH, 3, 6);

        this.file = new File(plugin.getDataFolder(), "practice.png");
    }

    @Override
    protected BufferedImage loadImage() throws IOException {
        return ImageIO.read(this.file);
    }
}
```

The location is the air block the top-left frame occupies, in front of the wall. Tiles run left to right, then top to bottom, as seen by a player facing the wall, and every tile needs a solid block behind it. Only horizontal facings are supported.

The image is scaled to `columns * 128` by `rows * 128`, stretching if the aspect ratio differs, so export it at exactly that size for the cleanest result: 384 by 768 for the grid above.

### Maps Across Restarts

A naive implementation creates new maps on every boot, leaving a new `map_N.dat` behind per tile per restart. The framework instead stores each picture's map IDs, alongside a hash of the rendered pixels, in the world's persistent data under `custom:picture_state_<identifier>`.

| On Startup | Result |
|---|---|
| Stored maps exist and the hash matches | The maps are reused as they are, with nothing drawn |
| Stored maps exist and the hash differs | The same maps are redrawn with the new image |
| The grid has grown | Stored maps are reused, and maps are created only for the new tiles |
| Nothing is stored | Maps are created and drawn, and their IDs stored |

Pixels are written straight into each map's saved data and the map is locked, so vanilla persists the image with the map itself and no renderer is ever attached. Swapping the image and restarting is the whole update process.

The identifier forms part of that key, so it must only contain characters valid in a namespaced key once lowercased: `a-z`, `0-9`, `.`, `_` and `-`.

### Colours

A map can only show vanilla's map palette, roughly two hundred and fifty colours, so every pixel is matched to the nearest one by red-mean distance, a perceptual weighting close to how the eye judges colour. Pixels under half alpha become transparent, letting the wall show through.

Flat colours and bold outlines, the style of most server banners, convert cleanly. Smooth gradients band, since the palette has no colours between its steps.

### Frames

Frames are glow item frames by default, so the image stays at full brightness at night and underground. Override `isGlowing()` to use plain frames, which darken with light level like any other block face.

Every frame is invisible, fixed and invulnerable, and `PictureListener` cancels any break of one, whether by a creative player, an explosion or the wall behind it being removed.

Frames are spawned non-persistent and tagged with `custom:picture_identifier`, so they are never saved to the chunk. When a chunk unloads its frames are discarded, and a scheduler respawns them within a second of the chunk loading again. Any tagged frame still in the world at startup, such as one left behind by a reload, is removed before the pictures are rendered.

A picture whose image cannot be read is skipped with a warning rather than failing startup.

---

## NPC System

The framework provides an NPC system backed by real server entities. An `Npc` describes the entity, where it stands and how it behaves, and the framework keeps it spawned.

Real entities rather than packets are what make an NPC more than a decoration: a boss paths, fights and takes damage through the same systems as any other mob, while a shopkeeper stands still and opens a window. Both are the same base class with different behaviour opted into.

Requires `@Scan("io.github.trae.spigot.framework.npc")`.

### Defining an NPC

Extend `Npc` with the backing entity's type, passing its class, an identifier, a namespace and a location, and register it as a component. `NpcManager` collects every subclass through the dependency injector:

```java
@Singleton
public class GuideNpc extends Npc<Villager> {

    public GuideNpc() {
        super(Villager.class, "GUIDE", "lobby", new Location(Bukkit.getWorld("lobby"), 0.5D, 64.0D, 4.5D));
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Guide", NamedTextColor.GOLD);
    }
}
```

The type parameter carries through to every hook, so `onSpawn` and `getEntity()` hand back a `Villager` rather than a `LivingEntity` to cast. The identifier is unique per NPC and is what it is looked up by, while the namespace groups NPCs by the plugin or feature that owns them.

NPCs are collected once the server has finished starting, so an NPC registered by any plugin is picked up regardless of enable order.

### Settings

Every setting has a default, so an NPC overrides only what it needs:

| Hook | Controls | Default |
|---|---|---|
| `getDisplayName()` | The name shown above the entity, and in death messages | `null` |
| `hasAI()` | Whether the entity moves, paths and attacks | `false` |
| `isInvulnerable()` | Whether the entity ignores damage | `true`, or `false` for a `DamageableNpc` |
| `isSilent()` | Whether the entity makes no sounds | `true` |
| `isCollidable()` | Whether the entity pushes and is pushed | `false` |
| `canSpawn()` | Whether the NPC may spawn right now | `true` |
| `onSpawn(entity)` | Further setup once spawned, such as equipment or attributes | Nothing |

### Interaction

Implement `InteractableNpc` for an NPC players can right-click:

```java
@Singleton
public class ShopkeeperNpc extends Npc<Villager> implements InteractableNpc {

    public ShopkeeperNpc() {
        super(Villager.class, "SHOPKEEPER", "lobby", new Location(Bukkit.getWorld("lobby"), 0.5D, 64.0D, 0.5D));
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Shopkeeper", NamedTextColor.GOLD);
    }

    @Override
    public boolean canInteract(final Player player) {
        return !player.isSneaking();
    }

    @Override
    public void onInteract(final Player player) {
        UtilWindow.open(player, new ShopWindow());
    }
}
```

A click passes the cancellable `NpcInteractEvent` first, then the NPC's own `canInteract`, and only reaches `onInteract` if both allow it. Only the main hand is routed, so a click never fires twice.

The vanilla interaction is cancelled for every NPC, interactable or not and in either hand, so a villager never opens its trade screen and an armour stand never gives up its equipment.

### Damage, Death and Respawning

Implement `DamageableNpc` for an NPC that can be hurt and killed:

```java
@Singleton
public class BossNpc extends Npc<Zombie> implements DamageableNpc {

    private long deathTime;

    public BossNpc() {
        super(Zombie.class, "BOSS", "arena", new Location(Bukkit.getWorld("arena"), 0.5D, 64.0D, 0.5D));
    }

    @Override
    public Component getDisplayName() {
        return Component.text("The Warden of Ash", NamedTextColor.RED);
    }

    @Override
    protected boolean hasAI() {
        return true;
    }

    @Override
    protected void onSpawn(final Zombie entity) {
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500.0D);
        entity.setHealth(500.0D);
    }

    @Override
    public boolean canRespawn() {
        return System.currentTimeMillis() - this.deathTime >= TimeUnit.MINUTES.toMillis(10L);
    }

    @Override
    public void onCustomDeathEvent(final CustomDeathEvent event) {
        this.deathTime = System.currentTimeMillis();

        event.getDrops().clear();
    }
}
```

An NPC that does not implement it has every hit cancelled, whether or not the active damage pipeline respects the invulnerability flag, so a shopkeeper is safe even under custom damage.

Damage and death each reach the NPC through exactly one callback, decided by which framework subsystems are registered:

| Registered | Damage Callback | Death Callback |
|---|---|---|
| Neither | `onEntityDamageByEntityEvent` | `onEntityDeathEvent` |
| Death system only | `onEntityDamageByEntityEvent` | `onVanillaDeathEvent` |
| Damage system only | `onCustomDamageEvent` | `onEntityDeathEvent` |
| Damage and death systems | `onCustomDamageEvent` | `onCustomDeathEvent` |

`canRespawn` is only consulted after an actual death. An NPC missing for any other reason, such as its chunk unloading, comes back as soon as it can, as does one that has never spawned. It defaults to `false`, so a `DamageableNpc` that says nothing stays gone once killed.

Under the damage pipeline, a `DamageableNpc`'s death is announced like a player's. In any death message, an NPC with a display name is named by it, whether it died or did the killing.

### Lifecycle

There is no spawn call to make. A scheduler runs once a second and spawns every NPC with no entity whose location is in a loaded chunk, through the cancellable `NpcPreSpawnEvent` and then `canSpawn`.

The entity is spawned non-persistent and tagged with `custom:npc_identifier`, so it is never saved to the chunk and never duplicates across a restart. When its chunk unloads the entity is discarded with it, and the NPC comes back once the chunk loads again. Any tagged entity left in the world, such as one that survived a reload, is removed as soon as it is found.

Two events bracket each spawn. `NpcInitializeEvent` fires once every base setting is applied but before the entity is added to the world, so a change a listener makes is in place before any client sees it. `NpcPostSpawnEvent` fires once the entity is in the world and tracked.

```java
// Look an NPC up by its identifier
this.npcManager.getNpcByIdentifier("BOSS").ifPresent(npc -> this.npcManager.despawn(npc));

// Resolve the NPC behind an entity, including one that has already died
this.npcManager.getNpcByTag(entity).ifPresent(npc -> UtilMessage.log("NPC", npc.getIdentifier()));
```

`getNpcByEntity` only resolves entities that are still tracked, while `getNpcByTag` reads the entity's tag and works on one that has already been untracked, such as after a death.

`despawn` is not a way to hide an NPC, since the next pass spawns it again. Use `canSpawn` or a cancelled `NpcPreSpawnEvent` for that.

---

## Resource Pack System

The framework sends server resource packs from a JSON list, through the Adventure multi-pack API. Each pack is addressed by its own identifier and sent without replacing anything, so packs stack, and applying or removing one never disturbs another the client has loaded.

Requires `@Scan("io.github.trae.spigot.framework.resourcepack")`.

### Configuration

`Resourcepack.json` holds every setting. It is a system configuration, so it lives in the data folder of the first plugin that scans the resource pack package. Resource packs are off until `enabled` is set.

```json
{
  "enabled": true,
  "prompt": "",
  "kickMessage": "<red>You must accept the resource pack to play.",
  "resourcePacks": [
    {"id": "4b1d6f0e-2c7a-4e93-9a58-1f3e7c0d2b64", "url": "https://example.com/core.zip", "hash": "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", "required": true, "worlds": [], "permission": ""},
    {"id": "c83a0f52-91d7-4b6e-8e2f-7a4c5d19b0e3", "url": "https://example.com/lobby.zip", "hash": "", "required": false, "worlds": ["lobby"], "permission": ""}
  ]
}
```

| Setting | Controls |
|---|---|
| `enabled` | Whether any pack is sent, `false` by default |
| `prompt` | The MiniMessage prompt on the download screen, or empty for vanilla's |
| `kickMessage` | The MiniMessage kick message for a required pack that fails |
| `resourcePacks` | The packs, in stacking order |

Each entry in `resourcePacks` holds its own settings:

| Setting | Controls |
|---|---|
| `id` | The pack's identifier, generated once and kept |
| `url` | The direct download URL, where an entry with none is never sent |
| `hash` | The SHA-1 of the zip as 40 hex characters, or empty to skip verification |
| `required` | Whether declining it or failing to load it kicks the player |
| `worlds` | The worlds the pack is active in, or empty for every world |
| `permission` | The permission needed to receive it, or empty for none |

Packs apply in list order, so a later entry overrides assets from an earlier one. A fresh file is seeded with one empty entry to show the shape.

### Delivery

On join, every pack the player is eligible for goes out in a single request, so the player sees one prompt. The request is required if any pack in it is.

On a world change, only the difference is sent: packs eligible in the old world and not the new one are removed, and packs eligible only in the new one are applied. A pack eligible in both stays loaded, with no second download.

A required pack that is declined, fails to download, has an invalid URL or fails to reload kicks the player with `kickMessage`. The check is per pack, so an optional pack failing alongside a required one kicks nobody.

Permission is checked on join and on world change only, so granting or revoking it mid-session takes effect on the player's next world change or rejoin.

### Gating a Pack

`ResourcePackApplyEvent` fires once per pack, after its world and permission checks pass. Cancelling it skips that pack alone, leaving the rest of the request intact:

```java
@EventHandler
public void onResourcePackApply(final ResourcePackApplyEvent event) {
    if (!event.getResourcePack().isRequired() && this.settingsManager.hasCosmeticPacksDisabled(event.getPlayer())) {
        event.setCancelled(true);
    }
}
```

---

## Damage System

The framework takes vanilla damage over entirely. The vanilla event is cancelled the moment it fires, a three-stage chain runs in its place, and the resolved figure is applied by hand.

That is a heavier intervention than the other subsystems make, and it buys two things. Damage becomes something a plugin composes rather than overwrites, so an ability, a weapon, a set of armour and a potion effect can each contribute without any of them knowing about the others. And the numbers become yours: the vanilla behaviour ships as the default, expressed in ordinary listeners that a game mode can replace or rebalance piece by piece.

Requires `@Scan("io.github.trae.spigot.framework.damage")`.

### The Pipeline

Every hit passes through three stages, each its own event, each cancellable. Cancelling stops the chain, so a later stage never runs against damage that was refused.

| Stage | For |
|---|---|
| `CustomPreDamageEvent` | Gating the hit and establishing the base |
| `CustomDamageEvent` | Consuming plugins setting ability damage |
| `CustomPostDamageEvent` | Reductions and side effects |

The pre stage is where the damage delay is enforced, where the weapon's contribution is resolved from the held item, and where a critical hit is recognised. By the time it ends, the base reflects what the attack is worth before anything custom touches it.

A player's melee hit starts from a base of zero, since vanilla's own figure already carries the held item, sharpness, the attack charge and the critical multiplier, all of which the pipeline resolves itself. Every other hit, a mob's attack included, starts from vanilla's figure, since that damage comes from attributes the pipeline does not resolve.

The damage stage is reserved. Nothing in the framework writes damage there, so an ability has the field to itself:

```java
@EventHandler
public void onCustomDamage(final CustomDamageEvent event) {
    if (!this.isFrostbite(event)) {
        return;
    }

    event.setDamage(7.0D);
    event.removeModifier(DamageModifier.WEAPON);
}
```

The post stage applies armour, protection and resistance early, then durability, knockback and the delay record at the end. Once it completes, `DamageManager` deals what is left.

### Hit Sounds

Cancelling vanilla's damage handling also cancels the sound an entity makes when struck, so the pass carries one of its own as a `SoundProvider`. It is seeded from the damagee rather than the weapon, so a zombie grunts and a skeleton rattles, and it plays under the entity's own sound category, so it follows the same client volume slider vanilla's would.

The sound is settable at any stage and read only once the damage lands, so an ability replaces it rather than playing its own alongside it:

```java
@EventHandler
public void onCustomDamage(final CustomDamageEvent event) {
    if (!this.isFrostbite(event)) {
        return;
    }

    event.setSoundProvider(SoundProvider.of(Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0F, 1.6F));
}
```

Set it to `null` for a silent hit. It is skipped on a hit that kills, since the death sound covers that, and a non-living damagee has none to begin with.

### Modifiers

The damage figure is a base plus a set of named contributions, rather than a single number listeners fight over. Each key holds at most one additive and one multiplier, so two things writing to different keys compose and two things writing to the same key do not.

| Key | Holds |
|---|---|
| `WEAPON` | The attacking item's own damage |
| `CRITICAL` | The critical hit multiplier |
| `POTION` | Strength and weakness on the attacker |
| `ARMOUR` | The reduction from worn armour |
| `PROTECTION` | The reduction from protection enchantments |
| `RESISTANCE` | The reduction from the resistance effect |
| `CUSTOM` | Anything a consuming plugin contributes |

```java
// One thing lives under this key, so replace it
event.setModifier(DamageModifier.WEAPON, 6.0D);

// Several things stack under this key, so add to it
event.addModifier(DamageModifier.ARMOUR, -2.5D);

// Proportional rather than flat
event.setMultiplier(DamageModifier.CRITICAL, 1.5D);

// Discard a contribution an ability does not want
event.removeModifier(DamageModifier.WEAPON);
```

`getFinalDamage()` applies every additive to the base first, then every multiplier, and floors the result at zero. That ordering is why a reduction written as a negative additive is still scaled by a multiplier written elsewhere, which is what makes armour and resistance compose the way vanilla's do.

### Dealing Damage Yourself

A plugin that wants to deal damage directly builds a pre stage and dispatches it, rather than calling `damage` and having the pipeline intercept it:

```java
// Environmental, with no attacker
UtilEvent.dispatch(CustomPreDamageEvent.create(target, DamageCause.MAGIC, 4.0D, Reason.of(Component.text("Trap"))));

// Attributed to an attacker, so the kill is credited and mobs retaliate
UtilEvent.dispatch(CustomPreDamageEvent.create(target, player, DamageCause.MAGIC, 4.0D, Reason.of(Component.text("Ignite"))));
```

Both go through the full chain, so a delay still applies, armour still reduces, and the death system still sees it. Neither carries a hit sound, since there is no swing behind it, so set one on the event before dispatching if the damage should be heard.

### Reasons

A reason is what the damage is attributed to in messages. The pipeline seeds one from the attacker's held item, complete with its display name and hover tooltip, so a death message can show what killed someone without rebuilding it.

A `CustomReason` carries a duration and outlives the hit that set it. That is what an ability wants: the attribution should survive the attacker swapping items, and should still name the ability if the kill lands a few seconds later.

```java
// Stands for five seconds, whatever the killing blow turns out to be
event.setReason(CustomReason.of(Component.text("Frostbite 5"), TimeUnit.SECONDS.toMillis(5L)));

// Stands until the target dies
event.setReason(CustomReason.of(Component.text("Bleed")));
```

The manager retains one of these per damagee and attacker pair, and the death system reads it before falling back to the killing hit's own reason. A plain reason reads with an article in front of it, so an item renders as "with a Diamond Sword"; a custom one does not, so an ability renders as "with Frostbite 5".

### Vanilla Defaults

Because the pipeline owns everything, every piece of vanilla behaviour it replaced ships as a listener. Each is ordinary code with no special standing, which is what makes the whole set replaceable.

| Listener | Provides |
|---|---|
| `DamageWeaponReductionListener` | A player's melee damage from the held item, by material, plus sharpness, scaled by attack charge in vanilla combat |
| `DamageCriticalListener` | The critical hit multiplier |
| `DamageArmourReductionListener` | Armour, toughness and protection reduction |
| `DamagePotionEffectListener` | Strength and weakness on a player's melee hit, and resistance on the damagee |
| `DamageWeaponDurabilityListener` | One durability point per hit on the attacker's item |
| `DamageArmourDurabilityListener` | A quarter of the damage, floored at one, on each worn piece |
| `DamageKnockbackListener` | Knockback away from the attacker, with resistance applied |
| `DamageDelayListener` | The immunity window for every cause, shared for PvP in vanilla combat and per source otherwise |
| `DamageIntervalListener` | A hit offered every tick for causes vanilla times itself, so their window sets their rate |
| `DamageAttackSpeedListener` | The attack cooldown, restored or removed depending on the combat mode |

The figures match vanilla: armour points and toughness by material, the twenty-point cap and the division by twenty-five, a point for the first level of sharpness and half a point for each after, three damage per level of strength and four less per level of weakness, twenty percent per resistance level, and the displayed attack damage for every weapon including copper. Whether armour, protection and resistance apply is decided by the damage type's tags, as vanilla decides it, so armour does nothing against drowning or poison, protection does nothing against starvation, and resistance does nothing against `/kill`.

Every formula figure is read from `Damage.json` on each hit, while the per-material tables stay in code, since per-item balance already has its own events. See [Rebalancing](#rebalancing).

### Configuration

`Damage.json` holds every combat setting. It is a system configuration, so it lives in the data folder of the first plugin that scans the damage package. The defaults match vanilla.

| Setting | Controls |
|---|---|
| `oldCombatEnabled` | Whether combat follows pre-1.9 rules rather than vanilla's, `false` by default |
| `oldCombatAttackSpeed` | The attack speed players are given while old combat is enabled |
| `delay` | The immunity window per damage cause, and the default for any cause without one |
| `interval` | What each extra hit of burning, drowning, freezing and starvation deals, and the health starvation stops at |
| `critical` | Whether critical hits deal extra damage, and the multiplier |
| `knockback` | The strength of the push and the most upward velocity a grounded target is given |
| `weaponReduction` | The sharpness bonus, and the attack charge curve for the weapon's damage |
| `armourReduction` | The armour cap and divisor, the toughness formula, and the protection cap and divisor |
| `potionEffect` | Strength and weakness per level, resistance per level, and the attack charge curve for strength and weakness |

A reload takes effect on the next hit, since every listener reads the same instance and a reload updates it in place. Attack speed is the exception, being written onto each player rather than read per hit, so it is pushed to everyone online when the file is reloaded.

A section added in a later version is written into an existing file on its next load. A value already in the file is kept, including the `delay` map, so a cause added to its defaults later only appears in a fresh file.

### Combat Mode

`oldCombatEnabled` decides which combat rules apply.

With old combat disabled, combat matches vanilla 1.21. The attack cooldown applies, and a player's melee damage is scaled by how charged the swing was: the item's damage and any strength or weakness by a fifth plus four fifths of the charge squared, and sharpness by the charge alone. Both curves are configurable, under `weaponReduction` and `potionEffect`. A player hit by a player gets one immunity window shared by every player, so a hit from one briefly protects against all of them.

With old combat enabled, attack speed is raised to `oldCombatAttackSpeed`, high enough that every swing lands at full strength, and PvP uses the same per attacker windows as everything else, so two players can hit the same target at once.

Every hit that is not PvP is windowed per attacker and per environmental cause in both modes. A mob or the environment never blocks a player's hit, a player's hit never blocks them, and burning does not protect against drowning.

### Immunity Windows

The window's length comes from the hit's cause in both modes, falling back to `defaultValue` for a cause with no value of its own. The window is the only thing spacing hits out, since cancelling vanilla's damage also discards its invulnerability frames.

Vanilla fires some causes on intervals of its own, hardcoded into the game, and a window could only ever slow those down. `DamageIntervalListener` offers each of them a hit on every tick for as long as the entity is still in the state that causes it, so for those causes the window is the actual rate:

| Cause | Default | Vanilla's own interval |
|---|---|---|
| `POISON` | `1250` | 25 ticks, halved per level |
| `WITHER` | `2000` | 40 ticks, halved per level |
| `FIRE_TICK` | `1000` | 20 ticks |
| `DROWNING` | `1000` | 20 ticks |
| `FREEZE` | `2000` | 40 ticks |
| `STARVATION` | `4000` | 80 ticks |

The defaults match vanilla at the first effect level. Setting `POISON` to `100` makes poison hit every two ticks, whatever its level, since the window alone now sets the pace. Nothing can hit more than once a tick, so any value under `50` behaves as `50`.

Each extra hit goes through vanilla's own damage call, with vanilla's source and cause, so it keeps vanilla's own checks: fire resistance and fire immunity, poison stopping at one health, and starvation stopping at a floor on peaceful, easy and normal. Burning, drowning, freezing and starvation deal the amounts set under `interval`, as do their starvation floors. Poison and wither deal what their effect deals, since their hits run inside the effect's own code. Poison and wither fire `EntityEffectTickEvent` first, as vanilla does. Vanilla's own interval hits still run alongside, and the window refuses them like any other, so nothing is dealt twice.

Poison, wither and burning are picked up the moment they start. Drowning, freezing and starvation are picked up from their first vanilla hit, so they begin one vanilla interval in.

Every other cause already fires as often as vanilla allows. Contact damage, such as lava, fire, cactus and magma, fires every tick once invulnerability frames are gone, so its window is already its rate.

### Potion Effects

A player's melee hit starts from a base of zero, so the effects vanilla folds into the attack damage attribute are added back by `DamagePotionEffectListener`, under `POTION`. Strength adds and weakness removes a set amount per level, scaled by the attack charge in vanilla combat. Weakness never takes the hit below what the weapon itself deals, as vanilla floors the attribute at zero. A mob's attack already carries both in vanilla's figure, so only players are resolved.

Resistance reduces damage by a share per level, under `RESISTANCE`, floored at total immunity.

Everything else is left to what already handles it: poison and wither to `DamageIntervalListener`, absorption to `DamageManager`, fire resistance to vanilla's own invulnerability check, and jump boost and slow falling to vanilla's fall damage, which they reduce before the event fires.

### Rebalancing

The two reduction events are per item, which is the hook a game mode uses to move off vanilla's balance without replacing the formula.

```java
@EventHandler
public void onWeaponReduction(final WeaponReductionEvent event) {
    if (event.getItemStack().getType() == Material.IRON_SWORD) {
        event.setAmount(4.0D);
    }
}

@EventHandler
public void onArmourReduction(final ArmourReductionEvent event) {
    if (event.getItemStack().getType() == Material.DIAMOND_CHESTPLATE) {
        event.setAmount(24.0D);
        event.setToughness(0.0D);
    }
}
```

`WeaponReductionEvent` sets what that item is worth in damage. `ArmourReductionEvent` sets what that piece is worth in armour points, and its toughness separately; every piece's value is summed and the total goes through the reduction formula, so raising one piece makes the whole set stronger. Setting toughness to zero collapses the formula to a flat armour curve, which is the pre-1.9 shape.

Cancelling either excludes that item from the calculation entirely.

### Applying Damage

`DamageManager` deals the resolved figure and reproduces what vanilla would have done: health and absorption, the hurt animation and flash, the combat tracker that names a killer in death messages, mob aggro, statistics, advancements, death protection, and death itself.

Aggro is the one worth knowing about. Marking the attacker as the damagee's last attacker is a single field write, and it is what makes a spider turn on a player who hits it in daylight, and what angers neutral mobs. Without it, custom damage would be invisible to mob behaviour.

Death protection is the other. Vanilla's totem check is private, so it is reproduced here: when a hit would kill, a death protection item in either hand fires `EntityResurrectEvent`, and on success the item is consumed, health is set to half a heart, the item's effects are applied and the totem animation plays. The entity never dies, so no death event fires. Any item carrying the death protection component counts, not only a totem, and sources that bypass invulnerability, such as the void, are never protected against.

One piece of vanilla behaviour remains unreachable from a plugin, because the method behind it is not visible: the damagee's own last damage source. The retained damage pass stands in for it, which is what the death system reads.

---

## Death System

The death system turns a vanilla death into one that knows what caused it.

Vanilla's own death handling only knows that an entity died. The damage pipeline knows who dealt the blow, with what item, and for what reason, so the two are joined here: `DamageManager` retains the last damage pass that landed on each entity, and the death listener reads it.

Requires `@Scan("io.github.trae.spigot.framework.death")`. The damage system is optional.

### Two Shapes of Death

Which event a death gets depends on whether the damage system is registered:

| Event | Dispatched When | Knows |
|---|---|---|
| `CustomDeathEvent` | The damage system is registered and has a record of the killing pass | The pass itself, with the item, both names, the cause and the resolved reason |
| `VanillaDeathEvent` | The damage system is not registered | Only what the entity and the vanilla damage source still hold, with the killer's held item standing in for the reason |

With the damage system registered, a death the pipeline has no record of dispatches nothing, since there is no damage behind it to report.

Both implement `DeathEvent`, which carries the entity, the killer, the cause, the reason, and the parts a listener can still change. Code that only needs those takes the interface and works whichever shape produced it:

```java
@EventHandler
public void onCustomDeath(final CustomDeathEvent event) {
    this.onDeath(event);
}

@EventHandler
public void onVanillaDeath(final VanillaDeathEvent event) {
    this.onDeath(event);
}

private void onDeath(final DeathEvent event) {
    final Entity killer = event.getKiller();
    if (killer == null) {
        return;
    }

    this.statisticsManager.addKill(killer);
}
```

Both are dispatched from inside the vanilla death event at its last priority, so the death itself is settled and neither is cancellable. A vanilla death another plugin cancelled is skipped outright, since nothing actually died.

Attribution is resolved before `CustomDeathEvent` is dispatched rather than read straight off the damage pass. A killer with an unexpired `CustomReason` standing against the target is credited with that instead of with whatever the killing hit happened to be, so a kill landed with a sword moments after an ability still names the ability. Both retained records are dropped only after dispatch, so a listener reading them for the same entity still finds them.

### Drops, Experience and Sound

Three things are still open when the event fires, and whatever listeners leave them as is written back to the vanilla death:

```java
@EventHandler
public void onCustomDeath(final CustomDeathEvent event) {
    event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.ROTTEN_FLESH);
    event.setDropExp(event.getDropExp() * 2);
    event.setSoundProvider(SoundProvider.of("custom:entity.boss.death", SoundCategory.HOSTILE, 2.0F, 1.0F));
}
```

The drop list is the event's own copy, and replaces the vanilla list wholesale once dispatch ends. Experience is copied back as it stands.

The death sound starts as the vanilla one, and how it is written back depends on what it names:

| Sound | Result |
|---|---|
| A sound the server knows | Replaces vanilla's death sound, with its category, volume and pitch |
| A sound only a resource pack knows | Vanilla's is silenced, and this one is played at the entity's location instead |
| `null` | The death is silent |

A death vanilla already keeps silent, such as a silent entity's, stays silent whatever the listener sets.

### Death Messages

Vanilla's message is suppressed and replaced. The replacement is dispatched once per recipient rather than broadcast, so a plugin can vary it by who is reading it, or suppress it for some players and not others:

```java
@EventHandler
public void onCustomDeathMessage(final CustomDeathMessageEvent event) {
    if (this.settingsManager.hasDeathMessagesHidden(event.getRecipient())) {
        event.setCancelled(true);
    }
}
```

The event carries the `DeathEvent` behind it, so a listener reads the entity and killer the same way whichever shape produced the death. Both names are `DisplayName`s, settable per recipient, so a team colour, a rank or a class tag replaces only that recipient's copy:

```java
@EventHandler
public void onCustomDeathMessage(final CustomDeathMessageEvent event) {
    if (event.getDeathEvent().getEntity() instanceof final Player player) {
        event.setEntityName(DisplayName.of(this.getRankPrefix(player), Component.text(player.getName()), null));
    }
}
```

The message takes one of three shapes: a self-inflicted death names nobody, a death with a killer names them and what it was attributed to, and anything else names the cause.

Messages are only produced for player deaths, and for the deaths of a `DamageableNpc` under the damage pipeline, which the NPC system announces like a player's. Other mob deaths still fire the death events, so a plugin wanting to announce those listens to them directly.

---

## Effect System

The framework provides a timed effect system for any `LivingEntity`. An effect holds its own users, each with an amplifier and a duration, and can bind itself to a vanilla potion effect that the framework keeps in sync across every transition.

Requires `@Scan("io.github.trae.spigot.framework.effect")`.

### Defining an Effect

Extend `Effect`, passing a name, and register it as a component. `EffectManager` discovers every subclass automatically through the dependency injector:

```java
@Singleton
public class BleedEffect extends Effect {

    public BleedEffect() {
        super("Bleed");
    }

    @Override
    protected void onTick(final LivingEntity livingEntity, final EffectData effectData) {
        livingEntity.damage(effectData.getAmplifier() * 0.5D);
    }
}
```

Effects are collected once the server has finished starting rather than on plugin enable, so an effect registered by any plugin is picked up regardless of enable order.

### Applying an Effect

```java
// Amplifier 2 for 10 seconds
this.bleedEffect.addUser(player, 2, TimeUnit.SECONDS.toMillis(10L));

// Amplifier 0, so no potion effect is sent
this.bleedEffect.addUser(player, TimeUnit.SECONDS.toMillis(10L));
```

Amplifiers are one-based, so `1` is level I, matching how a player reads it rather than how Bukkit stores it. Durations are milliseconds throughout, converted to ticks only where a packet needs them.

### Binding a Potion Effect

Override `getPotionEffectType()` and the framework applies, re-applies and clears the vanilla effect alongside its own state:

```java
@Singleton
public class FrostEffect extends Effect {

    public FrostEffect() {
        super("Frost");
    }

    @Override
    protected PotionEffectType getPotionEffectType() {
        return PotionEffectType.SLOWNESS;
    }
}
```

Left unbound, which is the default, the effect is purely logical and nothing vanilla is sent.

### Updating an Effect

`updateUser` hands the stored `EffectData` to a consumer and reconciles the potion effect with whatever the consumer leaves behind:

```java
// Extend by 5 seconds, keeping the time already served
this.frostEffect.updateUser(player, true, effectData -> effectData.setDuration(effectData.getDuration() + TimeUnit.SECONDS.toMillis(5L)));

// Raise the amplifier and start the new duration from scratch
this.frostEffect.updateUser(player, false, effectData -> {
    effectData.setAmplifier(effectData.getAmplifier() + 1);
    effectData.setDuration(TimeUnit.SECONDS.toMillis(30L));
});
```

The `carryOver` flag decides what happens to time already served when the duration grows. With it set, the potion effect is re-applied for the time remaining plus the increase, so a player 30 seconds into a one minute effect that is raised to two minutes ends up with a minute and a half. Without it, the new duration replaces what was left outright.

Zeroing the duration clears the potion effect but keeps the entry, so an effect can sit dormant and be revived by a later update. Zeroing the amplifier ends the effect entirely.

### Guards and Hooks

Each transition has a guard that can refuse it and a hook that runs once it has happened:

| Override | Runs |
|---|---|
| `canAdd` | Before the effect is stored, to refuse it |
| `canRemove` | Before a deliberate removal, to refuse it |
| `canUpdate` | Before the update consumer runs, to refuse it |
| `onAdd` | Once the effect is stored and its potion effect sent |
| `onUpdate` | Once the data is mutated and the potion effect reconciled |
| `onRemove` | Once the effect is removed, expiry aside |
| `onExpire` | Once the duration has elapsed |
| `onRemoveOrExpire` | On every path that ends the effect, after the path's own hook |
| `onTick` | Every tick, for each live holder that has not expired |

`onRemoveOrExpire` is the one to use for teardown that must happen however the effect ended, rather than duplicating it across `onRemove` and `onExpire`.

### Effects as Listeners

An effect is a component like any other, so it can implement `Listener` and be registered alongside everything else. That suits an effect whose whole behaviour is a reaction to something rather than a per-tick one, since the effect's state and the handler that reads it live in the same class:

```java
@Singleton
public class NoFallEffect extends Effect implements Listener {

    public NoFallEffect() {
        super("No Fall");
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (!(event.getEntity() instanceof final LivingEntity livingEntity)) {
            return;
        }

        if (this.getUserByLivingEntity(livingEntity).isEmpty()) {
            return;
        }

        event.setCancelled(true);
    }
}
```

`getUserByLivingEntity` is the membership check, and it doubles as the read when the handler needs the amplifier or the time left:

```java
this.getUserByLivingEntity(livingEntity).ifPresent(effectData -> event.setDamage(event.getDamage() / effectData.getAmplifier()));
```

The handler runs on whatever thread the event does, so an effect that is also a listener can read entity state directly, unlike `onTick`, which runs on the manager's own schedule.

### Ending an Effect

```java
// Deliberate removal, the one path a listener can refuse
this.frostEffect.removeUser(player);
```

Everything else is the framework's own doing, and carries a reason:

| Reason | Cause |
|---|---|
| `NORMAL` | A deliberate `removeUser` call |
| `EXPIRE` | The duration elapsed |
| `CONDITIONAL` | `removeOnCondition` returned true during the tick |
| `DEATH` | The holder died and the effect overrides `removeOnDeath` |
| `QUIT` | The holder disconnected and the effect overrides `removeOnQuit` |

Only `NORMAL` consults `canRemove` and `EffectPreRemoveEvent`, since it is the one case where the removal is still a proposal. The rest report something that has already happened and are applied unconditionally.

`removeOnCondition` is checked every tick, for state the effect wants to end itself on:

```java
@Override
protected boolean removeOnCondition(final LivingEntity livingEntity, final EffectData effectData) {
    return livingEntity.isInWater();
}

@Override
public boolean removeOnDeath() {
    return true;
}

@Override
public boolean removeOnQuit() {
    return true;
}
```

Both `removeOnDeath` and `removeOnQuit` default to false, so an effect survives a death or a reconnect unless it says otherwise.

### Querying

```java
// Every user, including entities that are currently unloaded
final Map<UUID, EffectData> users = this.frostEffect.getUsers();

// Only the users whose entity can be resolved right now
final Map<LivingEntity, EffectData> activeUsers = this.frostEffect.getActiveUsers();

// One user
final Optional<EffectData> effectData = this.frostEffect.getUserByLivingEntity(player);
```

`getActiveUsers` is a snapshot rebuilt per call, and it silently omits any user whose entity is offline or unloaded, so `getUsers` stays the source of truth for membership and counts.

### Cost

The manager ticks every 50ms and walks each effect's users, resolving the entity, checking expiry and dispatching the tick event. An effect with no users costs a map iteration and nothing else, so the cost scales with how many entities actually hold an effect rather than with how many effects exist.

The user map is pruned in place during that walk, so a hook invoked from it must not add or remove users of the same effect directly.

---

## Chat System

The chat system takes vanilla chat over and routes every message through a channel, such as global, staff, faction or ally chat.

The framework keeps no record of who is in which channel. That state belongs to the plugin, such as a channel stored on the player's account, and the framework asks for it on every message. That keeps one source of truth, and lets any number of plugins take part in deciding where a message goes.

Requires `@Scan("io.github.trae.spigot.framework.chat")`, and a `DefaultChatChannel` declared by the plugin that owns chat.

### The Flow

Vanilla's chat event is cancelled at its last priority, so every other plugin sees it first and vanilla never sends the message. Three events carry it from there, in order:

| Event | For |
|---|---|
| `ChatChannelEvent` | Resolving which channel the message is sent in |
| `ChatSendEvent` | Refusing or changing the message for everyone |
| `ChatReceiveEvent` | Refusing or changing one recipient's copy |

Each copy that survives its receive event is formatted by its channel and delivered to its recipient.

`ChatSwitchChannelEvent` sits outside that flow. It is how a player moves between channels, and is dispatched by the plugin rather than the framework.

A message another plugin already cancelled on the vanilla event is left alone.

### Defining a Channel

A channel decides who receives a message and how it reads, both per sender, so one implementation serves every player in it:

```java
@Singleton
public class StaffChatChannel implements ChatChannel {

    @Override
    public String getName() {
        return "Staff";
    }

    @Override
    public List<Player> getRecipients(final Player sender) {
        return UtilServer.getOnlinePlayers().stream().filter(player -> player.hasPermission("core.staffchat")).toList();
    }

    @Override
    public Component getFormat(final Player sender, final Component message) {
        return Component.text("[Staff] ", NamedTextColor.RED).append(Component.text(sender.getName(), NamedTextColor.YELLOW)).append(Component.text(": ", NamedTextColor.WHITE).append(message.colorIfAbsent(NamedTextColor.WHITE)));
    }
}
```

The sender is not added to the recipients automatically, so include them if they should see their own message. A faction channel resolves the sender's own faction inside `getRecipients` rather than existing once per faction.

### The Default Channel

Exactly one channel implements `DefaultChatChannel`. It is what every message starts in, and what a player chats in when their state names no other channel:

```java
@Singleton
public class GlobalChatChannel implements DefaultChatChannel {

    @Override
    public String getName() {
        return "Global";
    }

    @Override
    public List<Player> getRecipients(final Player sender) {
        return UtilServer.getOnlinePlayers();
    }

    @Override
    public Component getFormat(final Player sender, final Component message) {
        return Component.text(sender.getName(), NamedTextColor.YELLOW).append(Component.text(": ", NamedTextColor.WHITE).append(message.colorIfAbsent(NamedTextColor.WHITE)));
    }
}
```

`ChatManager` takes it through its constructor, so a plugin that enables the chat package without declaring one fails at startup rather than silently dropping every message.

### Resolving the Channel

`ChatChannelEvent` is fired for every message, seeded with the default channel. A listener reads the plugin's own state and sets the channel to match:

```java
@EventHandler(priority = EventPriority.LOW)
public void onChatChannel(final ChatChannelEvent event) {
    this.accountManager.getAccount(event.getSender()).map(Account::getChatChannel).ifPresent(event::setChannel);
}
```

Several listeners may set the channel, and the last to run wins, so priority decides between them. A state that should override a player's own choice sets it later:

```java
@EventHandler(priority = EventPriority.HIGH)
public void onChatChannelWhileJailed(final ChatChannelEvent event) {
    if (this.jailManager.isJailed(event.getSender())) {
        event.setChannel(this.jailChatChannel);
    }
}
```

The event is not cancellable, since every message needs a channel. Refusing a message belongs on the send event.

### Switching Channels

A command that moves a player between channels dispatches `ChatSwitchChannelEvent`, so every switch passes through one event whichever command caused it:

```java
@Override
public void execute(final Player player, final String[] args) {
    UtilEvent.dispatch(new ChatSwitchChannelEvent(player, this.staffChatChannel));
}
```

The framework does not act on it. The plugin records the new channel once the event survives, which is the state `ChatChannelEvent` reads back on the next message:

```java
@EventHandler(priority = EventPriority.MONITOR)
public void onChatSwitchChannel(final ChatSwitchChannelEvent event) {
    if (event.isCancelled()) {
        return;
    }

    this.accountManager.getAccount(event.getPlayer()).ifPresent(account -> account.setChatChannel(event.getChannel()));

    UtilMessage.message(event.getPlayer(), "Chat", "You are now chatting in <green>%s</green>.".formatted(event.getChannel().getName()));
}
```

Recording at `MONITOR` keeps the state in step with the outcome, since a listener that cancels the switch at any earlier priority has already had its say. Cancelling refuses the switch, such as a player without the rank for staff chat or with no faction to chat in.

### Sending and Receiving

`ChatSendEvent` is fired once per message, after the channel is settled. Cancel it to refuse the message for everyone, or change the message to change it for everyone:

```java
@EventHandler
public void onChatSend(final ChatSendEvent event) {
    if (this.punishmentManager.isMuted(event.getSender())) {
        event.setCancelled(true);
        UtilMessage.message(event.getSender(), "Chat", "You are muted.");
    }
}
```

A message that survives is split into one `ChatReceiveEvent` per recipient the channel names. Cancel one to hide the message from that recipient alone, or change its message to vary it for them:

```java
@EventHandler
public void onChatReceive(final ChatReceiveEvent event) {
    if (this.ignoreManager.isIgnoring(event.getRecipient(), event.getSender())) {
        event.setCancelled(true);
    }
}
```

Each copy that survives its own event is passed through the channel's `getFormat` and delivered to its recipient as a server message. The format is applied last, to the message that recipient's event settled on, so a listener changing one copy changes only the message inside the line, and the channel's wrapping stays the same for everyone.

The channel is fixed once the send event fires. A listener that needs a different channel sets it on `ChatChannelEvent` instead.

### Threading

A message a player types is handled off the main thread, so the channel, send and receive events, and a channel's `getRecipients` and `getFormat`, all run there. Keep them to state that is safe to read from another thread, and schedule anything that touches the world back onto the main thread.

---

## Sounds

A `SoundProvider` is a sound together with the category, volume and pitch to play it at, built once and replayable anywhere. The damage pass and both death events carry one.

The sound is held as its namespaced key rather than as a `Sound`, so a sound that exists only in a resource pack is named as easily as a vanilla one, and the value survives being written to configuration and read back:

```java
// A vanilla sound, converted to its key through the registry
SoundProvider.of(Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.2F);

// A resource pack sound, by key
SoundProvider.of("custom:ui.unlock", SoundCategory.MASTER, 1.0F, 1.0F);

// Full volume and normal pitch
SoundProvider.of(Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS);
```

```java
// Heard by everyone in range of a location
soundProvider.play(location);

// Heard by one player, following them as they move
soundProvider.play(player);

// Heard by every online player from their own position
soundProvider.broadcast();
```

Pick the category vanilla would use for the same kind of sound, so players can turn it down with the slider they expect: `PLAYERS` for anything a player's own gear or body makes, `HOSTILE` or `NEUTRAL` for mobs, and `MASTER` only for something that must always be heard.

Nothing here throws. A `null` sound, a sound with no registered key, a `null` key or a `null` category all yield a provider that plays nothing, so a provider can be built straight from a nullable source, such as the death sound of an entity that has none, without a guard. `getSound()` resolves the key back to a `Sound`, and is empty for a resource pack sound, which still plays.

---

## Display Names

A `DisplayName` is a name in three parts: whatever sits before it, the name itself, and whatever sits after it.

Keeping the parts separate rather than pre-joined means a consumer can take just the name where a prefix would be noise, such as a compact scoreboard line, and the full thing where it would not. Either side may be absent, which is the normal case for anything unranked or untagged.

```java
// With a rank in front and a tag behind
DisplayName.of(rankComponent, nameComponent, tagComponent);

// Nothing either side
DisplayName.of(nameComponent);
```

`getFullName()` joins the parts with no separator and skips the absent ones, so any spacing a caller wants between the parts belongs in the prefix or suffix itself:

```java
// Renders as "[Admin] Steve"
DisplayName.of(Component.text("[Admin] "), Component.text("Steve"), null);
```

The damage pipeline carries one for each side of a hit, so a plugin with display names, ranks or nicknames writes them once at the pre stage rather than at every message site. The death message event carries one for each side too, settable per recipient.

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

The damage system reaches deeper than this, driving vanilla's own damage internals directly rather than sending packets. See [Damage System](#damage-system).

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
| `UtilHologram` | Spawning, updating and despawning a `Hologram` for a single player |
| `UtilDamage` | Resolving whether an attack qualifies as a critical hit |
| `UtilMaterial` | Classifying materials by whether a block responds to a click, to a held item, or whether an item has a use of its own |
| `UtilAdventure` | Joining components with a separator and an inclusion filter, skipping nulls and empties |
| `UtilColor` | Converting between AWT, Adventure and Bukkit colours, and wrapping text in MiniMessage colour tags |
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
| `Hologram` | Define a packet-based floating text display with per-player text and visibility |
| `Picture` | Define an image shown across a grid of item frames |
| `Npc` | Define a non-player character backed by a real entity |
| `Reason` | Describe what a hit is attributed to in messages |
| `CustomReason` | Describe an attribution that outlives the hit that set it |
| `ChatChannel` | Define a chat channel with its own recipients and format |
| `DefaultChatChannel` | Define the channel every message starts in, exactly once |

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
| `CustomAsynchronousEvent` | Base asynchronous event with `Void` key type |
| `CustomCancellableEvent` | Synchronous event with cancellation and reason |
| `CustomAsynchronousCancellableEvent` | Asynchronous event with cancellation and reason |

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

## Hologram Events

| Event | Fired When |
|---|---|
| `HologramSpawnEvent` | A hologram is about to be sent to a player |
| `HologramDespawnEvent` | A hologram's remove packet has been sent to a player |
| `HologramUpdateEvent` | A hologram's metadata packet has been sent to a player |

Only the spawn event is cancellable, and cancelling it stops the hologram being sent. The other two are notifications dispatched after their packets have gone out. `HologramUpdateEvent` fires for every viewer of every dynamic hologram on each pass, so a listener on it should stay proportionate to that.

---

## NPC Events

| Event | Fired When |
|---|---|
| `NpcPreSpawnEvent` | An NPC's entity is about to be spawned |
| `NpcInitializeEvent` | An NPC's entity has been configured, before it is added to the world |
| `NpcPostSpawnEvent` | An NPC's entity has been spawned and is being tracked |
| `NpcInteractEvent` | A player right-clicked an `InteractableNpc`, before its own gate runs |

`NpcPreSpawnEvent` and `NpcInteractEvent` are cancellable. A cancelled spawn is retried on the next scheduler pass, so a listener hiding an NPC keeps cancelling for as long as that holds. `NpcInitializeEvent` fires while the entity is not yet in the world, so a listener may change its equipment or attributes but must not teleport or mount it.

---

## Resource Pack Events

| Event | Fired When |
|---|---|
| `ResourcePackApplyEvent` | A configured pack is about to be sent to a player, once per pack |

Cancellable. Cancelling skips only that pack, leaving the rest of the request intact.

---

## Damage Events

| Event | Fired When |
|---|---|
| `CustomPreDamageEvent` | Damage is about to be processed, before anything has been decided |
| `CustomDamageEvent` | The base is settled, for consuming plugins to set ability damage |
| `CustomPostDamageEvent` | The damage is final, for reductions and side effects |
| `WeaponReductionEvent` | The attacking item's damage contribution is being resolved |
| `ArmourReductionEvent` | A worn piece's armour value is being resolved, once per piece |
| `WeaponDurabilityEvent` | The attacker's item is about to spend durability |
| `ArmourDurabilityEvent` | A worn piece is about to spend durability, once per piece |
| `CustomKnockbackEvent` | Knockback has been calculated, before it is applied |

All are cancellable. Cancelling any of the three stages stops the chain there, so nothing downstream runs. Cancelling a reduction event excludes that item from the calculation entirely; cancelling a durability event spares that item; cancelling the knockback event means no knockback at all.

The three stages exist so the framework, consuming plugins and reductions each have a place that is not the others'. Writing damage at the pre stage would be overwritten by a weapon, and writing it at the post stage would miss armour.

---

## Death Events

| Event | Fired When |
|---|---|
| `CustomDeathEvent` | An entity has died, with the damage pass that killed it attached |
| `VanillaDeathEvent` | An entity has died and the damage system is not registered |
| `CustomDeathMessageEvent` | A death message is about to be sent to one player |

Neither death event is cancellable, since both are dispatched from inside the vanilla death once it is settled. Their drops, experience and death sound are still writable, and are written back to the vanilla death after dispatch. `CustomDeathMessageEvent` is cancellable, and is dispatched once per recipient, so a message can be suppressed or reworded for some players and not others.

---

## Effect Events

| Event | Fired When |
|---|---|
| `EffectPreAddEvent` | An effect is about to be applied to an entity |
| `EffectPostAddEvent` | An effect has been applied and its potion effect sent |
| `EffectPreUpdateEvent` | An entity's effect data is about to be mutated |
| `EffectPostUpdateEvent` | The data has been mutated and the potion effect reconciled |
| `EffectPreRemoveEvent` | An effect is about to be deliberately removed |
| `EffectPostRemoveEvent` | An effect has been removed and its potion effect cleared |
| `EffectExpireEvent` | An effect's duration has elapsed |
| `EffectTickEvent` | Every tick, for each live holder that has not expired |

The three pre events are cancellable. Cancelling an add leaves the effect unapplied, cancelling an update means the consumer never runs, and cancelling a remove leaves the effect in place. `EffectPreRemoveEvent` only fires for a deliberate removal, so expiry, death, a disconnect and a conditional removal cannot be refused.

Expiry reports through `EffectExpireEvent` in place of `EffectPostRemoveEvent`, so a listener wanting both listens to each. `EffectTickEvent` fires for every holder of every effect on each pass, so a listener on it should stay proportionate to that.

---

## Chat Events

| Event | Fired When |
|---|---|
| `ChatChannelEvent` | A message is about to be sent, to resolve which channel it goes in |
| `ChatSendEvent` | A message is about to be sent in its channel, before anyone receives it |
| `ChatReceiveEvent` | One recipient is about to receive a message |
| `ChatSwitchChannelEvent` | A player is switching channels, dispatched by the plugin |

All but `ChatChannelEvent` are cancellable. Cancelling a send refuses the message for everyone, cancelling a receive hides it from that recipient alone, and cancelling a switch keeps the player in their current channel. `ChatChannelEvent` is not, since every message needs a channel; the last listener to set one wins.

---

## Interfaces

| Interface | Description |
|---|---|
| `SpigotPlugin` | Root plugin with automatic Bukkit registration callbacks |
| `Node` | Typed parent access for commands and subcommands (provided by Hierarchy-Framework) |
| `SharedBaseCommand` | Shared contract between commands and subcommands: sender validation, permission, execution, and tab-complete |
| `IBaseCommand` | Command contract with subcommand management |
| `ICustomCancellableEvent` | Cancellable event with reason support |
| `DeathEvent` | Shared contract of both death events: entity, killer, cause, reason, drops, experience and death sound |
| `InteractableNpc` | Opts an NPC into right-click handling, with its own gate |
| `DamageableNpc` | Opts an NPC into damage, death and respawn handling |
| `SystemTimeMixin` | Carries a timestamp of when something was created or started |
| `DurationMixin` | Carries a duration, with `-1` meaning permanent |
| `ExpiredMixin` | Reports whether a duration has elapsed |
| `RemainingMixin` | Reports how much of a duration is left |
