---
title: "Intro"
bg: purple
color: white
style: center
---

# JDeps Wall Of Shame

## Who depends on JDK-internal API?

Java 9 is [looming on the horizon](http://blog.codefx.org/java/dev/delay-of-java-9-release/).
It comes with [Project Jigsaw](http://openjdk.java.net/projects/jigsaw/), which brings modularity to Java—and trouble!

Various changes and features might [break existing code](http://blog.codefx.org/java/dev/how-java-9-and-project-jigsaw-may-break-your-code/),
chief among them [strong encapsulation](openjdk.java.net/projects/jigsaw/spec/sotms/),
which will make JDK-internal APIs unavailable.
Even if your code does not use these APIs, your critical dependencies might.

Problematic dependencies can be identified with [_jdeps_](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jdeps.html), a command line tool that ships with Java 8+.
This site is the result of viciously downloading artifacts from Maven Central and running _jdeps_ on them.

## How To Use This Site

First of all, yes, the site is huge! Just be patient...

To find out whether an artifact of your interest is clean or not, use the browser search.
Simply look for the common pattern `groupId:artifactId:version`.

Besides this introduction the site is divided into three parts:

**Direct Dependencies**

For artifacts in this section _jdeps_ reported dependencies on JDK-interal APIs.
But this does not mean that it will immediately break when run on Java 9!
If the specific code goes untouched (maybe because it's a library and that part is just not called), everything will continue to work.

You might want to have a closer look or contact the maintainers, though.

**Indirect Dependencies**

Artifacts in this section do not access problematic APIs themselves.
But they depend (directly or transitively) on artifacts that do.
Like before, this does not mean that they will definitely stop working and you should investigate.

**No Dependencies**

According to _jdeps_ neither the artifact itself nor the dependencies reported by Maven Central use JDK-internal APIs.
Note that this does not mean that everything will be peaches and rainbows.
There are more ways for code to break with Jigsaw than internal APIs and the results shown here do not pertain to them.

## Thanks!

Shout out to [Boris Terzic]("http://www.aggregat4.net/) and Johannes Kissel:
Thank you so much for your ideas and contributions!
