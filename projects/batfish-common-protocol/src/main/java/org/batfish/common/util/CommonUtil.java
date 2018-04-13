package org.batfish.common.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.MustBeClosed;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.util.GlobalTracer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.Pair;
import org.batfish.common.plugin.FlowProcessor;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpLink;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfNeighbor;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.skyscreamer.jsonassert.JSONAssert;

public class CommonUtil {

  public static <M extends Map<?, ?>> M nullIfEmpty(M map) {
    return map == null ? null : map.isEmpty() ? null : map;
  }

  public static <C extends Collection<?>> C nullIfEmpty(C collection) {
    return collection == null ? null : collection.isEmpty() ? null : collection;
  }

  private static class TrustAllHostNameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  }

  private static String SALT;

  private static final int STREAMED_FILE_BUFFER_SIZE = 1024;

  public static String applyPrefix(String prefix, String msg) {
    String[] lines = msg.split("\n", -1);
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(prefix + line + "\n");
    }
    return sb.toString();
  }

  public static <T extends Throwable> boolean causedBy(Throwable e, Class<T> causeClass) {
    Set<Throwable> seenCauses = Collections.newSetFromMap(new IdentityHashMap<>());
    return causedBy(e, causeClass, seenCauses);
  }

  private static <T extends Throwable> boolean causedBy(
      Throwable e, Class<T> causeClass, Set<Throwable> seenCauses) {
    if (seenCauses.contains(e)) {
      return false;
    }
    seenCauses.add(e);
    if (causeClass.isInstance(e)) {
      return true;
    } else {
      Throwable cause = e.getCause();
      if (cause != null) {
        return causedBy(cause, causeClass, seenCauses);
      } else {
        return false;
      }
    }
  }

  public static boolean causedByMessage(Throwable e, String searchTerm) {
    Set<Throwable> seenCauses = Collections.newSetFromMap(new IdentityHashMap<>());
    return causedByMessage(e, searchTerm, seenCauses);
  }

  private static boolean causedByMessage(
      Throwable e, String searchTerm, Set<Throwable> seenCauses) {
    if (seenCauses.contains(e)) {
      return false;
    }
    seenCauses.add(e);
    if (e.getMessage().contains(searchTerm)) {
      return true;
    } else {
      Throwable cause = e.getCause();
      if (cause != null) {
        return causedByMessage(cause, searchTerm, seenCauses);
      } else {
        return false;
      }
    }
  }

  public static boolean checkJsonEqual(Object a, Object b) {
    try {
      String aString = BatfishObjectMapper.writePrettyString(a);
      String bString = BatfishObjectMapper.writePrettyString(b);
      JSONAssert.assertEquals(aString, bString, false);
      return true;
    } catch (Exception e) {
      throw new BatfishException("JSON equality check failed", e);
    } catch (AssertionError err) {
      return false;
    }
  }

  public static long communityStringToLong(String str) {
    String[] parts = str.split(":");
    long high = Long.parseLong(parts[0]);
    long low = Long.parseLong(parts[1]);
    long result = low + (high << 16);
    return result;
  }

  public static <C extends Comparable<? super C>> int compareIterable(
      Iterable<C> lhs, Iterable<C> rhs) {
    return Comparators.lexicographical(Ordering.<C>natural()).compare(lhs, rhs);
  }

  public static <T extends Comparable<T>> int compareCollection(
      Collection<T> lhs, Collection<T> rhs) {
    Iterator<T> l = lhs.iterator();
    Iterator<T> r = rhs.iterator();
    while (l.hasNext()) {
      if (!r.hasNext()) {
        return 1;
      }
      T lVal = l.next();
      T rVal = r.next();
      int ret = lVal.compareTo(rVal);
      if (ret != 0) {
        return ret;
      }
    }
    if (r.hasNext()) {
      return -1;
    }
    return 0;
  }

  public static Map<Ip, Set<String>> computeIpOwners(
      Map<String, Configuration> configurations, boolean excludeInactive) {
    return computeIpOwners(
        excludeInactive,
        configurations
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getInterfaces())));
  }

  /**
   * Compute the owners of IP addresses
   *
   * @param excludeInactive whether to ignore inactive interfaces
   * @param enabledInterfaces A mapping of enabled interfaces hostname -> interface name -> {@link
   *     Interface}
   * @return A map of {@link Ip}s to a set of hostnames that own this IP
   */
  public static Map<Ip, Set<String>> computeIpOwners(
      boolean excludeInactive, Map<String, Map<String, Interface>> enabledInterfaces) {
    // TODO: confirm VRFs are handled correctly
    Map<Ip, Set<String>> ipOwners = new HashMap<>();
    Map<Pair<InterfaceAddress, Integer>, Set<Interface>> vrrpGroups = new HashMap<>();
    enabledInterfaces.forEach(
        (hostname, currentEnabledInterfaces) -> {
          for (Interface i : currentEnabledInterfaces.values()) {
            if (!i.getActive() && (excludeInactive || !i.getBlacklisted())) {
              continue;
            }
            // collect vrrp info
            i.getVrrpGroups()
                .forEach(
                    (groupNum, vrrpGroup) -> {
                      InterfaceAddress address = vrrpGroup.getVirtualAddress();
                      if (address == null) {
                        // This Vlan Interface has invalid configuration. The VRRP has no source
                        // IP address that would be used for VRRP election. This interface could
                        // never win the election, so is not a candidate.
                        return;
                      }
                      Pair<InterfaceAddress, Integer> key = new Pair<>(address, groupNum);
                      Set<Interface> candidates =
                          vrrpGroups.computeIfAbsent(
                              key, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
                      candidates.add(i);
                    });
            // collect prefixes
            i.getAllAddresses()
                .stream()
                .map(InterfaceAddress::getIp)
                .forEach(
                    ip -> {
                      Set<String> owners = ipOwners.computeIfAbsent(ip, k -> new HashSet<>());
                      owners.add(hostname);
                    });
          }
        });
    vrrpGroups.forEach(
        (p, candidates) -> {
          InterfaceAddress address = p.getFirst();
          int groupNum = p.getSecond();
          Set<String> owners = ipOwners.computeIfAbsent(address.getIp(), k -> new HashSet<>());
          /*
           * Compare priorities first. If tied, break tie based on highest interface IP.
           */
          Interface vrrpMaster =
              Collections.max(
                  candidates,
                  Comparator.comparingInt(
                          (Interface o) -> o.getVrrpGroups().get(groupNum).getPriority())
                      .thenComparing(o -> o.getAddress().getIp()));
          owners.add(vrrpMaster.getOwner().getHostname());
        });
    return ipOwners;
  }

  public static Map<Ip, String> computeIpOwnersSimple(Map<Ip, Set<String>> ipOwners) {
    Map<Ip, String> ipOwnersSimple = new HashMap<>();
    ipOwners.forEach(
        (ip, owners) -> {
          String hostname =
              owners.size() == 1 ? owners.iterator().next() : Route.AMBIGUOUS_NEXT_HOP;
          ipOwnersSimple.put(ip, hostname);
        });
    return ipOwnersSimple;
  }

  public static Map<Ip, String> computeIpOwnersSimple(
      Map<String, Configuration> configurations, boolean excludeInactive) {
    return computeIpOwnersSimple(computeIpOwners(configurations, excludeInactive));
  }

  public static void copy(Path srcPath, Path dstPath) {
    if (Files.isDirectory(srcPath)) {
      copyDirectory(srcPath, dstPath);
    } else {
      copyFile(srcPath, dstPath);
    }
  }

  public static void copyDirectory(Path srcPath, Path dstPath) {
    try {
      FileUtils.copyDirectory(srcPath.toFile(), dstPath.toFile());
    } catch (IOException e) {
      throw new BatfishException(
          "Failed to copy directory: '" + srcPath + "' to: '" + dstPath + "'", e);
    }
  }

  public static void copyFile(Path srcPath, Path dstPath) {
    try {
      FileUtils.copyFile(srcPath.toFile(), dstPath.toFile());
    } catch (IOException e) {
      throw new BatfishException("Failed to copy file: '" + srcPath + "' to: '" + dstPath + "'", e);
    }
  }

  public static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new BatfishException(
          "Could not create directories leading up to and including '" + path + "'", e);
    }
  }

  public static ClientBuilder createHttpClientBuilder(
      boolean noSsl,
      boolean trustAllSslCerts,
      Path keystoreFile,
      String keystorePassword,
      Path truststoreFile,
      String truststorePassword) {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    try {
      if (!noSsl) {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers;
        if (trustAllSslCerts) {
          trustManagers =
              new TrustManager[] {
                new X509TrustManager() {
                  @Override
                  public void checkClientTrusted(X509Certificate[] arg0, String arg1) {}

                  @Override
                  public void checkServerTrusted(X509Certificate[] arg0, String arg1) {}

                  @Override
                  public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                  }
                }
              };
          clientBuilder.hostnameVerifier(new TrustAllHostNameVerifier());
        } else if (truststoreFile != null) {
          TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
          KeyStore ts = KeyStore.getInstance("JKS");
          if (truststorePassword == null) {
            throw new BatfishException("Truststore file supplied but truststore password missing");
          }
          char[] tsPass = truststorePassword.toCharArray();
          try (FileInputStream trustInputStream = new FileInputStream(truststoreFile.toFile())) {
            ts.load(trustInputStream, tsPass);
          }
          tmf.init(ts);
          trustManagers = tmf.getTrustManagers();
        } else {
          trustManagers = null;
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyManager[] keyManagers;
        if (keystoreFile != null) {
          if (keystorePassword == null) {
            throw new BatfishException("Keystore file supplied but keystore password");
          }
          char[] ksPass = keystorePassword.toCharArray();
          try (FileInputStream keystoreStream = new FileInputStream(keystoreFile.toFile())) {
            ks.load(keystoreStream, ksPass);
          }
          kmf.init(ks, ksPass);
          keyManagers = kmf.getKeyManagers();
        } else {
          keyManagers = null;
        }
        sslcontext.init(keyManagers, trustManagers, new java.security.SecureRandom());
        clientBuilder.sslContext(sslcontext);
      }
      if (GlobalTracer.isRegistered()) {
        clientBuilder.register(ClientTracingFeature.class);
      }
    } catch (Exception e) {
      throw new BatfishException("Error creating HTTP client builder", e);
    }
    return clientBuilder;
  }

  public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) {
    try {
      Path tempDir = Files.createTempDirectory(prefix, attrs);
      tempDir.toFile().deleteOnExit();
      return tempDir;
    } catch (IOException e) {
      throw new BatfishException("Failed to create temporary directory", e);
    }
  }

  public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attributes) {
    try {
      Path tempFile = Files.createTempFile(prefix, suffix, attributes);
      tempFile.toFile().deleteOnExit();
      return tempFile;
    } catch (IOException e) {
      throw new BatfishException("Failed to create temporary file", e);
    }
  }

  public static void delete(Path path) {
    try {
      Files.delete(path);
    } catch (NoSuchFileException e) {
      throw new BatfishException("Cannot delete non-existent file: '" + path + "'", e);
    } catch (IOException e) {
      throw new BatfishException("Failed to delete file: " + path, e);
    }
  }

  public static void deleteDirectory(Path path) {
    try {
      FileUtils.deleteDirectory(path.toFile());
    } catch (IOException | NullPointerException e) {
      throw new BatfishException("Could not delete directory: " + path, e);
    }
  }

  public static void deleteIfExists(Path path) {
    try {
      Files.delete(path);
    } catch (NoSuchFileException e) {
      return;
    } catch (IOException e) {
      throw new BatfishException("Failed to delete file: " + path, e);
    }
  }

  public static <S extends Set<T>, T> S difference(
      Set<T> minuendSet, Set<T> subtrahendSet, Supplier<S> setConstructor) {
    S differenceSet = setConstructor.get();
    differenceSet.addAll(minuendSet);
    differenceSet.removeAll(subtrahendSet);
    return differenceSet;
  }

  public static String escape(String offendingTokenText) {
    return offendingTokenText.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
  }

  public static String extractBits(long l, int start, int end) {
    StringBuilder s = new StringBuilder();
    for (int pos = end; pos >= start; pos--) {
      long mask = 1L << pos;
      long bit = l & mask;
      s.append((bit != 0) ? '1' : '0');
    }
    return s.toString();
  }

  public static <T> void forEachWithIndex(Iterable<T> ts, BiConsumer<Integer, T> biConsumer) {
    int i = 0;
    for (T t : ts) {
      biConsumer.accept(i, t);
      i++;
    }
  }

  public static Path getCanonicalPath(Path path) {
    try {
      return Paths.get(path.toFile().getCanonicalPath());
    } catch (IOException e) {
      throw new BatfishException("Could not get canonical path from: '" + path + "'");
    }
  }

  public static org.apache.commons.configuration2.Configuration getConfig(
      String overridePropertyName,
      String defaultPropertyFilename,
      Class<?> defaultPropertyLocatorClass) {
    String overriddenPath = System.getProperty(overridePropertyName);
    URL propertiesUrl;
    if (overriddenPath != null) {
      // The user provided an override, so look up that configuration instead.
      try {
        propertiesUrl = new URL(new URL("file://"), overriddenPath);
      } catch (MalformedURLException e) {
        throw new BatfishException(
            "Error treating " + overriddenPath + " as a path to a properties file", e);
      }
    } else {
      // Find the default properties file.
      propertiesUrl =
          defaultPropertyLocatorClass.getClassLoader().getResource(defaultPropertyFilename);
    }
    try {
      return new Configurations().properties(propertiesUrl);
    } catch (Exception e) {
      throw new BatfishException("Error loading configuration from " + overriddenPath, e);
    }
  }

  public static SortedSet<Path> getEntries(Path directory) {
    SortedSet<Path> entries = new TreeSet<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path entry : stream) {
        entries.add(entry);
      }
    } catch (IOException | DirectoryIteratorException e) {
      throw new BatfishException("Error listing directory '" + directory + "'", e);
    }
    return entries;
  }

  public static String getIndentedString(String str, int indentLevel) {
    String indent = getIndentString(indentLevel);
    StringBuilder sb = new StringBuilder();
    String[] lines = str.split("\n", -1);
    for (String line : lines) {
      sb.append(indent + line + "\n");
    }
    return sb.toString();
  }

  public static String getIndentString(int indentLevel) {
    return StringUtils.repeat("  ", indentLevel);
  }

  @Nullable
  public static Integer getInterfaceVlanNumber(String ifaceName) {
    String prefix = "vlan";
    String ifaceNameLower = ifaceName.toLowerCase();
    String withoutDot = ifaceNameLower.replaceAll("\\.", "");
    if (withoutDot.startsWith(prefix)) {
      String vlanStr = withoutDot.substring(prefix.length());
      if (vlanStr.length() > 0) {
        return Integer.parseInt(vlanStr);
      }
    }
    return null;
  }

  public static FileTime getLastModifiedTime(Path path) {
    try {
      return Files.getLastModifiedTime(path);
    } catch (IOException e) {
      throw new BatfishException("Failed to get last modified time for '" + path + "'");
    }
  }

  public static List<String> getMatchingStrings(String regex, Set<String> allStrings) {
    Pattern pattern;
    try {
      pattern = Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new BatfishException("Supplied regex is not a valid java regex: \"" + regex + "\"", e);
    }
    return allStrings
        .stream()
        .filter(s -> pattern.matcher(s).matches())
        .collect(Collectors.toList());
  }

  public static SortedSet<Path> getSubdirectories(Path directory) {
    SortedSet<Path> subdirectories = new TreeSet<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          subdirectories.add(entry);
        }
      }
    } catch (IOException | DirectoryIteratorException e) {
      throw new BatfishException("Error listing directory '" + directory + "'", e);
    }
    return subdirectories;
  }

  public static String getTime(long millis) {
    long cs = (millis / 10) % 100;
    long s = (millis / 1000) % 60;
    long m = (millis / (1000 * 60)) % 60;
    long h = (millis / (1000 * 60 * 60)) % 24;
    String time = String.format("%02d:%02d:%02d.%02d", h, m, s, cs);
    return time;
  }

  /**
   * Initialize BGP neighbors for all nodes.
   *
   * @param configurations map of all configurations, keyed by hostname
   * @param ipOwners mapping of Ips to a set of nodes (hostnames) that owns those IPs
   * @param checkReachability whether bgp neighbor reachability should be checked
   * @param flowProcessor dataplane plugin to use to check reachability. Must not be {@code null} if
   *     {@code checkReachability = true}
   * @param dp dataplane to use to check reachability. Must not be {@code null} if {@code
   *     checkReachability = true}
   */
  public static void initRemoteBgpNeighbors(
      Map<String, Configuration> configurations,
      Map<Ip, Set<String>> ipOwners,
      boolean checkReachability,
      @Nullable FlowProcessor flowProcessor,
      @Nullable DataPlane dp) {
    // TODO: handle duplicate ips on different vrfs
    Map<BgpNeighbor, Ip> remoteAddresses = new IdentityHashMap<>();
    Map<Ip, Set<BgpNeighbor>> localAddresses = new HashMap<>();

    /*
     * Construct maps indicating which neighbor owns which Ip Address
     */
    for (Configuration node : configurations.values()) {
      String hostname = node.getHostname();
      for (Vrf vrf : node.getVrfs().values()) {
        BgpProcess proc = vrf.getBgpProcess();
        if (proc == null) {
          // nothing to do if no bgp process on this VRF
          continue;
        }
        for (BgpNeighbor bgpNeighbor : proc.getNeighbors().values()) {
          /*
           * Begin by initializing candidate neighbors to an empty set
           */
          bgpNeighbor.initCandidateRemoteBgpNeighbors();

          // Skip things we don't handle
          if (bgpNeighbor.getPrefix().getPrefixLength() < Prefix.MAX_PREFIX_LENGTH) {
            throw new BatfishException(
                hostname
                    + ": Do not support dynamic bgp sessions at this time: "
                    + bgpNeighbor.getPrefix());
          }

          Ip remoteAddress = bgpNeighbor.getAddress();
          if (remoteAddress == null) {
            throw new BatfishException(
                hostname + ": Could not determine remote address of bgp neighbor: " + bgpNeighbor);
          }
          Ip localAddress = bgpNeighbor.getLocalIp();
          if (localAddress == null
              || !ipOwners.containsKey(localAddress)
              || !ipOwners.get(localAddress).contains(hostname)) {
            // Local address is not owned by anybody
            continue;
          }

          remoteAddresses.put(bgpNeighbor, remoteAddress);
          // Add this neighbor as owner of its local address
          localAddresses
              .computeIfAbsent(
                  localAddress, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
              .add(bgpNeighbor);
        }
      }
    }
    /*
     * For each neighbor, construct the set of candidate neighbors, then filter out impossible
     * sessions.
     */
    for (Entry<BgpNeighbor, Ip> e : remoteAddresses.entrySet()) {
      BgpNeighbor bgpNeighbor = e.getKey();
      Ip remoteAddress = e.getValue();
      Ip localAddress = bgpNeighbor.getLocalIp();
      int localLocalAs = bgpNeighbor.getLocalAs();
      int localRemoteAs = bgpNeighbor.getRemoteAs();

      /*
       * Let the set of candidate neighbors be set of neighbors that own the remoteAddress
       */
      Set<BgpNeighbor> remoteBgpNeighborCandidates = localAddresses.get(remoteAddress);
      if (remoteBgpNeighborCandidates == null) {
        // No possible remote neighbors
        continue;
      }
      /*
       * Filter the set of candidate neighbors based on these checks:
       * - Remote neighbor's remote address is the same as our local address
       * - Remote neighbor's remote AS is the same as our local AS (and vice-versa)
       */
      for (BgpNeighbor remoteBgpNeighborCandidate : remoteBgpNeighborCandidates) {
        int remoteLocalAs = remoteBgpNeighborCandidate.getLocalAs();
        int remoteRemoteAs = remoteBgpNeighborCandidate.getRemoteAs();
        Ip reciprocalRemoteIp = remoteBgpNeighborCandidate.getAddress();
        if (localAddress.equals(reciprocalRemoteIp)
            && localLocalAs == remoteRemoteAs
            && localRemoteAs == remoteLocalAs) {
          /*
           * Fairly confident establishing the session is possible here, but still check
           * reachability if needed.
           * We should check reachability only for eBgp multihop or iBgp
           */
          if (checkReachability
              && (bgpNeighbor.getEbgpMultihop() || localLocalAs == remoteLocalAs)) {
            /*
             * Ensure that the session can be established by running traceroute in both directions
             */
            if (flowProcessor == null || dp == null) {
              throw new BatfishException(
                  "Cannot compute neighbor reachability without a dataplane");
            }
            Flow.Builder fb = new Flow.Builder();
            fb.setIpProtocol(IpProtocol.TCP);
            fb.setTag("neighbor-resolution");

            fb.setIngressNode(bgpNeighbor.getOwner().getHostname());
            fb.setSrcIp(localAddress);
            fb.setDstIp(remoteAddress);
            fb.setSrcPort(NamedPort.EPHEMERAL_LOWEST.number());
            fb.setDstPort(NamedPort.BGP.number());
            Flow forwardFlow = fb.build();

            fb.setIngressNode(remoteBgpNeighborCandidate.getOwner().getHostname());
            fb.setSrcIp(forwardFlow.getDstIp());
            fb.setDstIp(forwardFlow.getSrcIp());
            fb.setSrcPort(forwardFlow.getDstPort());
            fb.setDstPort(forwardFlow.getSrcPort());
            Flow backwardFlow = fb.build();
            SortedMap<Flow, Set<FlowTrace>> traces =
                flowProcessor.processFlows(dp, ImmutableSet.of(forwardFlow, backwardFlow));

            if (traces
                .values()
                .stream()
                .map(
                    fts ->
                        fts.stream()
                            .allMatch(ft -> ft.getDisposition() != FlowDisposition.ACCEPTED))
                .anyMatch(Predicate.isEqual(true))) {
              /*
               * If either flow has all traceroutes fail, do not consider the neighbor valid
               */
              continue;
            }
            bgpNeighbor.getCandidateRemoteBgpNeighbors().add(remoteBgpNeighborCandidate);
          } else {
            bgpNeighbor.getCandidateRemoteBgpNeighbors().add(remoteBgpNeighborCandidate);
          }
        }
      }
      Set<BgpNeighbor> finalCandidates = bgpNeighbor.getCandidateRemoteBgpNeighbors();
      if (finalCandidates.size() > 1) {
        /* If we still have not narrowed it down to a single neighbor,
         * pick based on sorted hostnames
         */
        SortedMap<String, BgpNeighbor> hostnameToNeighbor =
            finalCandidates
                .stream()
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        String::compareTo, k -> k.getOwner().getHostname(), Function.identity()));
        bgpNeighbor.setRemoteBgpNeighbor(hostnameToNeighbor.get(hostnameToNeighbor.firstKey()));
      } else if (finalCandidates.size() == 1) {
        bgpNeighbor.setRemoteBgpNeighbor(finalCandidates.iterator().next());
      } else {
        bgpNeighbor.setRemoteBgpNeighbor(null);
      }
    }
  }

  /**
   * See {@link #initRemoteBgpNeighbors(Map, Map, boolean, FlowProcessor, DataPlane)}. Equivalent to
   * {@code initRemoteBgpNeighbors(Map, Map, false, null, null)}
   */
  public static void initRemoteBgpNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners) {
    initRemoteBgpNeighbors(configurations, ipOwners, false, null, null);
  }

  @VisibleForTesting
  static SetMultimap<Ip, IpWildcardSetIpSpace> initPrivateIpsByPublicIp(
      Map<String, Configuration> configurations) {
    /*
     * Very hacky mapping from public IP to set of spaces of possible natted private IPs.
     * Does not currently support source-nat acl.
     *
     * The current implementation just considers every IP in every prefix on a non-masquerading
     * interface (except the local address in each such prefix) to be a possible private IP
     * match for every public IP referred to by every source-nat pool on a masquerading interface.
     */
    ImmutableSetMultimap.Builder<Ip, IpWildcardSetIpSpace> builder = ImmutableSetMultimap.builder();
    for (Configuration c : configurations.values()) {
      Collection<Interface> interfaces = c.getInterfaces().values();
      Set<InterfaceAddress> nonNattedInterfaceAddresses =
          interfaces
              .stream()
              .filter(i -> i.getSourceNats().isEmpty())
              .flatMap(i -> i.getAllAddresses().stream())
              .collect(ImmutableSet.toImmutableSet());
      Set<IpWildcard> blacklist =
          nonNattedInterfaceAddresses
              .stream()
              .map(address -> new IpWildcard(address.getIp(), Ip.ZERO))
              .collect(ImmutableSet.toImmutableSet());
      Set<IpWildcard> whitelist =
          nonNattedInterfaceAddresses
              .stream()
              .map(address -> new IpWildcard(address.getPrefix()))
              .collect(ImmutableSet.toImmutableSet());
      IpWildcardSetIpSpace ipSpace =
          IpWildcardSetIpSpace.builder().including(whitelist).excluding(blacklist).build();
      interfaces
          .stream()
          .flatMap(i -> i.getSourceNats().stream())
          .forEach(
              sourceNat -> {
                for (long ipAsLong = sourceNat.getPoolIpFirst().asLong();
                    ipAsLong <= sourceNat.getPoolIpLast().asLong();
                    ipAsLong++) {
                  Ip currentPoolIp = new Ip(ipAsLong);
                  builder.put(currentPoolIp, ipSpace);
                }
              });
    }
    return builder.build();
  }

  public static void initRemoteOspfNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology) {
    for (Entry<String, Configuration> e : configurations.entrySet()) {
      String hostname = e.getKey();
      Configuration c = e.getValue();
      for (Entry<String, Vrf> e2 : c.getVrfs().entrySet()) {
        Vrf vrf = e2.getValue();
        OspfProcess proc = vrf.getOspfProcess();
        if (proc != null) {
          proc.setOspfNeighbors(new TreeMap<>());
          String vrfName = e2.getKey();
          for (Entry<Long, OspfArea> e3 : proc.getAreas().entrySet()) {
            long areaNum = e3.getKey();
            OspfArea area = e3.getValue();
            for (String ifaceName : area.getInterfaces()) {
              Interface iface = c.getInterfaces().get(ifaceName);
              if (iface.getOspfPassive()) {
                continue;
              }
              SortedSet<Edge> ifaceEdges =
                  topology.getInterfaceEdges().get(new NodeInterfacePair(hostname, ifaceName));
              boolean hasNeighbor = false;
              Ip localIp = iface.getAddress().getIp();
              if (ifaceEdges != null) {
                for (Edge edge : ifaceEdges) {
                  if (edge.getNode1().equals(hostname)) {
                    String remoteHostname = edge.getNode2();
                    String remoteIfaceName = edge.getInt2();
                    Configuration remoteNode = configurations.get(remoteHostname);
                    Interface remoteIface = remoteNode.getInterfaces().get(remoteIfaceName);
                    if (remoteIface.getOspfPassive()) {
                      continue;
                    }
                    Vrf remoteVrf = remoteIface.getVrf();
                    String remoteVrfName = remoteVrf.getName();
                    OspfProcess remoteProc = remoteVrf.getOspfProcess();
                    if (remoteProc != null) {
                      if (remoteProc.getOspfNeighbors() == null) {
                        remoteProc.setOspfNeighbors(new TreeMap<>());
                      }
                      OspfArea remoteArea = remoteProc.getAreas().get(areaNum);
                      if (remoteArea != null
                          && remoteArea.getInterfaces().contains(remoteIfaceName)) {
                        Ip remoteIp = remoteIface.getAddress().getIp();
                        IpLink localKey = new IpLink(localIp, remoteIp);
                        OspfNeighbor neighbor = proc.getOspfNeighbors().get(localKey);
                        if (neighbor == null) {
                          hasNeighbor = true;

                          // initialize local neighbor
                          neighbor = new OspfNeighbor(localKey);
                          neighbor.setArea(areaNum);
                          neighbor.setVrf(vrfName);
                          neighbor.setOwner(c);
                          neighbor.setInterface(iface);
                          proc.getOspfNeighbors().put(localKey, neighbor);

                          // initialize remote neighbor
                          IpLink remoteKey = new IpLink(remoteIp, localIp);
                          OspfNeighbor remoteNeighbor = new OspfNeighbor(remoteKey);
                          remoteNeighbor.setArea(areaNum);
                          remoteNeighbor.setVrf(remoteVrfName);
                          remoteNeighbor.setOwner(remoteNode);
                          remoteNeighbor.setInterface(remoteIface);
                          remoteProc.getOspfNeighbors().put(remoteKey, remoteNeighbor);

                          // link neighbors
                          neighbor.setRemoteOspfNeighbor(remoteNeighbor);
                          remoteNeighbor.setRemoteOspfNeighbor(neighbor);
                        }
                      }
                    }
                  }
                }
              }
              if (!hasNeighbor) {
                IpLink key = new IpLink(localIp, Ip.ZERO);
                OspfNeighbor neighbor = new OspfNeighbor(key);
                neighbor.setArea(areaNum);
                neighbor.setVrf(vrfName);
                neighbor.setOwner(c);
                neighbor.setInterface(iface);
                proc.getOspfNeighbors().put(key, neighbor);
              }
            }
          }
        }
      }
    }
  }

  public static void initRemoteIpsecVpns(Map<String, Configuration> configurations) {
    Map<IpsecVpn, Ip> vpnRemoteIps = new IdentityHashMap<>();
    Map<Ip, Set<IpsecVpn>> externalIpVpnMap = new HashMap<>();
    SetMultimap<Ip, IpWildcardSetIpSpace> privateIpsByPublicIp =
        initPrivateIpsByPublicIp(configurations);
    for (Configuration c : configurations.values()) {
      for (IpsecVpn ipsecVpn : c.getIpsecVpns().values()) {
        Ip remoteIp = ipsecVpn.getIkeGateway().getAddress();
        vpnRemoteIps.put(ipsecVpn, remoteIp);
        Set<InterfaceAddress> externalAddresses =
            ipsecVpn.getIkeGateway().getExternalInterface().getAllAddresses();
        for (InterfaceAddress address : externalAddresses) {
          Ip ip = address.getIp();
          Set<IpsecVpn> vpnsUsingExternalAddress =
              externalIpVpnMap.computeIfAbsent(ip, k -> Sets.newIdentityHashSet());
          vpnsUsingExternalAddress.add(ipsecVpn);
        }
      }
    }
    for (Entry<IpsecVpn, Ip> e : vpnRemoteIps.entrySet()) {
      IpsecVpn ipsecVpn = e.getKey();
      Ip remoteIp = e.getValue();
      Ip localIp = ipsecVpn.getIkeGateway().getLocalIp();
      ipsecVpn.initCandidateRemoteVpns();
      Set<IpsecVpn> remoteIpsecVpnCandidates = externalIpVpnMap.get(remoteIp);
      if (remoteIpsecVpnCandidates != null) {
        for (IpsecVpn remoteIpsecVpnCandidate : remoteIpsecVpnCandidates) {
          Ip remoteIpsecVpnLocalAddress = remoteIpsecVpnCandidate.getIkeGateway().getLocalIp();
          if (remoteIpsecVpnLocalAddress != null && !remoteIpsecVpnLocalAddress.equals(remoteIp)) {
            continue;
          }
          Ip reciprocalRemoteAddress = vpnRemoteIps.get(remoteIpsecVpnCandidate);
          Set<IpsecVpn> reciprocalVpns = externalIpVpnMap.get(reciprocalRemoteAddress);
          if (reciprocalVpns == null) {
            Set<IpWildcardSetIpSpace> privateIpsBehindReciprocalRemoteAddress =
                privateIpsByPublicIp.get(reciprocalRemoteAddress);
            if (privateIpsBehindReciprocalRemoteAddress != null
                && privateIpsBehindReciprocalRemoteAddress
                    .stream()
                    .anyMatch(ipSpace -> ipSpace.containsIp(localIp))) {
              reciprocalVpns = externalIpVpnMap.get(localIp);
              ipsecVpn.setRemoteIpsecVpn(remoteIpsecVpnCandidate);
              ipsecVpn.getCandidateRemoteIpsecVpns().add(remoteIpsecVpnCandidate);
              remoteIpsecVpnCandidate.initCandidateRemoteVpns();
              remoteIpsecVpnCandidate.setRemoteIpsecVpn(ipsecVpn);
              remoteIpsecVpnCandidate.getCandidateRemoteIpsecVpns().add(ipsecVpn);
            }
          } else if (reciprocalVpns.contains(ipsecVpn)) {
            ipsecVpn.setRemoteIpsecVpn(remoteIpsecVpnCandidate);
            ipsecVpn.getCandidateRemoteIpsecVpns().add(remoteIpsecVpnCandidate);
          }
        }
      }
    }
  }

  public static <S extends Set<T>, T> S intersection(
      Set<T> set1, Collection<T> set2, Supplier<S> setConstructor) {
    S intersectionSet = setConstructor.get();
    intersectionSet.addAll(set1);
    intersectionSet.retainAll(set2);
    return intersectionSet;
  }

  public static int intWidth(int n) {
    if (n == 0) {
      return 1;
    } else {
      return 32 - Integer.numberOfLeadingZeros(n);
    }
  }

  public static boolean isLoopback(String interfaceName) {
    return interfaceName.toLowerCase().startsWith("lo");
  }

  public static boolean isNullInterface(String ifaceName) {
    String lcIfaceName = ifaceName.toLowerCase();
    return lcIfaceName.startsWith("null");
  }

  public static boolean isNullOrEmpty(@Nullable Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  @MustBeClosed
  public static Stream<Path> list(Path configsPath) {
    try {
      return Files.list(configsPath);
    } catch (IOException e) {
      throw new BatfishException("Could not list files in '" + configsPath + "'", e);
    }
  }

  public static String longToCommunity(Long l) {
    Long upper = l >> 16;
    Long lower = l & 0xFFFF;
    return upper + ":" + lower;
  }

  public static void moveByCopy(Path srcPath, Path dstPath) {
    if (Files.isDirectory(srcPath)) {
      copyDirectory(srcPath, dstPath);
      deleteDirectory(srcPath);
    } else {
      copyFile(srcPath, dstPath);
      delete(srcPath);
    }
  }

  public static int nullChecker(Object a, Object b) {
    if (a == null && b == null) {
      return 0;
    } else if (a != null && b != null) {
      return 1;
    } else {
      return -1;
    }
  }

  public static void outputFileLines(Path downloadedFile, Consumer<String> outputFunction) {
    try (BufferedReader br = Files.newBufferedReader(downloadedFile, StandardCharsets.UTF_8)) {
      String line = null;
      while ((line = br.readLine()) != null) {
        outputFunction.accept(line + "\n");
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Failed to read and output lines of file: '" + downloadedFile + "'");
    }
  }

  public static String readFile(Path file) {
    String text = null;
    try {
      text = new String(Files.readAllBytes(file), "UTF-8");
    } catch (IOException e) {
      throw new BatfishException("Failed to read file: " + file, e);
    }
    return text;
  }

  public static String readResource(String resourcePath) {
    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new BatfishException("Error opening resource: '" + resourcePath + "'");
      }
      String output = IOUtils.toString(is);
      return output;
    } catch (IOException e) {
      throw new BatfishException("Could not open resource: '" + resourcePath + "'", e);
    }
  }

  public static synchronized String salt() {
    if (SALT == null) {
      SALT = readResource(BfConsts.ABSPATH_DEFAULT_SALT);
    }
    return SALT;
  }

  /** Returns a hex {@link String} representation of the SHA-256 hash digest of the input string. */
  public static String sha256Digest(String saltedSecret) {
    return Hashing.sha256().hashString(saltedSecret, StandardCharsets.UTF_8).toString();
  }

  public static void startSslServer(
      ResourceConfig resourceConfig,
      URI mgrUri,
      Path keystorePath,
      String keystorePassword,
      boolean trustAllCerts,
      Path truststorePath,
      String truststorePassword,
      Class<?> configurationLocatorClass,
      Class<?> callerClass) {
    if (keystorePath == null) {
      throw new BatfishException(
          "Cannot start SSL server without keystore. If you have none, you must disable SSL.");
    }
    // first find the file as specified.
    // if that does not work, find it relative to the binary
    Path keystoreAbsolutePath = keystorePath.toAbsolutePath();
    if (!Files.exists(keystoreAbsolutePath)) {
      String callingClass = callerClass.getCanonicalName();
      System.err.printf(
          "%s: keystore file not found at %s or %s%n",
          callingClass, keystorePath, keystoreAbsolutePath);
      System.exit(1);
    }
    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setKeyStoreFile(keystoreAbsolutePath.toString());
    sslCon.setKeyStorePass(keystorePassword);
    if (truststorePath != null) {
      if (truststorePassword == null) {
        throw new BatfishException("Truststore file supplied but truststore password missing");
      }
      sslCon.setTrustStoreFile(truststorePath.toString());
      sslCon.setTrustStorePass(truststorePassword);
    }
    boolean verifyClient = !trustAllCerts;
    GrizzlyHttpServerFactory.createHttpServer(
        mgrUri,
        resourceConfig,
        true,
        new SSLEngineConfigurator(sslCon, false, verifyClient, false));
  }

  public static <S extends Set<T>, T> S symmetricDifference(
      Set<T> set1, Set<T> set2, Supplier<S> constructor) {
    S differenceSet = constructor.get();
    differenceSet.addAll(set1);
    differenceSet.addAll(set2);
    S intersection = intersection(set1, set2, constructor);
    differenceSet.removeAll(intersection);
    return differenceSet;
  }

  public static Topology synthesizeTopology(Map<String, Configuration> configurations) {
    SortedSet<Edge> edges = new TreeSet<>();
    Map<Prefix, Set<Interface>> prefixInterfaces = new HashMap<>();
    configurations.forEach(
        (nodeName, node) -> {
          for (Interface iface : node.getInterfaces().values()) {
            if (!iface.isLoopback(node.getConfigurationFormat()) && iface.getActive()) {
              for (InterfaceAddress address : iface.getAllAddresses()) {
                if (address.getNetworkBits() < Prefix.MAX_PREFIX_LENGTH) {
                  Prefix prefix = address.getPrefix();
                  Set<Interface> interfaceBucket =
                      prefixInterfaces.computeIfAbsent(prefix, k -> new HashSet<>());
                  interfaceBucket.add(iface);
                }
              }
            }
          }
        });
    for (Set<Interface> bucket : prefixInterfaces.values()) {
      for (Interface iface1 : bucket) {
        for (Interface iface2 : bucket) {
          if (iface1 != iface2
              // don't connect interfaces that have the same address
              && Sets.intersection(iface1.getAllAddresses(), iface2.getAllAddresses()).isEmpty()) {
            edges.add(
                new Edge(
                    new NodeInterfacePair(iface1.getOwner().getHostname(), iface1.getName()),
                    new NodeInterfacePair(iface2.getOwner().getHostname(), iface2.getName())));
          }
        }
      }
    }
    return new Topology(edges);
  }

  public static SortedMap<Integer, String> toLineMap(String str) {
    SortedMap<Integer, String> map = new TreeMap<>();
    String[] lines = str.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      map.put(i, line);
    }
    return map;
  }

  public static <K1, K2, V1, V2> Map<K2, V2> toImmutableMap(
      Map<K1, V1> map,
      Function<Entry<K1, V1>, K2> keyFunction,
      Function<Entry<K1, V1>, V2> valueFunction) {
    return map.entrySet().stream().collect(ImmutableMap.toImmutableMap(keyFunction, valueFunction));
  }

  public static <E, K, V> Map<K, V> toImmutableMap(
      Collection<E> set, Function<E, K> keyFunction, Function<E, V> valueFunction) {
    return set.stream().collect(ImmutableMap.toImmutableMap(keyFunction, valueFunction));
  }

  public static <S extends Set<T>, T> S union(
      Set<T> set1, Set<T> set2, Supplier<S> setConstructor) {
    S unionSet = setConstructor.get();
    unionSet.addAll(set1);
    unionSet.addAll(set2);
    return unionSet;
  }

  public static void writeFile(Path outputPath, String output) {
    try (FileOutputStream fs = new FileOutputStream(outputPath.toFile());
        OutputStreamWriter os = new OutputStreamWriter(fs, StandardCharsets.UTF_8)) {
      os.write(output);
    } catch (FileNotFoundException e) {
      throw new BatfishException("Failed to write file (file not found): " + outputPath, e);
    } catch (IOException e) {
      throw new BatfishException("Failed to write file: " + outputPath, e);
    }
  }

  public static void writeStreamToFile(InputStream inputStream, Path outputFile) {
    try (OutputStream fileOutputStream = new FileOutputStream(outputFile.toFile())) {
      int read = 0;
      final byte[] bytes = new byte[STREAMED_FILE_BUFFER_SIZE];
      while ((read = inputStream.read(bytes)) != -1) {
        fileOutputStream.write(bytes, 0, read);
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Failed to write input stream to output file: '" + outputFile + "'");
    }
  }
}
