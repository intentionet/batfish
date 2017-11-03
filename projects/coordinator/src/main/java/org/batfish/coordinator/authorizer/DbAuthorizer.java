package org.batfish.coordinator.authorizer;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.util.CommonUtil;
import org.batfish.coordinator.Main;

// An authorizer that is backed by a file
// Useful for testing
public class DbAuthorizer implements Authorizer {

  private static final String COLUMN_APIKEY = "APIKey";
  private static final String COLUMN_CONTAINER_NAME = "ContainerName";
  private static final String COLUMN_DATE_CREATED = "DateCreated";
  private static final String COLUMN_DATE_LAST_ACCESSED = "DateLastAccessed";

  private static final int DB_VALID_CHECK_TIMEOUT_SECS = 3;
  private static final int MAX_DB_TRIES = 3;

  private static final String TABLE_CONTAINERS = "containers";
  private static final String TABLE_PERMISSIONS = "containerpermissions";
  private static final String TABLE_USERS = "members";

  private Map<String, Date> _cacheApiKeys = new HashMap<>();
  private Map<String, Date> _cachePermissions = new HashMap<>();

  private Connection _dbConn;
  private BatfishLogger _logger;

  public DbAuthorizer() throws SQLException {
    _logger = Main.getLogger();
    openDbConnection();
  }

  @Override
  public void authorizeContainer(String apiKey, String containerName) {
    _logger.infof("Authorizing %s for %s\n", apiKey, containerName);

    // add the container if it does not exist; update datelastaccessed if it
    // does
    java.sql.Date now = new java.sql.Date(new Date().getTime());
    String insertContainersString =
        String.format(
            "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?) " + " ON DUPLICATE KEY UPDATE %s = ?",
            TABLE_CONTAINERS,
            COLUMN_CONTAINER_NAME,
            COLUMN_DATE_CREATED,
            COLUMN_DATE_LAST_ACCESSED,
            COLUMN_DATE_LAST_ACCESSED);
    PreparedStatement insertContainers;
    int numInsertRows;
    try {
      insertContainers = _dbConn.prepareStatement(insertContainersString);
      insertContainers.setString(1, containerName);
      insertContainers.setDate(2, now);
      insertContainers.setDate(3, now);
      insertContainers.setDate(4, now);
      numInsertRows = executeUpdate(insertContainers);
    } catch (SQLException e) {
      throw new BatfishException("Could not update containers table", e);
    }

    // MySQL says 2 rows impacted when an old row is updated; otherwise it
    // says 1
    if (numInsertRows == 1) {
      _logger.infof("New container added\n");
    }

    // return if already accessible
    if (isAccessibleContainer(apiKey, containerName, false)) {
      return;
    }
    int numRows;
    String insertPermissionsString =
        String.format(
            "INSERT INTO %s (%s, %s) VALUES (?, ?)",
            TABLE_PERMISSIONS, COLUMN_APIKEY, COLUMN_CONTAINER_NAME);
    try {
      PreparedStatement insertPermissions = _dbConn.prepareStatement(insertPermissionsString);
      insertPermissions.setString(1, apiKey);
      insertPermissions.setString(2, containerName);
      numRows = executeUpdate(insertPermissions);
    } catch (SQLException e) {
      throw new BatfishException("Could not update permissions table", e);
    }
    if (numRows > 0) {
      String cacheKey = getPermsCacheKey(apiKey, containerName);
      insertInCache(_cachePermissions, cacheKey);
      _logger.infof("Authorization successful\n");
    }
  }

  @Nullable
  private synchronized ResultSet executeQuery(PreparedStatement query) {
    int triesLeft = MAX_DB_TRIES;
    String queryString = query.toString();
    while (triesLeft > 0) {
      triesLeft--;
      try {
        _logger.debugf("Executing SQL query: %s\n", queryString);
        return query.executeQuery();
      } catch (SQLException e) {
        _logger.errorf("SQLException while executing query '%s': %s", queryString, e.getMessage());
        _logger.errorf("Tries left = %d\n", triesLeft);

        if (triesLeft > 0) {
          try {
            if (!_dbConn.isValid(DB_VALID_CHECK_TIMEOUT_SECS)) {
              openDbConnection();
            }
          } catch (SQLException e1) {
            return null;
          }
        }
      } catch (Exception e) {
        throw new BatfishException("well shit");
      }
    }

    return null;
  }

  private synchronized int executeUpdate(PreparedStatement update) {
    int triesLeft = MAX_DB_TRIES;
    String updateString = update.toString();
    while (triesLeft > 0) {
      triesLeft--;
      try {
        _logger.debugf("Executing SQL update: %s\n", updateString);
        return update.executeUpdate();
      } catch (SQLException e) {
        _logger.errorf("SQLException while executing query '%s': %s", updateString, e.getMessage());
        _logger.errorf("Tries left = %d\n", triesLeft);

        if (triesLeft > 0) {
          try {
            if (!_dbConn.isValid(DB_VALID_CHECK_TIMEOUT_SECS)) {
              openDbConnection();
            }
          } catch (SQLException e1) {
            return 0;
          }
        }
      }
    }
    return 0;
  }

