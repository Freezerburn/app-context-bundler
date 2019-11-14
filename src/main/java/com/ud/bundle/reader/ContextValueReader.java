package com.ud.bundle.reader;

import com.google.gson.Gson;
import com.ud.bundle.AppContext;

public interface ContextValueReader {

  void readInto(final AppContext ctx);

  default Gson getGson() {
    return new Gson();
  }
}
