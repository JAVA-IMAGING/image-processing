#   image-processing

The goal is to process FITS images and apply filters and whatever you're 
supposed to do with astronomy pictures.

## Table of Contents
1. [Overview](#overview)
2. [Packages used](#packages-used)
3. [Project Setup](#project-setup)

##  Overview

Trying to figure out the previous code

##  Packages used

Look it up in the pom.xml file under dependencies

##  Project Setup

### Prerequisites

- Java 11: [[download link]](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
- Maven (_optional_): [[download link]](https://maven.apache.org/download.cgi)

### Installation

1. Clone the repository

    `git clone https://github.com/JAVA-IMAGING/image-processing.git`

2. Navigate to Maven project directory

    `cd ./image-processing/java-imaging`

3. Validate and compile the Maven project
    - `mvn validate` and `mvn compile`, a _test_ directory will then be created with class files. 
    - If Maven is not installed, replace `mvn` with either `./mvnw` or `.\mvnw.cmd`  if system is Windows or Linux/Mac respectively to make use of the wrapper

4. Package the compiled code and run
    `mvn package` will compile once more before packaging into _.jar_ file. Execute _.jar_ file found in _test_ directory `java -cp target/<jar-file-name>.jar com.package.of.main.file`
