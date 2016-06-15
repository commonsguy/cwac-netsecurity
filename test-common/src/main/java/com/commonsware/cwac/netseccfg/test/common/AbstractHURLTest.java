package com.commonsware.cwac.netseccfg.test.common;

import android.support.test.runner.AndroidJUnit4;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(AndroidJUnit4.class)
abstract public class AbstractHURLTest {
  abstract protected String getUrl();
  abstract protected String getExpectedResponse();

  @Test
  public void testRequest() throws IOException {
    HttpURLConnection c=
      (HttpURLConnection)new URL(getUrl()).openConnection();
    InputStream in=c.getInputStream();

    try {
      Assert.assertEquals(getExpectedResponse(), slurp(in));
    }
    finally {
      in.close();
    }
  }

  // based on http://stackoverflow.com/a/309718/115145

  public static String slurp(final InputStream is)
    throws IOException {
    final char[] buffer = new char[128];
    final StringBuilder out = new StringBuilder();
    final Reader in = new InputStreamReader(is, "UTF-8");

    for (;;) {
      int rsz = in.read(buffer, 0, buffer.length);
      if (rsz < 0)
        break;
      out.append(buffer, 0, rsz);
    }

    return out.toString();
  }
}
