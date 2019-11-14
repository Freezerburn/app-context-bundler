package com.ud.bundle.reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ud.bundle.AppContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;

public class JsonContextValueReader implements ContextValueReader {

  private final BufferedReader reader;
  private Gson gson;
  private JsonObject readerJson;

  public JsonContextValueReader(final String content) {
    reader = new BufferedReader(new StringReader(content));
  }

  public JsonContextValueReader(final File jsonFile) {
    try {
      reader = new BufferedReader(new FileReader(jsonFile));
    } catch (final FileNotFoundException e) {
      throw new IllegalArgumentException("File was not found.", e);
    }
  }

  public JsonContextValueReader(final Reader reader) {
    if (reader instanceof BufferedReader) {
      this.reader = (BufferedReader) reader;
    } else {
      this.reader = new BufferedReader(reader);
    }
  }

  public JsonContextValueReader(final URL jsonFileUrl) {
    try {
      reader = new BufferedReader(new InputStreamReader(jsonFileUrl.openStream()));
    } catch (final IOException e) {
      throw new IllegalArgumentException("Failed to open a reader from the URL input stream.", e);
    }
  }

  public JsonContextValueReader(final Path jsonFilePath) {
    try {
      reader = new BufferedReader(new FileReader(jsonFilePath.toFile()));
    } catch (final FileNotFoundException e) {
      throw new IllegalArgumentException("File was not found.", e);
    }
  }

  @Override
  public void readInto(final AppContext ctx) {
    try {
      if (readerJson == null) {
        gson = getGson();
        final var sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
        final var tree = gson.fromJson(sb.toString(), JsonObject.class);
        if (!tree.isJsonObject()) {
          throw new IllegalStateException("Root level of JSON for an AppContext must be an object.");
        }
        readerJson = tree.getAsJsonObject();
      }
    } catch (final IOException e) {
      throw new IllegalArgumentException("Failed to read the contents of JSON provider.", e);
    }

    final var sb = new StringBuilder();
    final var st = new ArrayDeque<>(readerJson.entrySet());
    while (!st.isEmpty()) {
      final var el = st.pop();

      final var currentKey = el.getKey();
      final var currentValue = el.getValue();
      if (currentValue.isJsonPrimitive()) {
        final var p = currentValue.getAsJsonPrimitive();
        if (p.isString()) {
          ctx.registerValue(currentKey, p.getAsString());
        } else if (p.isNumber()) {
          ctx.registerValue(currentKey, p.getAsNumber());
        } else if (p.isBoolean()) {
          throw new UnsupportedOperationException("TODO: Support booleans as values.");
        }
      } else if (currentValue.isJsonObject()) {
        for (final Map.Entry<String, JsonElement> entry : currentValue.getAsJsonObject().entrySet()) {
          final var key = entry.getKey();
          sb.append(currentKey).append('.').append(key);
          st.add(Map.entry(sb.toString(), entry.getValue()));
          sb.setLength(0);
        }
      } else if (currentValue.isJsonArray()) {
        final var arr = currentValue.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
          st.add(Map.entry(Integer.toString(i), arr.get(i)));
        }
      }
    }
  }
}
