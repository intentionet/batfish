package org.batfish.common.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
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
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
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
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.UniverseIpSpace;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.skyscreamer.jsonassert.JSONAssert;

public class CommonUtil {

  public static SortedSet<IpWildcard> asPositiveIpWildcards(IpSpace ipSpace) {
    // TODO use an IpSpace visitor
    if (ipSpace == null) {
      return null;
    } else if (ipSpace instanceof IpWildcardIpSpace) {
      return ImmutableSortedSet.of(((IpWildcardIpSpace) ipSpace).getIpWildcard());
    } else if (ipSpace instanceof IpWildcardSetIpSpace) {
      return ((IpWildcardSetIpSpace) ipSpace).getWhitelist();
    } else if (ipSpace instanceof UniverseIpSpace) {
      return ImmutableSortedSet.of();
    } else {
      throw new BatfishException(
          String.format("Cannot represent as SortedSet<IpWildcard>: %s", ipSpace));
    }
  }

  public static SortedSet<IpWildcard> asNegativeIpWildcards(IpSpace ipSpace) {
    // TODO use an IpSpace visitor
    if (ipSpace == null) {
      return null;
    } else if (ipSpace instanceof IpWildcardIpSpace) {
      return ImmutableSortedSet.of(((IpWildcardIpSpace) ipSpace).getIpWildcard());
    } else if (ipSpace instanceof IpWildcardSetIpSpace) {
      return ((IpWildcardSetIpSpace) ipSpace).getWhitelist();
    } else if (ipSpace instanceof EmptyIpSpace) {
      return ImmutableSortedSet.of();
    } else {
      throw new BatfishException(
          String.format("Cannot represent as SortedSet<IpWildcard>: %s", ipSpace));
    }
  }

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

