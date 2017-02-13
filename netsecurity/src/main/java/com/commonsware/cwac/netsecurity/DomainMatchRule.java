/***
 Copyright (c) 2017 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.netsecurity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a rule for identifying matching domain names. This is used
 * by MemorizingTrustManager to limit the scope of memorization to only
 * domains matching a rule.
 *
 * The whitelist() and blacklist() static methods implement two common
 * patterns (accept only domains in a list or deny a list of domains and
 * accept everything else). The other static methods allow you to assemble
 * other scenarios. These methods are designed to be used as static imports,
 * akin to Hamcrest matchers.
 */
abstract public class DomainMatchRule {
  abstract public boolean matches(String host);

  /**
   * Implements a logical AND: only domains matching all of the supplied
   * rules are considered to be a match.
   *
   * @param rules Rules to apply and AND together
   * @return Rule implementing the AND logic
   */
  public static DomainMatchRule allOf(DomainMatchRule... rules) {
    return(new Composite(false, rules));
  }

  /**
   * Implements a logical AND: only domains matching all of the supplied
   * rules are considered to be a match.
   *
   * @param rules Rules to apply and AND together
   * @return Rule implementing the AND logic
   */
  public static DomainMatchRule allOf(List<DomainMatchRule> rules) {
    return(new Composite(false, rules));
  }

  /**
   * Implements a logical OR: any domains matching at least one of the supplied
   * rules are considered to be a match.
   *
   * @param rules Rules to apply and OR together
   * @return Rule implementing the OR logic
   */
  public static DomainMatchRule anyOf(DomainMatchRule... rules) {
    return(new Composite(true, rules));
  }

  /**
   * Implements a logical OR: any domains matching at least one of the supplied
   * rules are considered to be a match.
   *
   * @param rules Rules to apply and OR together
   * @return Rule implementing the OR logic
   */
  public static DomainMatchRule anyOf(List<DomainMatchRule> rules) {
    return(new Composite(true, rules));
  }

  /**
   * Inverts a rule, by applying a logical NOT to whatever it returns
   * from matches()
   *
   * @param rule Rules to invert
   * @return Rule implementing the NOT logic
   */
  public static DomainMatchRule not(DomainMatchRule rule) {
    return(new Not(rule));
  }

  /**
   * A concrete matcher, comparing domain names against the supplied
   * regular expression.
   *
   * @param pattern Pattern to compare against domain names
   * @return Rule implementing the pattern-match rule
   */
  public static DomainMatchRule is(Pattern pattern) {
    return(new Regex(pattern));
  }

  /**
   * A concrete matcher, comparing domain names against a "glob"-style
   * expression. So, "foo.com" as a glob will only match "foo.com" as a
   * domain. But "*.foo.com" as a glob will match "www.foo.com", "bar.foo.com",
   * "www.bar.foo.com", and so on.
   *
   * @param glob Glob to compare against domain names
   * @return Rule implementing the glob-match rule
   */
  public static DomainMatchRule is(String glob) {
    return(new Regex(glob));
  }

  /**
   * Accept any of the supplied globs, and reject anything else (see is(String)
   * for what "glob" means).
   *
   * @param globs Globs to compare against domain names
   * @return Rule implementing the whitelist
   */
  public static DomainMatchRule whitelist(String... globs) {
    List<DomainMatchRule> rules=new ArrayList<>();

    for (String glob : globs) {
      rules.add(is(glob));
    }

    return(anyOf(rules));
  }

  /**
   * Reject all of the supplied globs, and accept anything else (see is(String)
   * for what "glob" means).
   *
   * @param globs Globs to compare against domain names
   * @return Rule implementing the blacklist
   */
  public static DomainMatchRule blacklist(String... globs) {
    List<DomainMatchRule> rules=new ArrayList<>();

    for (String glob : globs) {
      rules.add(not(is(glob)));
    }

    return(allOf(rules));
  }

  private static class Composite extends DomainMatchRule {
    private final List<DomainMatchRule> rules;
    private final boolean isOr;

    Composite(boolean isOr, DomainMatchRule... rules) {
      this(isOr, Arrays.asList(rules));
    }

    Composite(boolean isOr, List<DomainMatchRule> rules) {
      this.isOr=isOr;
      this.rules=rules;
    }

    @Override
    public boolean matches(String host) {
      for (DomainMatchRule rule : rules) {
        boolean match=rule.matches(host);

        if (match && isOr) {
          return(true);
        }
        else if (!match && !isOr) {
          return(false);
        }
      }

      return(!isOr);
    }
  }

  private static class Not extends DomainMatchRule {
    private final DomainMatchRule rule;

    Not(DomainMatchRule rule) {
      this.rule=rule;
    }

    @Override
    public boolean matches(String host) {
      return(!rule.matches(host));
    }
  }

  private static class Regex extends DomainMatchRule {
    private final Pattern pattern;

    Regex(String glob) {
      this(Pattern.compile(glob.replaceAll("\\.", "\\\\.")
        .replaceAll("\\*", "\\.\\*")));
    }

    Regex(Pattern pattern) {
      this.pattern=pattern;
    }

    @Override
    public boolean matches(String host) {
      return(pattern.matcher(host).matches());
    }
  }
}
