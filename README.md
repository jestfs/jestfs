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


## Getting Started Guide

We support two ways to use `jestfs`:
  1. [Using a Docker container](#using-a-docker-container)
  2. [Building from source](#building-from-source)

However, we STRONGLY RECOMMEND you using the Docker container because it
requires to install not only artifacts but also target JavaScript engines and
transpilers.


### Using a Docker Container

We provide a Docker container with `jestfs` and its dependencies. You can
install the docker by following the instruction in
[https://docs.docker.com/get-started/](https://docs.docker.com/get-started/) and
downlaod our docker image with the following command:
```bash
docker pull jestfs/jestfs
docker run --name jestfs -it -m=16g --rm jestfs/jestfs
# user: guest, password: guest
```
> **WARNING**: The docker image is 15GB large thus be patient when you download
> it and please assign more than 16GB memory for the docker engine.


### Building from Source

Our framework is developed in Scala, which works on JDK 8+, including
GraalVM. So before installation, [GraalVM Community Edition
22.2.0](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.2.0) and
[sbt](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html), an
interactive build tool for Scala.

Then, to set the exection environment, insert the following commands to
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
  - If `-fuzz:cp` is given, turn on the call-path mode (default: false) (meaningful if `k-fs` is greater than 0).
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

Then, you can generate files generate JavaScript programs via fuzzing with
1-feature-sensitive (1-FS) coverage in 60 seconds.

```bash
jestfs fuzz -fuzz:duration=60 -fuzz:k-fs=1
```

> **WARNING**: Note that it may take a few minutes longer than 60 seconds
> because it requires to extract the mechanized specification from ECMA-262 and
> construct initial program pool for fuzzing from it.

Then, the generated programs and detailed logs are stored in `logs/fuzz/recent`
directory, and it consists of the following files:

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
RECOMMEND you to use the generated programs we provided in
[`data.tar.gz`](https://doi.org/10.5281/zenodo.7694243)**:
```bash
# It is already included in the `data` directory when you use the docker image.
curl https://zenodo.org/record/7694243/files/data.tar.gz -o data.tar.gz
tar -xvzf data.tar.gz
```

However, if you want to generate JavaScript programs from the scratch, please
type the following commands:
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
```bash
mkdir result

# using programs generated with feature-insensitive (0-FS) coverage.
jestfs conform-test data/0/minimal data/0/minimal-assertion
mv logs/conform-test result/0

# using programs generated with 1-feature-sensitive (1-FS) coverage.
jestfs conform-test data/1/minimal data/1/minimal-assertion
mv logs/conform-test result/1

# using programs generated with 1-feature-sensitive (1-FS) coverage.
jestfs conform-test data/2/minimal data/2/minimal-assertion
mv logs/conform-test result/2

# using programs generated with 1-feature-call-path-sensitive (1-FS) coverage.
jestfs conform-test data/1-cp/minimal data/1-cp/minimal-assertion
mv logs/conform-test result/1-cp

# using programs generated with 1-feature-call-path-sensitive (1-FS) coverage.
jestfs conform-test data/2-cp/minimal data/2-cp/minimal-assertion
mv logs/conform-test result/2-cp
```
> **WARNING**: Note that it may take 5-10 hours.

Then, **please compare the `result` directory with the expected result we
provided in [`out.tar.gz`](https://doi.org/10.5281/zenodo.7694243)**:
```bash
# It is already included in the `out` directory when you use the docker image.
curl https://zenodo.org/record/7694243/files/out.tar.gz -o out.tar.gz
tar -xvzf out.tar.gz

# compare the result and the expected result.
diff -r result out
```

### 3) Tables and Figures

Please open the `Tables & Figures.xlsx` file and compare the following tables
and figures in the paper:

- Table 1. Detected conformance bugs in JavaScript engines and transpilers
- Table 2. Comparison of synthesized conformance tests guided by five graph coverage criteria
- Fig. 9. The histogram of numbers of $k$-FS or $k$-FCPS TRs per less sensitive $k$-FS or $k$-FCPS TR
- Fig. 10. Covered $k$-FS-TRs and $k$-FCPS-TRs for synthesized tests via ${\sf JEST}_{\sf fs}$ and Test262
