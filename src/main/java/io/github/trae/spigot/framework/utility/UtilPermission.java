package io.github.trae.spigot.framework.utility;

import io.github.trae.utilities.UtilString;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;

/**
 * Provides utility methods for checking Bukkit permissions with support for
 * operator status and global wildcard permissions.
 *
 * <p>A permissible is considered to have a permission when any of the following
 * conditions are satisfied:</p>
 *
 * <ul>
 *     <li>The requested permission is empty.</li>
 *     <li>The permissible is an operator.</li>
 *     <li>The permissible has the global {@code *} permission.</li>
 *     <li>The permissible has the configured custom wildcard permission.</li>
 *     <li>The permissible directly has the requested permission.</li>
 * </ul>
 */
@UtilityClass
public class UtilPermission {

    /**
     * Additional wildcard node treated as granting every permission, such as a server admin
     * node. Defaults to {@code *}.
     */
    @Getter
    @Setter
    private static String customWildcardPermission = "*";

    /**
     * Determines whether a permissible has access to the specified permission.
     *
     * <p>An empty permission is always permitted. Otherwise, access is granted
     * when the permissible is an operator, has the global {@code *} permission,
     * has the configured custom wildcard permission, or directly has the
     * requested permission.</p>
     *
     * @param permissible the permissible to check
     * @param permission  the permission to check
     * @return {@code true} if access is permitted, otherwise {@code false}
     */
    public static boolean hasPermission(final Permissible permissible, final String permission) {
        if (UtilString.isEmpty(permission)) {
            return true;
        }

        if (permissible != null) {
            if (permissible.isOp()) {
                return true;
            }

            if (permissible.hasPermission("*")) {
                return true;
            }

            if (permissible.hasPermission(customWildcardPermission)) {
                return true;
            }

            if (permissible.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }
}