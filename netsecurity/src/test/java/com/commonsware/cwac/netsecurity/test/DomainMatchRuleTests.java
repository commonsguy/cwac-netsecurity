/***
 Copyright (c) 2017 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License _is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.netsecurity.test;

import org.junit.Test;
import java.util.regex.Pattern;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.allOf;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.anyOf;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.blacklist;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.is;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.not;
import static com.commonsware.cwac.netsecurity.DomainMatchRule.whitelist;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DomainMatchRuleTests {
  @Test
  public void _is() {
    assertTrue(is("foo.com").matches("foo.com"));
    assertFalse(is("*.foo.com").matches("foo.com"));
    assertTrue(is("*.foo.com").matches("www.foo.com"));
    assertTrue(is("*.foo.com").matches("www.bar.foo.com"));
    assertTrue(is(Pattern.compile("[a-z]+\\.com")).matches("foo.com"));
    assertFalse(is(Pattern.compile("[a-z]+\\.com")).matches("www.foo.com"));
  }

  @Test
  public void _not() {
    assertFalse(not(is("foo.com")).matches("foo.com"));
    assertTrue(not(is("*.foo.com")).matches("foo.com"));
    assertFalse(not(is("*.foo.com")).matches("www.foo.com"));
    assertFalse(not(is(Pattern.compile("[a-z]+\\.com"))).matches("foo.com"));
    assertTrue(not(is(Pattern.compile("[a-z]+\\.com"))).matches("www.foo.com"));
  }

  @Test
  public void _anyOf() {
    assertTrue(anyOf(is("foo.com")).matches("foo.com"));
    assertFalse(anyOf(is("*.foo.com")).matches("foo.com"));
    assertTrue(anyOf(is("*.foo.com")).matches("www.foo.com"));

    assertTrue(anyOf(is("foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(anyOf(is("*.foo.com"), is("bar.com")).matches("foo.com"));
    assertTrue(anyOf(is("*.foo.com"), is("bar.com")).matches("www.foo.com"));

    assertTrue(anyOf(is("goo.com"), is("foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(anyOf(is("goo.com"), is("*.foo.com"), is("bar.com")).matches("foo.com"));
    assertTrue(anyOf(is("goo.com"), is("*.foo.com"), is("bar.com")).matches("www.foo.com"));
  }

  @Test
  public void _allOf() {
    assertTrue(allOf(is("foo.com")).matches("foo.com"));
    assertFalse(allOf(is("*.foo.com")).matches("foo.com"));
    assertTrue(allOf(is("*.foo.com")).matches("www.foo.com"));

    assertFalse(allOf(is("foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(allOf(is("*.foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(allOf(is("*.foo.com"), is("bar.com")).matches("www.foo.com"));

    assertFalse(allOf(is("goo.com"), is("foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(allOf(is("goo.com"), is("*.foo.com"), is("bar.com")).matches("foo.com"));
    assertFalse(allOf(is("goo.com"), is("*.foo.com"), is("bar.com")).matches("www.foo.com"));
  }

  @Test
  public void _whitelist() {
    assertTrue(whitelist("foo.com").matches("foo.com"));
    assertFalse(whitelist("*.foo.com").matches("foo.com"));
    assertTrue(whitelist("*.foo.com").matches("www.foo.com"));

    assertTrue(whitelist("foo.com", "bar.com").matches("foo.com"));
    assertFalse(whitelist("*.foo.com", "bar.com").matches("foo.com"));
    assertTrue(whitelist("*.foo.com", "bar.com").matches("www.foo.com"));

    assertTrue(whitelist("goo.com", "foo.com", "bar.com").matches("foo.com"));
    assertFalse(whitelist("goo.com", "*.foo.com", "bar.com").matches("foo.com"));
    assertTrue(whitelist("goo.com", "*.foo.com", "bar.com").matches("www.foo.com"));
  }

  @Test
  public void _blacklist() {
    assertFalse(blacklist("foo.com").matches("foo.com"));
    assertTrue(blacklist("*.foo.com").matches("foo.com"));
    assertFalse(blacklist("*.foo.com").matches("www.foo.com"));

    assertFalse(blacklist("foo.com", "bar.com").matches("foo.com"));
    assertTrue(blacklist("*.foo.com", "bar.com").matches("foo.com"));
    assertFalse(blacklist("*.foo.com", "bar.com").matches("www.foo.com"));

    assertFalse(blacklist("goo.com", "foo.com", "bar.com").matches("foo.com"));
    assertTrue(blacklist("goo.com", "*.foo.com", "bar.com").matches("foo.com"));
    assertFalse(blacklist("goo.com", "*.foo.com", "bar.com").matches("www.foo.com"));
  }
}