  private String getPermsCacheKey(String apiKey, String containerName) {
    return apiKey + "::" + containerName;
  }

  private synchronized void insertInCache(Map<String, Date> cache, String key) {
    cache.put(key, new Date());
  }

  @Override
  public boolean isAccessibleContainer(String apiKey, String containerName, boolean logError) {

    String cacheKey = getPermsCacheKey(apiKey, containerName);

    boolean validInCache = isValidInCache(_cachePermissions, cacheKey);

    if (validInCache) {
      return true;
    }

    String selectPermittedContainerString =
        String.format(
            "SELECT * FROM %s WHERE %s = ? AND %s = ?",
            TABLE_PERMISSIONS, COLUMN_APIKEY, COLUMN_CONTAINER_NAME);
    ResultSet rs;
    try {
      PreparedStatement ps = _dbConn.prepareStatement(selectPermittedContainerString);
      ps.setString(1, apiKey);
      ps.setString(2, containerName);
      rs = executeQuery(ps);
    } catch (SQLException e) {
      throw new BatfishException("Could not query permissions table", e);
    }
    boolean authorized;
    try {
      authorized = rs != null && rs.first();
    } catch (SQLException e) {
      throw new BatfishException("Error examinng permissions query result set", e);
    }
    if (authorized) {
      insertInCache(_cachePermissions, cacheKey);

      // a valid access was made; update datelastaccessed
      java.sql.Date now = new java.sql.Date(new Date().getTime());
      String updatePsString =
          String.format(
              "UPDATE %s SET %s=? WHERE %s=?",
              TABLE_CONTAINERS, COLUMN_DATE_LAST_ACCESSED, COLUMN_CONTAINER_NAME);
      try {
        PreparedStatement updatePs = _dbConn.prepareStatement(updatePsString);
        updatePs.setDate(1, now);
        updatePs.setString(2, containerName);
        executeUpdate(updatePs);
      } catch (SQLException e) {
        throw new BatfishException("Could not update containers table", e);
      }
      return true;
    }

    if (logError) {
      _logger.infof("Authorizer: %s is NOT allowed to access %s\n", apiKey, containerName);
    }

    return false;
  }

  private synchronized boolean isValidInCache(Map<String, Date> cache, String key) {
    if (cache.containsKey(key)) {
      // check if the entry is expired
      if (new Date().getTime() - cache.get(key).getTime()
          > Main.getSettings().getDbAuthorizerCacheExpiryMs()) {
        cache.remove(key);
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean isValidWorkApiKey(String apiKey) {
    boolean validInCache = isValidInCache(_cacheApiKeys, apiKey);
    if (validInCache) {
      return true;
    }
    String selectApiKeyRowString =
        String.format("SELECT * FROM %s WHERE %s = ?", TABLE_USERS, COLUMN_APIKEY);
    PreparedStatement ps;
    ResultSet rs;
    try {
      ps = _dbConn.prepareStatement(selectApiKeyRowString);
      ps.setString(1, apiKey);
      rs = executeQuery(ps);
    } catch (SQLException e) {
      throw new BatfishException("Could not query users table", e);
    }
    boolean authorized;
    try {
      authorized = rs != null && rs.first();
    } catch (SQLException e) {
      throw new BatfishException("Could not example users query result set", e);
    }
    if (authorized) {
      insertInCache(_cacheApiKeys, apiKey);
      return true;
    }
    _logger.infof("Authorizer: %s is NOT a valid key\n", apiKey);
    return false;
  }

  private synchronized void openDbConnection() throws SQLException {
    int triesLeft = MAX_DB_TRIES;

    String driverClassName = Main.getSettings().getDriverClass();
    while (triesLeft > 0) {
      triesLeft--;
      try {
        if (_dbConn != null) {
          _dbConn.close();
        }
        if (driverClassName != null) {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          cl.loadClass(driverClassName);
          Class.forName(driverClassName, true, cl);
        }
        _dbConn = DriverManager.getConnection(Main.getSettings().getDbAuthorizerConnString());
        return;
      } catch (SQLException e) {
        if (CommonUtil.causedByMessage(e, "Access denied for user")
            || CommonUtil.causedByMessage(e, "No suitable driver found")
            || CommonUtil.causedByMessage(e, "Unknown database")
            || CommonUtil.causedBy(e, UnknownHostException.class)
            || CommonUtil.causedBy(e, ConnectException.class)) {
          throw new BatfishException(
              "Unrecoverable SQLException loading JDBC driver: " + driverClassName, e);
        }
        if (triesLeft == 0) {
          throw new BatfishException("No tries left loading JDBC driver: " + driverClassName);
        }
        _logger.errorf(
            "SQLException while opening Db connection: %s\n", ExceptionUtils.getStackTrace(e));
        _logger.errorf("Tries left = %d\n", triesLeft);

      } catch (ClassNotFoundException e) {
        throw new BatfishException("JDBC driver class not found: " + driverClassName, e);
      }
    }
  }
}
