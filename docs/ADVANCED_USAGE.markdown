# Advanced CWAC-NetSecurity Usage

If [the basics](https://github.com/commonsguy/cwac-netsecurity#basic-usage)
are too basic, here are some other things that you can do using
`netsecurity` and `netsecurity-okhttp3`.

## Using Alternative Network Security Configuration XML

`withManifestConfig()` on `TrustManagerBuilder` uses the resource
that you declare in your manifest as the network security configuration
to apply. However, that is fairly inflexible, as you can only define
this in the manifest once. Also, `withManifestConfig()` performs the
version check to only apply the backport on pre-7.0 devices.

You can also use `withConfig()`, where you provide a `Context` and the resource ID
of the XML resource to use for the network security configuration.
This is useful for cases where:

- You want to always use the backport, for consistent behavior across
OS versions

- You want to use different configurations in different settings
for the same APK

For example, the test suites use `withConfig()`, as otherwise we would
need dozens of separate manifests.

## Using the Backport Directly

You do not have to use `TrustManagerBuilder` to use the network security
configuration backport. If you wish to use it directly:

- Create an instance of `ApplicationConfig`, passing in a `ConfigSource`
implementation that indicates where the configuration should be pulled
from. Two likely `ConfigSource` implementations are `ManifestConfigSource`
(to use the one defined in the manifest) and `XmlConfigSource` (to
use one defined in an arbitrary XML resource).

- Call `getTrustManager()` on the `ApplicationConfig` to get a `TrustManager`
that will implement the requested configuration.

- Add that `TrustManager` to your HTTP client via whatever API that
client offers for such things. In many cases, that will be by configuring
an `SSLContext` to use the `TrustManager`, then using the `SSLContext`
(or an `SSLSocketFactory` created by the `SSLContext`) with your
HTTP client.

## Integrating with Other HTTP Client Libraries

If you want to integrate `TrustManagerBuilder` and the network security
configuration backport with some other HTTP client API, start by reviewing
the `OkHttp3Integrator` class in the `netsecurity-okhttp3` library.
This will give you an idea of what is required and how easy it will
be to replicate this class for your particular HTTP client API.

### Adding the TrustManager

Calling `build()` on the `TrustManagerBuilder` gives you a
`CompositeTrustManager`, set up to implement your desired network
security configuration. You will need to add that to your HTTP client
by one means or another. If `size()` on the `CompositeTrustManager`
returns 0, though, you can skip it, as it means that there are no rules
to be applied (e.g., you used `withManifestConfig()`, and your app
is running on an Android 7.0+ device).

So, you might have code that looks like this, where `tmb` is a
configured `TrustManagerBuilder`:

```java
CompositeTrustManager trustManager=tmb.build();

if (trustManager.size()>0) {
  SSLContext ssl=SSLContext.getInstance("TLS");
  X509Interceptor interceptor=new X509Interceptor(trustManager, tmb);

  ssl.init(null, new TrustManager[]{trustManager}, null);

  // apply the SSLContext or ssl.getSocketFactory() to your HTTP client
}
```

### Handling Cleartext

You can call `isCleartextTrafficPermitted()` on the `CompositeTrustManager`
to determine if cleartext traffic should be supported. This takes the
domain name of the Web server you are going to be communicating with
and returns a simple `boolean`. If `isCleartextTrafficPermitted()`
returns `false`, you will need to examine the scheme of the URL and
accept or reject the HTTP operation accordingly.

If you fail to do this, then cleartext traffic will be allowed in all
cases, akin to the stock `HttpURLConnection` integration.

### Handling Domains

Before actually making the HTTPS request, ideally you call `setHost()`
on the `CompositeTrustManager`, to tell it the domain name of the
upcoming HTTP request. If you fail to do this, and your app is running
on an Android 4.2-6.0 device, any `<domain-config>` rules will
be ignored, akin to the stock `HttpURLConnection` integration.

### Handling Redirects

If your HTTP client automatically traverses server-side redirects
(making the HTTP request for the redirected-to URL), you will need
to handle the cleartext check and the `setHost()` call on every
step of the redirection, not just your initial request. In the
case of OkHttp3, this is accomplished via their interceptor framework.

## Debugging Certificate Chains

You can call `withCertChainListener()` on `TrustManagerBuilder`,
providing an implementation of `CertChainListener`. Your listener
will be called with `onChain()` each time a certificate chain is
encountered. In `onChain()`, you can inspect the certificates, dump
their contents to LogCat, or whatever you wish to do.

This is designed for use in development. For example, when writing
the `demo/` app, the author used a `CertChainListener` to log what
HTTP requests were being made, what domains those were for, and what
root certificates are being used. This in turn led to creating the
network security configuration that matched.

However, logging certificate chains on a production device may result
in security issues. Please only use `CertChainListener` in debug
builds.
