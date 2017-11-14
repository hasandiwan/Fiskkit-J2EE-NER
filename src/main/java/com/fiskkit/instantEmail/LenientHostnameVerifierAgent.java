package com.fiskkit.instantEmail;
import java.lang.instrument.Instrumentation;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class LenientHostnameVerifierAgent {
  public static void premain(String args, Instrumentation inst) {
    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
      public boolean verify(String s, SSLSession sslSession) {
        return true;
      }
    });
  }
}
