package com.macmario.services.web.jproxy;

import java.io.IOException;

public interface jHttpProxyInputStreamInf {
  /** reads the data */
  public int read_f(byte[] b) throws IOException;
}
