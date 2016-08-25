# The Capsule Hash Trie Collections Library

This is a JDK7 compatible backport of the [usethesource/capsule](https://github.com/usethesource/capsule). Please refer to the main project for up-to-date documentation, issues, etcetera.


# Getting Started

Binary builds of capsule are deployed in the usethesource repository. In case you use Maven for dependency management, you have to add another repository location to your pom.xml file:

```
<repositories>
	<repository>
		<id>usethesource</id>
		<url>http://nexus.rascal-mpl.org/repository/maven-public/</url>
	</repository>
</repositories>
```

Furthermore, you have to declare capsule as a dependency. To obtain the latest version, insert the following snippet in your pom.xml file:

```
<dependency>
	<groupId>io.usethesource</groupId>
	<artifactId>capsule-jdk7</artifactId>
	<version>0.3.0-SNAPSHOT</version>
</dependency>
```