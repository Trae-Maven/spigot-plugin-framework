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

There are two levels. `Item` is the plain description, producing stacks with no identity, suited to transient things such as window icons. `CustomItem` stamps an identifier and a version hash onto every stack it produces, so the stack can be recognised later and replaced when the definition changes.

Requires `@Scan("io.github.trae.spigot.framework.item")`.

### Defining a Plain Item

Extend `Item` when the stack is throwaway and never needs to be recognised again:

```java
public class BackIcon extends Item {

    public BackIcon() {
        super(Material.RED_WOOL);
    }

    @Override
    protected Color getColor() {
        return ChatColor.RED.getColor();
    }

    @Override
    protected String getDisplayName() {
        return "Back";
    }

    @Override
    protected List<String> getLore() {
        return List.of("Return to the previous window.");
    }
}
```

```java
final ItemStack itemStack = new BackIcon().create();
```

### Defining a Custom Item

Extend `CustomItem` and register it as a component. `ItemApplyListener` discovers every subclass through the dependency injector at server load and registers it under its identifier:

```java
@Singleton
public class MinersPickaxe extends CustomItem {

    public MinersPickaxe() {
        super(Material.IRON_PICKAXE, "miners_pickaxe");
    }

    @Override
    protected Color getColor() {
        return ChatColor.AQUA.getColor();
    }

    @Override
    protected String getDisplayName() {
        return "Miner's Pickaxe";
    }

    @Override
    protected List<String> getLore() {
        return List.of(
                "Mines a little faster than it should.",
                "",
                "Right-Click to toggle vein mining."
        );
    }

    @Override
    protected NamespacedKey getModel() {
        return new NamespacedKey("custom", "miners_pickaxe");
    }
}
```

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
```

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

### Naturally Obtainable Items

An item declaring `naturallyObtainable()` is registered under its material as well as its identifier. Any vanilla stack of that material a player mines, crafts, smelts, or picks up is converted into the custom item automatically:

```java
@Singleton
public class RawIron extends CustomItem {

    public RawIron() {
        super(Material.RAW_IRON, "raw_iron");
    }

    @Override
    protected boolean naturallyObtainable() {
        return true;
    }

    @Override
    protected Color getColor() {
        return ChatColor.WHITE.getColor();
    }

    @Override
    protected String getDisplayName() {
        return "Raw Iron";
    }

    @Override
    protected List<String> getLore() {
        return List.of("Smelt in a furnace to refine.");
    }
}
```

Only one item may claim a given material. Registering two throws at server load.

### Activatable Items

An item implementing `Activatable` gains a click action. `ItemActivateListener` resolves the item behind the clicked stack and calls `onActivate` once the click has passed the item's own gate and the cancellable `ItemPreActivateEvent`:

```java
@Singleton
public class MinersPickaxe extends CustomItem implements Activatable {

    public MinersPickaxe() {
        super(Material.IRON_PICKAXE, "miners_pickaxe");
    }

    @Override
    public void onActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
        if (activateType != ActivateType.RIGHT_CLICK) {
            return;
        }

        UtilMessage.message(player, "Items", "Vein mining <green>enabled</green>.");
    }

    @Override
    protected Color getColor() {
        return ChatColor.AQUA.getColor();
    }

    @Override
    protected String getDisplayName() {
        return "Miner's Pickaxe";
    }

    @Override
    protected List<String> getLore() {
        return List.of(
                "Mines a little faster than it should.",
                "",
                "Right-Click to toggle vein mining."
        );
    }
}
```

The capability is opt-in per item rather than a hook every custom item overrides, so an item that does not implement the interface is never invoked.

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

`canActivate` is the item-level check, evaluated before the pre-activate event, for conditions the item itself owns such as a cooldown or a durability threshold:

```java
@Override
public boolean canActivate(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    if (activateType != ActivateType.RIGHT_CLICK) {
        return false;
    }

    return !this.cooldownManager.hasCooldown(player, "Vein Mine");
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

`ItemPostActivateEvent` fires after a successful activation, for recording a cooldown or a statistic. It is not cancellable, and never fires for an activation that was refused.

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
public Event.Result useInteractedBlock(final Player player, final ItemStack itemStack, final ActivateType activateType) {
    return Event.Result.DENY;
}
```

Both default to `Event.Result.DEFAULT`, leaving vanilla behaviour untouched.

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
this.itemManager.updatePlayerInventory(player);

// Look an item up
this.itemManager.getItemByIdentifier("miners_pickaxe").ifPresent(item -> player.getInventory().addItem(item.create()));
```

`apply` returns the input reference untouched when nothing changed, so callers can skip a write with an identity comparison.

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
        this.addButton(new StatsButton(11, this.accountManager, player));
        this.addButton(new SettingsButton(15, this));
    }
}
```

### Defining a Button

Extend `Button` with the slot it occupies. `getItemStack` is resolved on every refresh, so a button whose appearance depends on changing state simply returns a different stack next time the window redraws:

```java
public class SettingsButton extends Button {

    private final Window parentWindow;

    public SettingsButton(final int slot, final Window parentWindow) {
        super(slot);

        this.parentWindow = parentWindow;
    }

    @Override
    protected ItemStack getItemStack() {
        return new SettingsIcon().create();
    }

    @Override
    protected boolean canClick(final Player player, final ClickType clickType) {
        return clickType.isLeftClick();
    }

    @Override
    protected void onClick(final Player player, final ClickType clickType) {
        UtilWindow.open(player, new SettingsWindow(this.parentWindow));
    }
}
```

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
protected void onClick(final Player player, final ClickType clickType) {
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

`canOpen`, `canClose`, and `canClick` are the window-level checks, for conditions the window or button itself owns. `WindowOpenEvent`, `WindowCloseEvent`, and `ButtonClickEvent` are the system-level equivalents, for conditions external to it, such as a world restriction or a global lockdown.

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

### Click Cooldowns

`WindowManager` exposes `addCooldown` and `hasCooldown` as no-op hooks, so button clicks are unthrottled by default. A plugin wanting rate-limiting subclasses the manager and implements them. The cooldown is only recorded for a click that actually ran, so a refused click does not throttle the next attempt.

### Querying

```java
// What is this player looking at
this.windowManager.getWindowByPlayer(player).ifPresent(window -> window.refresh());

// Which window owns this inventory
this.windowManager.getWindowByInventory(inventory);
```

Click dispatch never consults these maps. A window is its own `InventoryHolder`, so a click resolves straight off the event and a momentarily stale map can never misroute one.

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
        super("hub", 10);

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
        super("factions", 0); // wins over HubSidebar at 10

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
UtilEvent.dispatch(new SidebarUpdateEvent("hub", player));
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
        super("rank", 10); // fallback

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
        super("factions", 0); // wins over RankTeam

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
| `ItemPreActivateEvent` | A player activated an item, before the item's action runs |
| `ItemPostActivateEvent` | An item's activation has run |

`ItemPreActivateEvent` is cancellable, and cancelling suppresses the activation entirely. `ItemPostActivateEvent` is not, since the action has already happened, and only fires for an activation that actually ran.

---

## Window Events

| Event | Fired When |
|---|---|
| `WindowOpenEvent` | A window is about to be rendered and shown to a player |
| `WindowCloseEvent` | A player closed a window, before its tracking entries are dropped |
| `ButtonClickEvent` | A player clicked a button, before the button's action runs |

All three are cancellable. Cancelling an open aborts it, cancelling a close re-opens the window a tick later, and cancelling a click suppresses the button's action.

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
