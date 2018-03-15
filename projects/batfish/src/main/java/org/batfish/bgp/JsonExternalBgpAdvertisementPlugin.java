package org.batfish.bgp;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.plugin.ExternalBgpAdvertisementPlugin;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.BgpAdvertisement;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@AutoService(Plugin.class)
public class JsonExternalBgpAdvertisementPlugin extends ExternalBgpAdvertisementPlugin {

  @Override
  protected void externalBgpAdvertisementPluginInitialize() {}

  @Override
  public Set<BgpAdvertisement> loadExternalBgpAdvertisements() {
    Set<BgpAdvertisement> advertSet = new LinkedHashSet<>();
    String externalBgpAnnouncementsFileContents = _batfish.readExternalBgpAnnouncementsFile();
    if (externalBgpAnnouncementsFileContents != null) {
      // Populate advertSet with BgpAdvertisements that
      // gets passed to populatePrecomputedBgpAdvertisements.
      // See populatePrecomputedBgpAdvertisements for the things that get
      // extracted from these advertisements.

      try {
        JSONObject jsonObj = new JSONObject(externalBgpAnnouncementsFileContents);

        JSONArray announcements = jsonObj.getJSONArray(BfConsts.PROP_BGP_ANNOUNCEMENTS);

        for (int index = 0; index < announcements.length(); index++) {
          JSONObject announcement = new JSONObject();
          JSONObject announcementSrc = announcements.getJSONObject(index);
          for (Iterator<?> i = announcementSrc.keys(); i.hasNext(); ) {
            String key = (String) i.next();
            announcement.put(key, announcementSrc.get(key));
          }
          BgpAdvertisement bgpAdvertisement =
              BatfishObjectMapper.mapper()
                  .readValue(announcement.toString(), BgpAdvertisement.class);
          advertSet.add(bgpAdvertisement);
        }

      } catch (JSONException | IOException e) {
        throw new BatfishException("Error processing external BGP advertisements file", e);
      }
    }
    return advertSet;
  }
}
