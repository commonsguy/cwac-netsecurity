# Advanced CWAC-NetSecurity Usage

If [the basics](https://github.com/commonsguy/cwac-netsecurity#basic-usage)
are too basic, here are some other things that you can do using
`netsecurity`.

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

## Certificate Memorization

Certificate memorization can be thought of as "on-the-fly certificate pinning".
Basically, as we encounter certificates in the wild, we can elect to say
"yes, that certificate is fine, we will keep accepting it", even if it otherwise
conflicts with whatever network security configuration that we have set up.

This involves a `MemorizingTrustManager`, which is an `X509TrustManager`
that happens to handle certificate memorization.

### Creating a MemorizingTrustManager

`MemorizingTrustManager` follows a typical builder pattern:

- Create an instance of `MemorizingTrustManager.Builder`
- Call methods on that builder to configure what you want
- Call `build()` on the `Builder` to get a `MemorizingTrustManager`

#### saveTo()

The one `Builder` method that is required is `saveTo()`. This indicates where
and how the memorized certificate information should be stored. There are
two variants of this method, taking two and three parameters, respectively.

The first parameter for both variants is a `File` object. This needs to point
to a place where `MemorizingTrustManager` can save its certificate information,
in the form of keystore files. This `File` should point to a unique spot
on internal storage, separate from any other location that you might be using.
So, use `getCacheDir()` or `getFilesDir()` on `Context` to get a base directory,
then create some `File` off of it (e.g., `new File(getCacheDir(), "foo")`).
This file should not already exist; `MemorizingTrustManager.Builder` will
create a directory at this location.

The second parameter is a `char[]` representing the passphrase to use for
encrypting the keystores. This value is held in memory for as long as the
`MemorizingTrustManager` is in use, so as certificates get memorized, we can
save them to disk. There are a variety of possibilities here:

- Use a hard-coded value, if your threat vectors do not involve people rooting
a device and making note of what certificates the user has memorized
 
- Use a value uniquely derived from a user-supplied passphrase (e.g., cryptographically
secure hash with a unique salt)

- Use a value stored in a hardware-backed `KeyStore` or encrypted via a key in
such a `KeyStore`

The three-parameter `saveTo()` also takes the keystore file type. In the
two-parameter version of `saveTo()`, this defaults to `KeyStore.getDefaultType()`.
However, the range of supported types has varied over the years
(see [the `KeyStore` JavaDocs](https://developer.android.com/reference/java/security/KeyStore.html)).
Hence, you might want to specifically state what keystore type that you want,
based on your supported API levels.

#### Other Configuration Methods

These configuration methods are optional:

- `noTOFU()` is to disable automatic trust-on-first-use behavior, described
below.

- `cacheSize()` indicates how many domains' worth of memorized certificates
should be held in cache (default: 128).

- `forDomains()` indicates which domains should be memorized; all other
domains will be ignored, as if this `TrustManager` were not involved. The
default is to memorize all domains. This method takes a `DomainMatchRule`,
described in detail later in this page.

- `onlySingleItemChains()` indicates that we should only memorize certificate
chains with a single item. This should effectively limit memorization to
self-signed certificates (as any CA-backed certificate should have a longer
chain). By default, this is disabled, and so memorization is applied regardless
of chain length.

### Adding the MemorizingTrustManager

`MemorizingTrustManager` is an `X509TrustManager` that also implements
the `X509TrustManagerExtensions` methods. In principle, it could be used
directly by anything needing a `TrustManager`.

In practice, it is designed to be used by the `CompositeTrustManager`
that you get from a `TrustManagerBuilder`, as is described in
[the main project `README](../README.markdown). There are three likely
patterns here:

- Configure the `TrustManagerBuilder` with just the `MemorizingTrustManager`.

- Configure the `TrustManagerBuilder` to use the `MemorizingTrustManager`
*or* some other network security configuration. For example, "we will support
this network security configuration; anything failing that will be checked
against memorized certificates". To do that, use `or()`:

```java
MemorizingTrustManager memo=new MemorizingTrustManager.Builder()
  .saveTo(memoDir, "sekrit".toCharArray())
  .build();

TrustManagerBuilder tmb=new TrustManagerBuilder()
  .withConfig(ctxt, R.xml.something, BuildConfig.DEBUG)
  .or()
  .add(memo);
```

(where `R.xml.something` is your network security configuration XML,
`memoDir` is a place to write memorized certificates, and `ctxt` is a `Context`)

- Configure the `TrustManagerBuilder` to use the `MemorizingTrustManager`
*and* some other network security configuration. In this case, the certificate
must pass both tests: match the network security configuration *and* be memorized.
To do this, use `and()` in place of `or()` in the preceding example.

### What Happens Now

If the `MemorizingTrustManager` is asked to check a certificate chain for
some host, and that certificate chain has been memorized, no exceptions
will be raised.

By default, `MemorizingTrustManager` works in trust-on-first-use (TOFU) mode.
The first time that we encounter a new domain, we assume that whatever certificates
that we receive are good, and we memorize them.

When a validation failure occurs, your HTTP API will throw an `SSLHandshakeException`
from whatever method actually does the HTTP I/O (e.g., `execute()` in OkHttp3).
You need to look at the wrapped exception, obtained by calling `getCause()`
on the `SSLHandshakeException`. There are two main possibilities here:

1. The wrapped exception is a `CertificateNotMemorizedException`. This means
that we have no certificates memorized for the host identified in the URL that
you attempted to access. This will only occur if you disabled TOFU by calling
 `noTOFU()` on the `MemorizingTrustManager.Builder`.

2. The wrapped exception is a `MemorizationMismatchException`. This means that
we *do* have certificates memorized for this host, but they do not match the
certificates that we just got from the server.

Both of these inherit from a base `MemorizationException` class.

If your `SSLHandshakeException` has anything else as its cause, either there
was a serious malfunction in the `MemorizingTrustManager` (e.g., cannot access
files in the directory supplied to `saveTo()`), or there was some other
problem.

### Memorizing a Certificate: Manually

If you get a `MemorizationException` (either `CertificateNotMemorizedException` or
`MemorizationMismatchException`), and you want to have the `MemorizingTrustManager`
memorize that certificate chain for use in future requests, you have two methods
that you can call on the manager:

- `memorize()` memorizes the certificate chain, saving the data in a keystore
file in the designated directory.

- `memorizeForNow()` memorizes the certificate chain, but only in memory. This
might be used for temporary situations (e.g., the user wishes to proceed for
now but wants to talk to the IT department to determine if the server did indeed
change certificates). The "for now" will be until the process terminates or
until this domain's memorized certificates fall out of the cache maintained
by `MemorizingTrustManager`.

So, for memorization, you can either have it happen automatically
or "manually" (using `noTOFU()` and `memorize()`). Mostly, it boils down to
whether you want to have any form of user decision as to whether to memorize
the certificate when first encountered. If the user should make the call, use
`noTOFU()` and `memorize()`. If you do not want to bother the user (and assume 
that the first use is actually giving valid certificates), use the defaults.

Note that not all HTTP APIs are well-suited for manual mode. For example,
with Picasso, while we can find out about exceptions (via `listener()` on
the `Picasso.Builder`), we are not given enough information to automatically
retry the request after calling `memorize()`.

### Clearing Memorization

`MemorizingTrustManager` has two methods to let you clear out memorized certificates:

- `clear()`, which takes the name of the host whose certificates should be cleared
- `clearAll()`, to clear all certificates

Both methods also take a `boolean`: `true` if you want persistent certificates
to be cleared, `false` if you only want to clear `memorizeForNow()` certificates

If your HTTP client API caches `SSLSession` objects (such as OkHttp3), then
`clear()`/`clearAll()` will only take effect when those sessions expire. Until
then, the trust managers are no longer consulted.

### Rules for Memorization

Use the same `MemorizingTrustManager` instance consistently. Having two
or more instances can get you into trouble, as they do not coordinate with
each other. So, a certificate memorized in one will not be known by another
instance that was outstanding at the time.

The test suite for this library does use multiple `MemorizingTrustManager`
instances, mostly to confirm that certificates do get loaded from disk.

### Memorizing Certain Domains

By default, `MemorizingTrustManager` applies for all domains that it sees.

Alternatively, you can use `forDomains()` to limit the scope of the domains
that `MemorizingTrustManager` worries about. `forDomains()` takes a
`DomainMatchRule` as a parameter.

The simplest ways to create a `DomainMatchRule` are the `whitelist()`
and `blacklist()` static methods on that class. They each take one or more
`String` parameters representing domains. Those can either be simple domains
(e.g., `foo.com`) or with a leading wildcard (e.g., `*.foo.com`). `whitelist()`
will apply the `MemorizingTrustManager` for the specified domains and skip
it for anything else. `blacklist()` will skip the `MemorizingTrustManager`
for the specified domains and apply it for anything else.

`DomainMatchRule` has other static methods for more granular control:

- `is(String)` takes a domain name or wildcard domain name and matches it
but nothing else

- `is(Pattern)` takes a `Pattern` (i.e., regular expression) and matches
it but nothing else

- `not(DomainMatchRule)` takes some other rule (e.g., one returned by `is()`)
and inverts it

- `anyOf(DomainMatchRule...)` and `anyOf(List<DomainMatchRule>)` apply a logical
OR, so a domain matching any of those rules is applied

- `allOf(DomainMatchRule...)` and `allOf(List<DomainMatchRule>)` apply a logical
AND, so only domains matching all of those rules are applied

For example, `whitelist()` is implemented by wrapping each supplied domain
in `is()` and using `anyOf()` for the collection, so we accept any of those
domains but nothing else. `blacklist()` is implemented by wrapping each supplied
domain in `not(is())` and using `allOf()` for the collection, so we only accept
domains that are not any of the supplied ones.

## Integration with NetCipher

[NetCipher](https://github.com/guardianproject/NetCipher) is a library to
simplify integration between an app and Orbot, which is a Tor client.

The `com.commonsware.cwac:netsecurity-netcipher` artifact provides a
`StrongOkHttpClientBuilderEx` that blends NetCipher and `TrustManagerBuilder`,
so you can apply network security configuration (including certificate
memorization/TOFU) and NetCipher.

The NetCipher documentation explains
[how to use the `StrongBuilder` family of classes](https://github.com/guardianproject/NetCipher#the-strong-builders).
`StrongOkHttpClientBuilderEx` follows the same basic pattern, though your
best way to set one up is to call the static `newInstance()` method, supplying
a `Context` along with your configured `TrustManagerBuilder`:

```java
StrongOkHttpClientBuilderEx
    .newInstance(getActivity(), tmb)
    .build(this);
```          

(where `tmb` is the `TrustManagerBuilder`)

When your `StrongBuilder.Callback<OkHttpClient>` callback is called with
`onConnected()`, the `OkHttpClient` that you receive will be configured
both for Orbot and for the configuration you set up for your `TrustManagerBuilder`.

However:

- The NetCipher SSL configuration is not used. Set up all your SSL rules
via the `TrustManagerBuilder` and the associated network security configuration
XML files.

- `withTorValidation()` on the `StrongOkHttpClientBuilderEx` is not supported
right now, while some bugs get ironed out.

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
the `OkHttp3Integrator` class in the `netsecurity` library.
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

This is handled automatically for OkHttp3 and `HttpUrlConnection`
in the existing library code.

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
