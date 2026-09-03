package io.github.trae.spigot.framework.utility;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;

import java.util.stream.Stream;

/**
 * Utility helpers for permission checks that honour wildcards and operator status.
 *
 * <p>Bukkit's own {@link Permissible#hasPermission(String)} only matches the node it is given, so a
 * holder of a wildcard node is not granted everything beneath it unless the permission plugin
 * expands it. These helpers close that gap by also testing the global {@code *} node and a
 * configurable custom wildcard, and by treating operators as permitted.</p>
 */
@UtilityClass
public class UtilPermission {

    /**
     * Additional wildcard node treated as granting every permission, such as a network-wide admin
     * node. Defaults to {@code *}.
     */
    @Getter
    @Setter
    private static String customWildcardPermission = "*";

    /**
     * Checks whether the given holder has the specified permission.
     *
     * <p>Returns true if the holder is an operator, holds the global {@code *} node, holds the
     * configured custom wildcard, or holds the permission itself.</p>
     *
     * @param permissible the permission holder to test, such as a player or console sender
     * @param permission  the permission node to test
     * @return true if the holder is permitted
     */
    public static boolean hasPermission(final Permissible permissible, final String permission) {
        return permissible.isOp() || Stream.of("*", customWildcardPermission, permission).anyMatch(permissible::hasPermission);
    }
}