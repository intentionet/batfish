package org.batfish.coordinator.authorizer;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.util.CommonUtil;
import org.batfish.coordinator.Main;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// An authorizer that is backed by a file
// Useful for testing
public class FileAuthorizer implements Authorizer {

  private static final String APIKEY_KEY = "apikey";
  private static final String CONTAINER_KEY = "container";
  private static final String PERMS_KEY = "perms";
  private static final String USERS_KEY = "users";

  private BatfishLogger _logger;
  private Path _permsFile;
  private Path _usersFile;

  public FileAuthorizer() {
    _logger = Main.getLogger();
    try {
      _usersFile =
          Main.getSettings()
              .getFileAuthorizerRootDir()
              .resolve(Main.getSettings().getFileAuthorizerUsersFile());
      _permsFile =
          Main.getSettings()
              .getFileAuthorizerRootDir()
              .resolve(Main.getSettings().getFileAuthorizerPermsFile());
      if (!Files.exists(_usersFile)) {
        throw new FileNotFoundException(
            "Users file not found: '" + _usersFile.toAbsolutePath() + "'");
      }
      if (!Files.exists(_permsFile)) {
        throw new FileNotFoundException(
            "Perms file not found: '" + _permsFile.toAbsolutePath() + "'");
      }
    } catch (Exception e) {
      throw new BatfishException(
          String.format(
              "Could not initialize FileAuthorizer with "
                  + " RootDir = %s UsersFile=%s. PermsFile=%s",
              Main.getSettings().getFileAuthorizerRootDir(),
              Main.getSettings().getFileAuthorizerUsersFile(),
              Main.getSettings().getFileAuthorizerPermsFile()),
          e);
    }
  }

  @Override
  public synchronized void authorizeContainer(String apiKey, String containerName) {
    _logger.infof("Authorizing %s for %s\n", apiKey, containerName);
    String allPerms = CommonUtil.readFile(_permsFile);
    String newAllPerms;
    try {
      JSONObject jObj = new JSONObject(allPerms);
      if (!jObj.has(PERMS_KEY)) {
        throw new BatfishException("Do not understand the format of perms file");
      }
      JSONArray permsArray = jObj.getJSONArray(PERMS_KEY);
      if (doPermsExist(apiKey, containerName, permsArray)) {
        _logger.infof("Authorizer: %s is already allowed to access %s\n", apiKey, containerName);
        return;
      }
      JSONObject jPermsObj = new JSONObject();
      jPermsObj.put(APIKEY_KEY, apiKey);
      jPermsObj.put(CONTAINER_KEY, containerName);
      permsArray.put(jPermsObj);
      jObj.put("perms", permsArray);
      newAllPerms = jObj.toString();
    } catch (JSONException e) {
      throw new BatfishException("Could not update JSON permissions object", e);
    }
    CommonUtil.writeFile(_permsFile, newAllPerms);
  }

  private boolean doPermsExist(String apiKey, String containerName, JSONArray permsArray)
      throws JSONException {
    for (int index = 0; index < permsArray.length(); index++) {
      JSONObject jPermsObj = permsArray.getJSONObject(index);
      if (apiKey.equals(jPermsObj.getString(APIKEY_KEY))
          && containerName.equals(jPermsObj.getString(CONTAINER_KEY))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAccessibleContainer(String apiKey, String containerName, boolean logError) {
    String allPerms = CommonUtil.readFile(_permsFile);
    try {
      JSONObject jObj = new JSONObject(allPerms);
      if (!jObj.has(PERMS_KEY)) {
        throw new BatfishException("Do not understand the format of perms file");
      }
      JSONArray permsArray = jObj.getJSONArray(PERMS_KEY);
      if (doPermsExist(apiKey, containerName, permsArray)) {
        _logger.infof("Authorizer: %s is allowed to access %s\n", apiKey, containerName);
        return true;
      }
    } catch (JSONException e) {
      throw new BatfishException("Could not process perms JSON object", e);
    }
    if (logError) {
      _logger.infof("Authorizer: %s is NOT allowed to access %s\n", apiKey, containerName);
    }
    return false;
  }

  @Override
  public boolean isValidWorkApiKey(String apiKey) {
    String allUsers = CommonUtil.readFile(_usersFile);
    try {
      JSONObject jObj = new JSONObject(allUsers);
      if (!jObj.has(USERS_KEY)) {
        throw new BatfishException("Do not understand the format of users file");
      }
      JSONArray usersArray = jObj.getJSONArray(USERS_KEY);
      for (int index = 0; index < usersArray.length(); index++) {
        JSONObject jUserObj = usersArray.getJSONObject(index);
        if (apiKey.equals(jUserObj.getString(APIKEY_KEY))) {
          _logger.infof("Authorizer: %s is a valid key\n", apiKey);
          return true;
        }
      }
    } catch (JSONException e) {
      throw new BatfishException("Could not process users JSON object", e);
    }
    _logger.infof("Authorizer: %s is NOT a valid key\n", apiKey);
    return false;
  }
}
