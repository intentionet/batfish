package org.batfish.common.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;

import com.google.common.hash.Hashing;
import com.ibm.icu.text.CharsetDetector;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.util.GlobalTracer;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.ClientBuilder;
import org.apache.commons.io.FileUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class CommonUtil {

  private static class TrustAllHostNameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  }

  private static String SALT;

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

  /**
   * Returns a {@link ClientBuilder} with supplied settings
   *
   * @param registerTracing Whether to register JAX-RS tracing on the {@link ClientBuilder}
   * @return {@link ClientBuilder} with the supplied settings
   */
  public static ClientBuilder createHttpClientBuilder(boolean registerTracing) {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    try {
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

  public static <T> void forEachWithIndex(Iterable<T> ts, BiConsumer<Integer, T> biConsumer) {
    int i = 0;
    for (T t : ts) {
      biConsumer.accept(i, t);
      i++;
    }
  }

  @Nullable
  public static Integer getInterfaceVlanNumber(String ifaceName) {
    return getInterfaceNumber("vlan", ifaceName);
  }

  @Nullable
  public static Integer getInterfaceNumber(String prefix, String ifaceName) {
    String ifaceNameLower = ifaceName.toLowerCase();
    String withoutDot = ifaceNameLower.replaceAll("\\.", "");
    if (withoutDot.startsWith(prefix.toLowerCase())) {
      String vlanStr = withoutDot.substring(prefix.length());
      if (vlanStr.length() > 0) {
        return Integer.parseInt(vlanStr);
      }
    }
    return null;
  }

  @Nonnull
  public static String readFile(Path file) throws BatfishException {
    String text;
    try {
      byte[] bytes = Files.readAllBytes(file);
      text = new String(bytes, detectCharset(bytes));
    } catch (IOException e) {
      throw new BatfishException("Failed to read file: " + file, e);
    }
    return text;
  }

  public static @Nonnull Charset detectCharset(byte[] bytes) {
    CharsetDetector detector = new CharsetDetector();
    detector.setText(bytes);
    return Charset.forName(detector.detect().getName());
  }

  public static synchronized String salt() {
    if (SALT == null) {
      SALT = readResource(BfConsts.ABSPATH_DEFAULT_SALT, UTF_8);
    }
    return SALT;
  }

  /** Returns a hex {@link String} representation of the SHA-256 hash digest of the input string. */
  public static String sha256Digest(String saltedSecret) {
    return Hashing.sha256().hashString(saltedSecret, UTF_8).toString();
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

  public static void writeFile(Path outputPath, String output) {
    try (FileOutputStream fs = new FileOutputStream(outputPath.toFile());
        OutputStreamWriter os = new OutputStreamWriter(fs, UTF_8)) {
      os.write(output);
    } catch (FileNotFoundException e) {
      throw new BatfishException("Failed to write file (file not found): " + outputPath, e);
    } catch (IOException e) {
      throw new BatfishException("Failed to write file: " + outputPath, e);
    }
  }
}
