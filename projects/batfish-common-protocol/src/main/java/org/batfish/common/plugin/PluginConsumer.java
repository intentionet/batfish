package org.batfish.common.plugin;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectInputStream;

public abstract class PluginConsumer implements IPluginConsumer {

  /**
   * A byte-array containing the first 4 bytes of the header for a file that is the output of java
   * serialization
   */
  private static final byte[] JAVA_SERIALIZED_OBJECT_HEADER = {
    (byte) 0xac, (byte) 0xed, (byte) 0x00, (byte) 0x05
  };

  private ClassLoader _currentClassLoader;

  private final boolean _serializeToText;

  public PluginConsumer(boolean serializeToText) {
    _currentClassLoader = Thread.currentThread().getContextClassLoader();
    _serializeToText = serializeToText;
  }

  protected <S extends Serializable> S deserializeObject(byte[] data, Class<S> outputClass) {
    ByteArrayInputStream stream = new ByteArrayInputStream(data);
    return deserializeObject(stream, outputClass);
  }

  protected <S extends Serializable> S deserializeObject(InputStream stream, Class<S> outputClass) {
    try {
      // Allows us to peek at the beginning of the stream and then push the bytes back in for
      // downstream consumers to read.
      PushbackInputStream pbstream =
          new PushbackInputStream(stream, JAVA_SERIALIZED_OBJECT_HEADER.length);
      boolean isJavaSerializationData = isJavaSerializationData(pbstream);
      ObjectInputStream ois;
      if (!isJavaSerializationData) {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        xstream.setClassLoader(_currentClassLoader);
        ois = xstream.createObjectInputStream(pbstream);
      } else {
        ois = new BatfishObjectInputStream(pbstream, _currentClassLoader);
      }
      Object o = ois.readObject();
      return outputClass.cast(o);
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      throw new BatfishException(
          "Failed to deserialize object of type '" + outputClass.getCanonicalName() + "' from data",
          e);
    }
  }

  protected <S extends Serializable> S deserializeObject(Path inputFile, Class<S> outputClass) {
    try {
      // Awkward nested try blocks required because we refuse to throw IOExceptions.
      try (Closer closer = Closer.create()) {
        FileInputStream fis = closer.register(new FileInputStream(inputFile.toFile()));
        BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
        GZIPInputStream gis = closer.register(new GZIPInputStream(bis));
        return deserializeObject(gis, outputClass);
      }
    } catch (IOException e) {
      throw new BatfishException(
          String.format(
              "Failed to deserialize object of type %s from file %s",
              outputClass.getCanonicalName(), inputFile),
          e);
    }
  }

  protected byte[] fromGzipFile(Path inputFile) {
    try {
      // Awkward nested try blocks required because we refuse to throw IOExceptions.
      try (Closer closer = Closer.create()) {
        FileInputStream fis = closer.register(new FileInputStream(inputFile.toFile()));
        GZIPInputStream gis = closer.register(new GZIPInputStream(fis));
        return IOUtils.toByteArray(gis);
      }
    } catch (IOException e) {
      throw new BatfishException("Failed to gunzip file: " + inputFile, e);
    }
  }

  public ClassLoader getCurrentClassLoader() {
    return _currentClassLoader;
  }

  public abstract PluginClientType getType();

  /**
   * Determines whether the data in the stream is Java serialized bytes. Requires a {@link
   * PushbackInputStream} so that the inspected bytes can be put back into the stream after reading.
   */
  private boolean isJavaSerializationData(PushbackInputStream stream) throws IOException {
    byte[] header = new byte[JAVA_SERIALIZED_OBJECT_HEADER.length];
    ByteStreams.readFully(stream, header);
    boolean ret = Arrays.equals(header, JAVA_SERIALIZED_OBJECT_HEADER);
    stream.unread(header);
    return ret;
  }

  protected final void loadPlugins() {
    SortedSet<Plugin> plugins;
    try {
      plugins =
          new TreeSet<>(Lists.newArrayList(ServiceLoader.load(Plugin.class, _currentClassLoader)));
    } catch (ServiceConfigurationError e) {
      throw new BatfishException("Failed to locate and/or instantiate plugins", e);
    }
    List<BatfishException> initializationExceptions = new ArrayList<>();
    for (Plugin plugin : plugins) {
      try {
        plugin.initialize(this);
      } catch (Exception e) {
        initializationExceptions.add(
            new BatfishException(
                "Failed to initialize plugin: " + plugin.getClass().getCanonicalName(), e));
      }
    }
    if (!initializationExceptions.isEmpty()) {
      BatfishException e = new BatfishException("Failed to initialize one or more plugins");
      initializationExceptions.forEach(e::addSuppressed);
      throw e;
    }
  }

  /** Serializes the given object to a file with the given output name, using GZIP compression. */
  public void serializeObject(Serializable object, Path outputFile) {
    try {
      try (Closer closer = Closer.create()) {
        OutputStream out = closer.register(Files.newOutputStream(outputFile));
        BufferedOutputStream bout = closer.register(new BufferedOutputStream(out));
        serializeToGzipData(object, bout);
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Failed to serialize object to gzip output file: " + outputFile, e);
    }
  }

  /** Serializes the given object to a GZIP-compressed byte[]. */
  protected byte[] toGzipData(Serializable object) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    serializeToGzipData(object, baos);
    return baos.toByteArray();
  }

  private static class CloseIgnoringOutputStream extends FilterOutputStream {

    protected CloseIgnoringOutputStream(OutputStream out) {
      super(out);
    }

    /** Does nothing, deliberately. */
    @Override
    public void close() {}
  }

  /** Serializes the given object to the given stream, using GZIP compression. */
  private void serializeToGzipData(Serializable object, OutputStream out) {
    // This is a hack:
    //   XStream requires that its streams be closed to properly finish serialization,
    //   but we do not actually want to close the passed-in output stream.
    out = new CloseIgnoringOutputStream(out);

    try (Closer closer = Closer.create()) {
      GZIPOutputStream gos = closer.register(new GZIPOutputStream(out));
      ObjectOutputStream oos;
      if (_serializeToText) {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        oos = closer.register(xstream.createObjectOutputStream(gos));
      } else {
        oos = closer.register(new ObjectOutputStream(gos));
      }
      oos.writeObject(object);
    } catch (IOException e) {
      throw new BatfishException("Failed to convert object to gzip data", e);
    }
  }
}
