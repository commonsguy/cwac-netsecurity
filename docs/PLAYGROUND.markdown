# Using the Demo Playground

The `demo/` module in this project represents a "playground" app. It consists
of two activities:

- One with a series of preferences to control the behavior of CWAC-NetSecurity

- One that applies those preferences, in the context of showing a `ListView`
of the latest `android` questions on Stack Overflow, using Retrofit and Picasso,
wrapped around OkHttp3

Given a set of preferences, clicking the "Run" option in the action bar will
launch the activity to show the list of Stack Overflow questions, using
a `TrustManagerBuilder` configured to specifications.

## Default Behavior

By default, all of the various preferences are effectively off. In that
case, CWAC-NetSecurity applies a basic network security configuration,
pinning `api.stackexchange.com`, `graph.facebook.com`, and `www.gravatar.com`
to only certificates from the root authorities that they are presently using.

The only way this should fail, from a network security configuration standpoint,
is if one of those services not only switches certificates but also switches
certificate authorities. That may well happen, and this demo app will be updated
later on to match that. If the demo app has not yet been updated, and this
is causing problems for your use of this library, file an issue and the author
will work to get it fixed.

## Testing MITM Attacks

The "Use proxy?" switch is designed to allow you to test CWAC-NetSecurity against
a MITM proxy server, such as `mitmproxy`, to confirm that certificates get
rejected.

To do this, set up your desired MITM proxy server as normal (e.g., via
[this `mitmproxy` guide](https://blog.heckel.xyz/2013/07/01/how-to-use-mitmproxy-to-read-and-modify-https-traffic-of-your-phone/)).
Then, turn on the "Use proxy?" switch, then fill in the proxy host and port
in their preferences. The default values for those are suitable for an Android
emulator, where `mitmproxy` is running on the development machine with the default
8080 port.

If you click "Run" with a MITM proxy enabled, you should fail with a certificate
validation error, as the proxy's certificate will not match the certificates
used in the network security configuration.

## Testing Memorization

The "Enable memorization?" switch allows you to toggle on certificate
memorization support. There are a few different ways that this will work,
for testing different scenarios, based on some dependent preferences.

### With TOFU

The default state with "Enable memorization?" turned on is that "Enable TOFU?"
and "Also apply network security config?" are also both turned on. In that case,
when you click "Run", it should work normally, as the certificates that we
encounter will be trusted on first use.

"Also apply network security config?" controls the overall `TrustManagerBuilder`
configuration:

- On: we check *both* the pinned CAs *and* the memorization

- Off: we only use memorization

### Without TOFU

If you turn off TOFU, and you have no currently memorized certificates,
what you should see is:

- A `Toast` appear, saying that we are memorizing a certificate

- The list of Stack Overflow questions appear

- The avatars associated with those questions *not* appear

The latter is because Picasso is not well-suited for non-TOFU certificate
memorization.

### Testing Memorization Mismatch

The key test with certificate memorization is if it will detect MITM
attacks. To do this:

- Clear the memorized certificates (see below)

- Click "Run" with memorization enabled, to memorize the relevant certificates

- Set up the MITM proxy per the instructions above, including the
"Use proxy?" and related preferences in the playground app

- Enable memorization but turn off "Also apply manifest config?" (otherwise,
you will fail because the network security configuration does not validate
the MITM proxy's certificate)

- Click "Run", and you should see in the resulting stack trace that
you got a `MemorizationMismatchException`

### Clearing Certificates

On the launcher activity (the one with "Run" and all the preferences), the
action bar overflow menu has a "Clear" option, which wipes out all of the
memorized certificates.

## NetCipher

If "Use NetCipher?" is on, then the playground will blend in
[NetCipher](https://github.com/guardianproject/NetCipher) and run your requests
through Orbot, if you have Orbot installed.

Note that this option is mutually exclusive with the "Use proxy?" option &mdash;
if you turn on one, the other is turned off.

## Logging Certificate Chains

The "Log certificate chains?" switch will log information about the encountered
certificates to LogCat.
