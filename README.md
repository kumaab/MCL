# MCL

Missing Commit Locator

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

#### Exhaustive Search - Default Usage
```
java -cp "target/classes:target/dependency/*" Main
```
Returns search results for all commits by all developers in the last 12 months sorted by commit date(in first repo) in descending order.

#### Search for Custom Author
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar"
```
#### Search for Custom Committer
```
java -cp "target/classes:target/dependency/*" Main -c "Abhishek Kumar"
```
#### Search through the last 10 Days of Commits
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar" --days 10
```
#### Search through the last 6 Months of Commits
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar" --months 6
```
#### Search and Sort Results by Status
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar" --sort
```
#### Show only missing Commits
```
java -cp "target/classes:target/dependency/*" Main -a "Abhishek Kumar" --missing
```