  /**
   * Convert a BGP community string to its numeric representation. Only 16-bit AS numbers and
   * community values are supported.
   *
   * @throws IllegalArgumentException if the AS number or community value is over 16 bits.
   */
  public static long communityStringToLong(@Nonnull String str) {
    String[] parts = str.split(":");
    long high = Long.parseLong(parts[0]);
    // Bug: this function is called on both regular and extended communities.
    // Do not perform checking of as "validity"
    // https://github.com/batfish/batfish/issues/2103
    //    checkLongWithin16Bit(high);
    long low = Long.parseLong(parts[1]);
    //    checkLongWithin16Bit(low);
    return low + (high << 16);
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

  /**
   * Returns a {@link ClientBuilder} with supplied settings
   *
   * @param noSsl {@link javax.ws.rs.client.Client} will use plain HTTP with no SSL if set to true
   * @param trustAllSslCerts {@link javax.ws.rs.client.Client} will not verify URL's hostname
   *     against server's identification hostname
   * @param keystoreFile File to be used to load the {@link KeyStore}
   * @param keystorePassword Password to be used with the keyStoreFile
   * @param truststoreFile File to be used to load the {@link TrustManager}
   * @param truststorePassword Password to be used with the data in the trustStoreFile
   * @param registerTracing Whether to register JAX-RS tracing on the {@link ClientBuilder}
   * @return {@link ClientBuilder} with the supplied settings
   */
  public static ClientBuilder createHttpClientBuilder(
      boolean noSsl,
      boolean trustAllSslCerts,
      Path keystoreFile,
      String keystorePassword,
      Path truststoreFile,
      String truststorePassword,
      boolean registerTracing) {
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
            throw new BatfishException("Keystore file supplied but keystore password missing");
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
      /* register tracing feature if a tracer was initialized and caller wants client to
      send tracing information */
      if (GlobalTracer.isRegistered() && registerTracing) {
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

  public static <T> void forEachWithIndex(Iterable<T> ts, BiConsumer<Integer, T> biConsumer) {
    int i = 0;
    for (T t : ts) {
      biConsumer.accept(i, t);
      i++;
    }
  }

  /**
   * Returns an active interface with the specified name for configuration.
   *
   * @param name The name to check
   * @param c The configuration object in which to check
   * @return Any Interface that matches the condition
   */
  public static Optional<Interface> getActiveInterfaceWithName(String name, Configuration c) {
    return c.getAllInterfaces()
        .values()
        .stream()
        .filter(iface -> iface.getActive() && iface.getName().equals(name))
        .findAny();
  }

  /**
   * Returns an active interface with the specified IP address for configuration.
   *
   * @param ipAddress The IP address to check
   * @param c The configuration object in which to check
   * @return Any Interface that matches the condition
   */
  public static Optional<Interface> getActiveInterfaceWithIp(Ip ipAddress, Configuration c) {
    return c.getAllInterfaces()
        .values()
        .stream()
        .filter(
            iface ->
                iface.getActive()
                    && iface
                        .getAllAddresses()
                        .stream()
                        .anyMatch(ifAddr -> Objects.equals(ifAddr.getIp(), ipAddress)))
        .findAny();
  }

  public static Path getCanonicalPath(Path path) {
    try {
      return Paths.get(path.toFile().getCanonicalPath());
    } catch (IOException e) {
      throw new BatfishException("Could not get canonical path from: '" + path + "'", e);
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
      Collection<Interface> interfaces = c.getAllInterfaces().values();
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

  public static void initRemoteIpsecVpns(Map<String, Configuration> configurations) {
    Map<IpsecVpn, Ip> vpnRemoteIps = new IdentityHashMap<>();
    Map<Ip, Set<IpsecVpn>> externalIpVpnMap = new HashMap<>();
    SetMultimap<Ip, IpWildcardSetIpSpace> privateIpsByPublicIp =
        initPrivateIpsByPublicIp(configurations);
    for (Configuration c : configurations.values()) {
      for (IpsecVpn ipsecVpn : c.getIpsecVpns().values()) {
        IkeGateway ikeGateway = ipsecVpn.getIkeGateway();
        if (ikeGateway == null || ikeGateway.getExternalInterface() == null) {
          continue;
        }
        Ip remoteIp = ikeGateway.getAddress();
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
                    .anyMatch(ipSpace -> ipSpace.containsIp(localIp, ImmutableMap.of()))) {
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

  /** Convert a given long to a string BGP community representation. */
  @Nonnull
  public static String longToCommunity(long l) {
    long upper = l >> 16;
    long lower = l & 0xFFFF;
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

  public static void outputFileLines(Path downloadedFile, Consumer<String> outputFunction) {
    try (BufferedReader br = Files.newBufferedReader(downloadedFile, StandardCharsets.UTF_8)) {
      String line = null;
      while ((line = br.readLine()) != null) {
        outputFunction.accept(line + "\n");
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Failed to read and output lines of file: '" + downloadedFile + "'", e);
    }
  }

  public static boolean rangesContain(Collection<SubRange> ranges, int num) {
    return ranges.stream().anyMatch(sr -> sr.includes(num));
  }

  @Nonnull
  public static String readFile(Path file) throws BatfishException {
    String text;
    try {
      text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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
      String output = IOUtils.toString(is, StandardCharsets.UTF_8);
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

  public static HttpServer startSslServer(
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
    return GrizzlyHttpServerFactory.createHttpServer(
        mgrUri,
        resourceConfig,
        true,
        new SSLEngineConfigurator(sslCon, false, verifyClient, false));
  }

  /** Returns {@code true} if any {@link Ip IP address} is owned by both devices. */
  private static boolean haveIpInCommon(Interface i1, Interface i2) {
    for (InterfaceAddress ia : i1.getAllAddresses()) {
      for (InterfaceAddress ia2 : i2.getAllAddresses()) {
        if (ia.getIp().equals(ia2.getIp())) {
          return true;
        }
      }
    }
    return false;
  }

  public static Topology synthesizeTopology(Map<String, Configuration> configurations) {
    Map<Prefix, List<Interface>> prefixInterfaces = new HashMap<>();
    configurations.forEach(
        (nodeName, node) -> {
          for (Interface iface : node.getAllInterfaces().values()) {
            if (iface.isLoopback(node.getConfigurationFormat()) || !iface.getActive()) {
              continue;
            }
            for (InterfaceAddress address : iface.getAllAddresses()) {
              if (address.getNetworkBits() < Prefix.MAX_PREFIX_LENGTH) {
                Prefix prefix = address.getPrefix();
                List<Interface> interfaceBucket =
                    prefixInterfaces.computeIfAbsent(prefix, k -> new LinkedList<>());
                interfaceBucket.add(iface);
              }
            }
          }
        });

    ImmutableSortedSet.Builder<Edge> edges = ImmutableSortedSet.naturalOrder();
    for (Entry<Prefix, List<Interface>> bucketEntry : prefixInterfaces.entrySet()) {
      Prefix p = bucketEntry.getKey();

      // Collect all interfaces that have subnets overlapping P iff they have an IP address in P.
      // Use an IdentityHashSet to prevent duplicates.
      Set<Interface> candidateInterfaces = Sets.newIdentityHashSet();
      IntStream.range(0, Prefix.MAX_PREFIX_LENGTH)
          .mapToObj(
              i -> prefixInterfaces.getOrDefault(new Prefix(p.getStartIp(), i), ImmutableList.of()))
          .flatMap(Collection::stream)
          .filter(
              iface -> iface.getAllAddresses().stream().anyMatch(ia -> p.containsIp(ia.getIp())))
          .forEach(candidateInterfaces::add);

      for (Interface iface1 : bucketEntry.getValue()) {
        for (Interface iface2 : candidateInterfaces) {
          // No device self-adjacencies
          if (iface1.getOwner() == iface2.getOwner()) {
            continue;
          }
          // don't connect interfaces that have any IP address in common
          if (haveIpInCommon(iface1, iface2)) {
            continue;
          }
          edges.add(new Edge(iface1, iface2));
        }
      }
    }
    return new Topology(edges.build());
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

  public static <K1, K2 extends Comparable<? super K2>, V1, V2>
      SortedMap<K2, V2> toImmutableSortedMap(
          Map<K1, V1> map,
          Function<Entry<K1, V1>, K2> keyFunction,
          Function<Entry<K1, V1>, V2> valueFunction) {
    return map.entrySet()
        .stream()
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(), keyFunction, valueFunction));
  }

  public static <T, K extends Comparable<? super K>, V>
      Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    return ImmutableSortedMap.toImmutableSortedMap(
        Comparator.naturalOrder(), keyFunction, valueFunction);
  }

  public static <E, K extends Comparable<? super K>, V> NavigableMap<K, V> toImmutableSortedMap(
      Collection<E> set, Function<E, K> keyFunction, Function<E, V> valueFunction) {
    return set.stream()
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(), keyFunction, valueFunction));
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
          "Failed to write input stream to output file: '" + outputFile + "'", e);
    }
  }
}
