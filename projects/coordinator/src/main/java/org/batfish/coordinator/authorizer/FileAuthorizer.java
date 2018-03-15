package org.batfish.coordinator.authorizer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.coordinator.Main;
import org.batfish.coordinator.config.Settings;

/**
 * An {@link Authorizer} backed by JSON files on a local file system.
 *
 * <p>Primarily used for testing.
 */
public class FileAuthorizer implements Authorizer {

  static class Permission {
    public String apikey;
    public String container;
  }

  static class Permissions {
    public List<Permission> perms;
  }

  static class User {
    public String apikey;
  }

  static class Users {
    public List<User> users;
  }

  private BatfishLogger _logger;
  private Path _permsFile;
  private Path _usersFile;

  /**
   * Creates a new {@link FileAuthorizer} using the given {@link Settings} to determine the
   * directory and file names from which to read the user and permissions files.
   */
  public static FileAuthorizer createFromSettings(Settings settings) {
    try {
      return new FileAuthorizer(
          settings.getFileAuthorizerRootDir().resolve(settings.getFileAuthorizerUsersFile()),
          settings.getFileAuthorizerRootDir().resolve(settings.getFileAuthorizerPermsFile()),
          Main.getLogger());
    } catch (Exception e) {
      throw new BatfishException(
          String.format(
              "Could not initialize FileAuthorizer with RootDir = %s UsersFile=%s. PermsFile=%s",
              settings.getFileAuthorizerRootDir(),
              settings.getFileAuthorizerUsersFile(),
              settings.getFileAuthorizerPermsFile()),
          e);
    }
  }

  @Override
  public synchronized void authorizeContainer(String apiKey, String containerName) {
    _logger.infof("Authorizing %s for %s\n", apiKey, containerName);

    Permissions perms = loadPermissions();
    if (accessAllowed(apiKey, containerName, perms)) {
      _logger.infof("Authorizer: %s is already allowed to access %s\n", apiKey, containerName);
      return;
    }

    // Add the new permissions to the list and save them back to disk.
    Permission newPermission = new Permission();
    newPermission.apikey = apiKey;
    newPermission.container = containerName;
    perms.perms.add(newPermission);

    savePermissions(perms);
  }

  @Override
  public boolean isAccessibleContainer(String apiKey, String containerName, boolean logError) {
    Permissions allPerms = loadPermissions();
    boolean allowed = accessAllowed(apiKey, containerName, allPerms);
    if (!allowed && logError) {
      _logger.infof("Authorizer: %s is NOT allowed to access %s\n", apiKey, containerName);
    }
    return allowed;
  }

  @Override
  public boolean isValidWorkApiKey(String apiKey) {
    Users allUsers = loadUsers();
    boolean validUser = allUsers.users.stream().anyMatch(u -> apiKey.equals(u.apikey));
    _logger.infof("Authorizer: %s is %s valid API key\n", apiKey, validUser ? "a" : "NOT a");
    return validUser;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(FileAuthorizer.class)
        .add("users", _usersFile)
        .add("perms", _permsFile)
        .toString();
  }

  // Visible for testing.
  FileAuthorizer(Path usersFile, Path permsFile, BatfishLogger logger)
      throws FileNotFoundException {
    _logger = checkNotNull(logger, "logger should not be null");

    if (!Files.exists(usersFile)) {
      throw new FileNotFoundException("Users file not found: '" + usersFile.toAbsolutePath() + "'");
    }
    this._usersFile = usersFile;

    if (!Files.exists(permsFile)) {
      throw new FileNotFoundException("Perms file not found: '" + permsFile.toAbsolutePath() + "'");
    }
    this._permsFile = permsFile;
  }

  private static boolean accessAllowed(String apiKey, String container, Permissions permissions) {
    return permissions
        .perms
        .stream()
        .anyMatch(p -> apiKey.equals(p.apikey) && container.equals(p.container));
  }

  private Permissions loadPermissions() {
    try {
      return BatfishObjectMapper.mapper().readValue(_permsFile.toFile(), Permissions.class);
    } catch (IOException e) {
      throw new BatfishException(
          String.format("Error loading permissions from '%s'", _permsFile.toAbsolutePath()), e);
    }
  }

  private void savePermissions(Permissions p) {
    try {
      BatfishObjectMapper.prettyWriter().writeValue(_permsFile.toFile(), p);
    } catch (IOException e) {
      throw new BatfishException(
          String.format("Error saving permissions to '%s'", _permsFile.toAbsolutePath()), e);
    }
  }

  private Users loadUsers() {
    try {
      return BatfishObjectMapper.mapper().readValue(_usersFile.toFile(), Users.class);
    } catch (IOException e) {
      throw new BatfishException(
          String.format("Error loading users from '%s'", _usersFile.toAbsolutePath()), e);
    }
  }
}
