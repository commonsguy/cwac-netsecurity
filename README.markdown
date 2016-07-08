# CWAC-NetSecurity: Simplifying Secure Internet Access

This library contains a backport of
[the Android 7.0 network security configuration](https://developer.android.com/preview/features/security-config.html)
subsystem. In Android 7.0, this subsystem makes it easier for developers
to tie their app to particular certificate authorities or certificates,
support self-signed certificates, and handle other advanced SSL
certificate scenarios. This backport allows the same XML configuration
to be used, going back to API Level 17 (Android 4.2).

This library also offers a `TrustManagerBuilder` and related classes
to make it easier for developers to integrate the network security
configuration backport, particularly for
[OkHttp3](https://github.com/square/okhttp)
and `HttpURLConnection`.

## Installation

The artifacts for this library are distributed via the CWAC repository,
so you will need to configure that in your module's `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}
```

If you are using this library with OkHttp3, you will want to integrate
the `netsecurity-okhttp3` dependency:

```groovy
dependencies {
    compile 'com.commonsware.cwac:netsecurity-okhttp3:0.0.1'
}
```

If you are using `HttpURLConnection`, or tying this code into some
other HTTP client stack, use the `netsecurity` dependency (which
`netsecurity-okhttp3` pulls in by reference):

```groovy
dependencies {
    compile 'com.commonsware.cwac:netsecurity:0.0.1'
}
```

## Usage

TBD

## Dependencies

Not surprisingly, `netcipher-okhttp3` depends upon OkHttp3.