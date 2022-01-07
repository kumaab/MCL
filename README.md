# MCL

Missing Commit Locator.

## Installation

#### Prerequisites
- Java 8
- Maven 3.6.3

Verify installation of java using the below command.

```
$ java -version
java version "1.8.0_281"
Java(TM) SE Runtime Environment (build 1.8.0_281-b09)
Java HotSpot(TM) 64-Bit Server VM (build 25.281-b09, mixed mode)
```

## Usage

```
mvn clean package install dependency:copy-dependencies
```

Working Directory: MCL

#### Exhaustive Search
```
java -cp "target/classes:target/dependency/*" Main
```

#### Search for Custom Author
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar"
```
#### Search through the last N Months of Commits
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar" -m 6
```
