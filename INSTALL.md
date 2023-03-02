# Installation Guide

For evaluation, you need to install the following JavaScript engines and
transpilers:

|Kind|Name|Version|Release|
|:-|:-|:-|:-|
|Engine     |V8            |v10.8.121  |2022.10.06
|Engine     |JSC           |v615.1.10  |2022.10.26
|Engine     |GraalJS       |v22.2.0    |2022.07.26
|Engine     |SpiderMonkey  |v107.0b4   |2022.10.24
|Transpiler |Babel       |v7.19.1  |2022.09.15
|Transpiler |SWC         |v1.3.10  |2022.10.21
|Transpiler |Terser      |v5.15.1  |2022.10.05
|Transpiler |Obfuscator  |v4.0.0   |2022.02.15

## Engines

### d8
[`d8`](https://v8.dev/docs/d8) is V8's own developer shell. See
https://v8.dev/docs/source-code and https://v8.dev/docs/build-gn.

```bash
mkdir v8
cd v8

#install depot_tools and update
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
export PATH="<path/to/depot_tools>:$PATH"

# checkout v8 directory
fetch v8
cd v8
git checkout 10.8.121
gclient sync

# use gm to build v8 (and d8)
gn gen out/d8 --args='v8_use_external_startup_data=false'
ninja -C out/d8 d8

export PATH="<path/to/out/d8>:$PATH"

# run d8
#   --ignore-unhandled-promises : option to ignore unhandled rejected promises
#                                 (Same option --unhandled-rejections=none for node)
#   -e                          : execute a string as script
d8 --ignore-unhandled-promises -e "print(42);"
```

### GraalJS
[GraalJS](https://github.com/oracle/graaljs) is a high performance
implementation of the JavaScript programming language. Built on the GraalVM by
Oracle Labs. Go to
[Downloads](https://github.com/graalvm/graalvm-ce-builds/releases), select the
Java version and download GraalVM. See https://www.graalvm.org/java/quickstart/.
The version used for the paper can be downloaded
[here](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.2.0).
```bash
# download and unzip the archive to your file system
wget <graalvm-archive>.tar.gz
tar -xzf <graalvm-archive>.tar.gz

# required for macOS only
sudo mv <graalvm> /Library/Java/JavaVirtualMachines

# set the JAVA_HOME environment variable to resolve to the installation directory
export GRAAL_HOME=<graalvm>
export JAVA_HOME=$GRAAL_HOME

# point the PATH environment variable to the GraalVM bin directory
export PATH=$GRAAL_HOME/bin:$PATH

# install js on jvm
gu --jvm install js

# run GraalJS
#   -e : execute a string as script
js -e "print(42);"
```

### JavaScriptCore
(NOTE: Trying to install JavaScriptCore on Linux requires heavy dependency for build system, and is highly likely to fail)

The [JavaScriptCore](https://developer.apple.com/documentation/javascriptcore)
framework provides the ability to evaluate JavaScript programs from within
Swift, Objective-C, and C-based apps. See
https://github.com/WebKit/WebKit#building-webkit.
```bash
# install Xcode and turn on the developer mode
xcode-select --install
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer

# clone WebKit git repository
git clone https://github.com/WebKit/WebKit.git WebKit -b safari-7615.1.10-branch --single-branch --depth 1

# build WebKit
cd WebKit
./Tools/Scripts/build-jsc --release

# set the DYLD_FRAMEWORK_PATH environment variable
export WEBKIT_HOME=<webkit>
export DYLD_FRAMEWORK_PATH=$WEBKIT_HOME/WebKitBuild/Release

# point the PATH environment variable to the jsc
export PATH=$DYLD_FRAMEWORK_PATH:$PATH

# run JavaScriptCore
#   -e : execute a string as script
jsc -e "print(42);"
```

### SpiderMonkey
[SpiderMonkey](https://spidermonkey.dev/) is Mozilla’s JavaScript and
WebAssembly Engine, used in Firefox, Servo and various other projects.
Download the `jsshell` zip file for the target versions in
https://ftp.mozilla.org/pub/firefox/releases/ and unzip it.
The version used for the paper can be downloaded [here](https://ftp.mozilla.org/pub/firefox/releases/107.0b4/jsshell/).
```bash
# download and unzip jsshell.zip
wget <jsshell>.zip
unzip <jsshell>.zip -d jsshell

# rename `js` as `sm` to prevent the conflict with GraalJS
export PATH=<jsshell>:$PATH
mv <jsshell>/js <jsshell>/sm

# run Spider Monkey
#   -e 'ignoreUnhandledRejections()' : Register unhandled-rejections ignoring mode
#   -e : execute a string as script
# 
sm -e 'ignoreUnhandledRejections()' -e "print(42);"
```

## Transpilers

### Babel
[Babel](https://babeljs.io/) is a JavaScript compiler to use next generation
JavaScript. See https://babeljs.io/docs/en/babel-cli.
The standalone, minified babel@7.19.1 is already stored in `src/main/resources/trans`, so there is no need to manually download it.

### SWC
[SWC](https://swc.rs/) (stands for Speedy Web Compiler) is a super-fast
TypeScript / JavaScript compiler written in Rust. See
https://github.com/swc-project/swc and https://swc.rs/docs/usage/cli.
```bash
# install SWC
npm i -g @swc/cli @swc/core@1.3.10

# run swc
swc in.js -o out.js
```

### terser
[`terser`](https://terser.org/) is a JavaScript mangler/compressor toolkit for ES6+.
See https://github.com/terser/terser.
```bash
# install terser
npm i -g terser@5.15.1

# run terser
#   -c          : compress
#   --ecma 2022 : for ES13 (ES2022)
terser -c --ecma 2022 in.js -o out.js
```

### JavaScript Obfuscator
[JavaScript Obfuscator](https://obfuscator.io/) is a free and efficient
obfuscator for JavaScript (including support of ES2022). See
https://www.npmjs.com/package/javascript-obfuscator.
```bash
# install JavaScript Obfuscator
npm i -g javascript-obfuscator@4.0.0

# run JavaScript Obfuscator
javascript-obfuscator in.js -o out.js
```
