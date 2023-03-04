# Artifact For "Feature-Sensitive Coverage for Conformance Testing of Programming Language Implementations"

This artifact is for PLDI 2023 paper: "Feature-Sensitive Coverage for
Conformance Testing of Programming Language Implementations" and includes
`jestfs`, a JavaScript conformance test generator using _feature-sensitive
coverage_. We developed `jestfs` by extending a JavaScript mechanized
specification extraction tool [`ESMeta`](https://github.com/es-meta/esmeta) and
a JavaScript conformance test generator
[`jest`](https://github.com/kaist-plrg/jest) with feature-sensitive coverage.


## Table of Contents

* [Getting Started Guide](#getting-started-guide)
  + [Using a Docker Container](#using-a-docker-container)
  + [Building from Source](#building-from-source)
* [Kick-the-tires Phase](#kick-the-tires-phase)
* [Basic Commands](#basic-commands)
* [Step-by-Step Instructions](#step-by-step-instructions)
  + [1) JavaScript Program Generation via Fuzzing (Optional)](#1-javascript-program-generation-via-fuzzing-optional)
  + [2) Conformance Test Generation and Bug Detection](#2-conformance-test-generation-and-bug-detection)
  + [3) Categorization of Detected Bugs](#3-categorization-of-detected-bugs)
  + [4) Collecting Coverage Information from Test262](#4-collecting-coverage-information-from-test262)
  + [5) Drawing Tables and Figures](#5-drawing-tables-and-figures)


## Getting Started Guide

We support two ways to use `jestfs`:
  1. [Using a Docker container](#using-a-docker-container)
  2. [Building from source](#building-from-source)

However, we STRONGLY RECOMMEND you use the Docker container because it requires
you to install not only the artifact but also JavaScript engines and
transpilers.


### Using a Docker Container

We provide a Docker container with `jestfs` and its dependencies. You can
install the docker by following the instruction in
[https://docs.docker.com/get-started/](https://docs.docker.com/get-started/) and
download our docker image with the following command:

> **WARNING**: The docker image is 16GB large. Thus, be patient when you
> download it, and please assign more than 16GB of memory for the docker engine.

```bash
docker pull jestfs/jestfs
docker run --name jestfs -it -m=16g --rm jestfs/jestfs
# user: guest, password: guest
```


### Building from Source

Our framework is developed in Scala, which works on JDK 8+, including
GraalVM. So before installation, [GraalVM Community Edition
22.2.0](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.2.0) and
[sbt](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html), an
interactive build tool for Scala.

Then, to set the execution environment, insert the following commands to
`~/.bashrc` (or `~/.zshrc`):
```bash
# for jestfs
export JESTFS_HOME="<path to jestfs>" # IMPORTANT!!!
export PATH="$JESTFS_HOME/bin:$PATH"  # for executables `jestfs` and etc.
source $JESTFS_HOME/.completion       # for auto-completion
```
The `<path to jestfs>` should be the absolute path of the `jestfs` repository.

Finally, please type the following command to 1) update the git submodules, 2)
generate binary file `bin/jestfs`, and 3) apply the `.completion` for
auto-completion.

```bash
cd jestfs                   # enter the jestfs repository
git submodule update --init # update the git submodules
sbt assembly                # generate binary file `bin/jestfs`
source .completion          # apply the `.completion` for auto-completion
```

For evaluation, you need to install JavaScript engines and transpilers. Please
refer to the [Installation Guide](INSTALL.md) for more details.


## Basic Commands

You can run this framework with the following command:
```bash
jestfs <command> <option>*
```
It supports the following commands:
- `help` - shows help messages.
- `fuzz` - generates JavaScript programs for fuzzing with the following options:
  - If `-fuzz:log-interval={number}` is given, turn on logging mode and set logging interval (default: 600 seconds).
  - If `-fuzz:debug={number}` is given, turn on deug mode with level (0: no-debug, 1: partial, 2: all)
  - If `-fuzz:timeout={number}` is given, set the time limit in seconds (default: 1 second).
  - If `-fuzz:trial={number}` is given, set the number of trials (default: INF).
  - If `-fuzz:duration={number}` is given, set the maximum duration for fuzzing (default: INF)
  - If `-fuzz:seed={number}` is given, set the specific seed for the random number generator (default: None).
  - If `-fuzz:k-fs={number}` is given, set the k-value for feature sensitivity (default: 0).
  - If `-fuzz:cp` is given, turn on the call-path mode (default: false) (meaningful if k-fs > 0).
- `conform-test` - performs conform test for a JavaScript engine or a transpiler with the following options:
  - If `-gen-test:debug` is given, turn on debug mode for test generation.
  - If `-gen-test:engines={string}` is given, list of engines to test, separated by comma.
  - If `-gen-test:transpilers={string}` is given, generate conformance tests for transpilers (names should be separated by comma).
  - If `-gen-test:use-cache` is given, use cached transpiled code from previous run.
  - If `-gen-test:only={string}` is given, only run files containing test names.
  - If `-gen-test:skip={string}` is given, skip files containing test names.
  - If `-conform-test:debug` is given, turn on debug mode for conformance testing.
  - If `-conform-test:msgdir={string}` is given, set the directory for log messages.
  - If `-conform-test:save-bugs` is given, save found bugs to database.
- `test262-test` - tests Test262 tests with harness files.
  - If `-test262-test:k-fs={number}` is given, set the k-value for feature sensitivity. (default: 0)
  - If `-test262-test:cp` is given, turn on the call-path mode (default: false) (meaningful if k-fs > 0).
- `categorize-bug` - categorizes detected bugs.
- `draw-figure` - draws various figures based on coverage.

and global options:
- If `-silent` is given, do not show final results.
- If `-error` is given, show error stack traces.
- If `-status` is given, exit with status.
- If `-time` is given, display the duration time.

If you want to see the detailed help messages and command-specific options,
please use the `help` command:
```bash
# show help messages for all commands
jestfs help

# show help messages for specific commands with more details
jestfs help <command>
```


## Kick-the-tires Phase

If you see the following message, `jestfs` is successfully installed:
```bash
jestfs
# Welcome to `jestfs` - JavaScript conformance test generator using feature-sensitive coverage.
# Please type `jestfs help` to see the help message.
```

Then, you generate JavaScript programs via fuzzing with 1-feature-sensitive
(1-FS) coverage in 60 seconds.

> **WARNING**: Note that it may take a few minutes longer than 60 seconds
> because it requires extracting the mechanized specification from ECMA-262 and
> constructing the initial program pool for fuzzing from it.

```bash
jestfs fuzz -fuzz:duration=60 -fuzz:k-fs=1
```


Then, the generated programs and detailed logs are stored in `logs/fuzz/recent`
directory, and which consists of the following files:

- `node-coverage.json` - the node coverage of generated programs in the specification.
- `branch-coverage.json` - the branch coverage of generated programs in the specification.
- `constructor.json` - options used in the program generation.
- `default-engine` - the default JavaScript engine used for syntax early error validation.
- `minimal` - the minimal set of programs that cover visited test requirements.
- `mutation-stat.tsv` - statistical information about mutation strategies.
- `seed` - the seed used for the random number generator.
- `selector-stat.tsv` - statistical information about selector strategies.
- `summary.tsv` - summary of the fuzzing during program generation.


## Step-by-Step Instructions

We provide step-by-step instructions to reproduce the results in our paper.


### 1) JavaScript Program Generation via Fuzzing (Optional)

First, you should generate JavaScript programs via fuzzing with five different
feature-sensitive coverage criteria in 50 hours.

Since it requires 250 hours (approx. 10 days) with a single machine, **we
RECOMMEND you use the generated programs we provided in
[`data.tar.gz`](https://doi.org/10.5281/zenodo.7697977)**:

```bash
# It is already included in the `data` directory when you use the docker image.
curl https://zenodo.org/record/7697977/files/data.tar.gz -o data.tar.gz
tar -xvzf data.tar.gz
```

However, if you want to generate JavaScript programs from scratch, please type
the following commands:
```bash
mkdir data

# with feature-insensitive (0-FS) coverage.
jestfs fuzz -fuzz:duration=180000
cp -r logs/fuzz/recent data/0

# with 1-feature-sensitive (1-FS) coverage.
jestfs fuzz -fuzz:duration=180000 -fuzz:k-fs=1
cp -r logs/fuzz/recent data/1

# with 2-feature-sensitive (2-FS) coverage.
jestfs fuzz -fuzz:duration=180000 -fuzz:k-fs=2
cp -r logs/fuzz/recent data/2

# with 1-feature-call-path-sensitive (1-FCPS) coverage.
jestfs fuzz -fuzz:duration=180000 -fuzz:k-fs=1 -fuzz:cp
cp -r logs/fuzz/recent data/1-cp

# with 2-feature-call-path-sensitive (2-FCPS) coverage.
jestfs fuzz -fuzz:duration=180000 -fuzz:k-fs=2 -fuzz:cp
cp -r logs/fuzz/recent data/2-cp
```


### 2) Conformance Test Generation and Bug Detection

To generate conformance tests and detect bugs in a JavaScript engine or a
transpiler, please type the following commands:

> **WARNING**: Note that it may take 5-10 hours.

```bash
mkdir result

# using programs generated with feature-insensitive (0-FS) coverage.
jestfs conform-test data/0/minimal data/0/minimal-assertion
mv logs/conform-test result/0

# using programs generated with 1-feature-sensitive (1-FS) coverage.
jestfs conform-test data/1/minimal data/1/minimal-assertion
mv logs/conform-test result/1

# using programs generated with 2-feature-sensitive (2-FS) coverage.
jestfs conform-test data/2/minimal data/2/minimal-assertion
mv logs/conform-test result/2

# using programs generated with 1-feature-call-path-sensitive (1-FCPS) coverage.
jestfs conform-test data/1-cp/minimal data/1-cp/minimal-assertion
mv logs/conform-test result/1-cp

# using programs generated with 2-feature-call-path-sensitive (2-FCPS) coverage.
jestfs conform-test data/2-cp/minimal data/2-cp/minimal-assertion
mv logs/conform-test result/2-cp
```

Then, **please compare the `result` directory with the expected result we
provided in [`out.tar.gz`](https://doi.org/10.5281/zenodo.7697977)**:
```bash
# It is already included in the `out` directory when you use the docker image.
curl https://zenodo.org/record/7697977/files/out.tar.gz -o out.tar.gz
tar -xvzf out.tar.gz

# compare the result and the expected result.
diff -r result out
```

### 3) Categorization of Detected Bugs

Please categorize the detected conformance bugs as follows:
```bash
mkdir categorized

# for feature-insensitive (0-FS) coverage.
jestfs categorize-bug data/0/minimal result/0/fails.json
mv logs/categorize/test-summary.tsv categorized/0.tsv

# for 1-feature-sensitive (1-FS) coverage.
jestfs categorize-bug data/1/minimal result/1/fails.json
mv logs/categorize/test-summary.tsv categorized/1.tsv

# for 2-feature-sensitive (2-FS) coverage.
jestfs categorize-bug data/2/minimal result/2/fails.json
mv logs/categorize/test-summary.tsv categorized/2.tsv

# for 1-feature-call-path-sensitive (1-FCPS) coverage.
jestfs categorize-bug data/1-cp/minimal result/1-cp/fails.json
mv logs/categorize/test-summary.tsv categorized/1-cp.tsv

# for 2-feature-call-path-sensitive (2-FCPS) coverage.
jestfs categorize-bug data/2-cp/minimal result/2-cp/fails.json
mv logs/categorize/test-summary.tsv categorized/2-cp.tsv
```


### 4) Collecting Coverage Information from Test262

To extract data from Test262 tests, please run the following command:

> **WARNING**: Note that it may take 5-10 hours.

```bash
jestfs test262-test -test262-test:debug -test262-test:k-fs=2 -test262-test:cp
mv logs/test262/* test262-result
```

Or, you could download it as follows:
```bash
# It is already included in the `test262-result` directory when you use the docker image.
curl https://zenodo.org/record/7697977/files/test262-result.tar.gz -o test262-result.tar.gz
tar -xvzf test262-result.tar.gz
```


### 5) Drawing Tables and Figures

Please run the `draw-figure` command as follows:
```bash
jestfs draw-figure test262-result data
```

Then, open the [`Tables-Figures.xlsx`](./Tables-Figures.xlsx) file and fill in
the cells colored in blue with the corresponding files as follows:

|Tab|Filename|
|:-|:-|
|0-bug|[`categorized/0.tsv`](./categorized/0.tsv)|
|1-bug|[`categorized/1.tsv`](./categorized/1.tsv)|
|2-bug|[`categorized/2.tsv`](./categorized/2.tsv)|
|1-cp-bug|[`categorized/1-cp.tsv`](./categorized/1-cp.tsv)|
|2-cp-bug|[`categorized/2-cp.tsv`](./categorized/2-cp.tsv)|
|0-summary|[`data/0/summary.tsv`](./data/0/summary.tsv)|
|1-summary|[`data/1/summary.tsv`](./data/1/summary.tsv)|
|2-summary|[`data/2/summary.tsv`](./data/2/summary.tsv)|
|1-cp-summary|[`data/1-cp/summary.tsv`](./data/1-cp/summary.tsv)|
|2-cp-summary|[`data/2-cp/summary.tsv`](./data/2-cp/summary.tsv)|
|Figure 9 (a)|[`logs/draw-figure/1-graph.tsv`](./logs/draw-figure/1-graph.tsv)|
|Figure 9 (b)|[`logs/draw-figure/2-graph`](./logs/draw-figure/2-graph)|
|Figure 9 (c)|[`logs/draw-figure/1-cp-graph.tsv`](./logs/draw-figure/1-cp-graph.tsv)|
|Figure 9 (d)|[`logs/draw-figure/2-cp-graph.tsv`](./logs/draw-figure/2-cp-graph.tsv)|
|Figure 9 (d)|[`logs/draw-figure/2-cp-graph.tsv`](./logs/draw-figure/2-cp-graph.tsv)|
|Figure 10 (a)|[`logs/draw-figure/test262-cmp.csv`](./logs/draw-figure/test262-cmp.csv)|

and compare the following tables and figures in the paper:

- **Table 1. Detected conformance bugs in JavaScript engines and transpilers**
- **Table 2. Comparison of synthesized conformance tests guided by five graph coverage criteria**
- **Fig. 9. The histogram of numbers of $k$-FS or $k$-FCPS TRs per less sensitive $k$-FS or $k$-FCPS TR**
- **Fig. 10. Covered $k$-FS-TRs and $k$-FCPS-TRs for synthesized tests via ${\sf JEST}_{\sf fs}$ and Test262**
