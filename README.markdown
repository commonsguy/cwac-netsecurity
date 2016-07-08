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

Not surprisingly, `netsecurity-okhttp3` depends upon OkHttp3. This library
should fairly closely track the latest OkHttp3 release. If you find
that the library has fallen behind, please
[file an issue](https://github.com/commonsguy/cwac-netsecurity/issues)
if one has not already been filed.

`netsecurity` depends upon the `support-annotations` from the Android SDK.

Otherwise, there are no external dependencies.

## Version

The current version is **0.0.1**.

Right now, before Android 7.0 ships, version numbers have limited basis
in reality. Once Android 7.0 ships, though, the version numbers will
reflect the AOSP code that the backport is based upon:

- Major version = API level of the AOSP code (e.g., 24)
- Minor version = Compatibility-breaking changes, due to AOSP code changes or changes to the library's own code (e.g., `TrustManagerBuilder`)
- Patch version = General bug fixes that should not require changes to apps using the library

## Demo

The `demo/` module is an Android app that uses OkHttp3, Retrofit,
and Picasso to show the latest Android questions on Stack Overflow
in a `ListView`. Retrofit and Picasso use a common OkHttp3-defined
`OkHttpClient` object, and that client uses `netcipher-okhttp3` to
ensure that connections to key hosts, such as the Stack Exchange
Web service API, use SSL certificates from the expected certificate
authorities.

## License

All of the code in this repository is licensed under the
Apache Software License 2.0. Look to the headers of the Java source
files to determine the actual copyright holder, as it is a mix of
the Android Open Source Project and CommonsWare, LLC.

## Questions

If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+camera). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please read [the contribution guidelines](.github/CONTRIBUTING.md), then
post an [issue](./issues).
**Be certain to include complete steps for reproducing the issue.**

Do not ask for help via social media.

## Release Notes

- v0.0.1: initial release
