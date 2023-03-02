# Installation

## Engines

### d8
[`d8`](https://v8.dev/docs/d8) is V8's own developer shell. See
https://v8.dev/docs/source-code and https://v8.dev/docs/build-gn.
```shell
cd $HOME

#install depot_tools and update
mkdir -p bin
mkdir -p bin/v8
(cd bin/v8; git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git)
export PATH="$HOME/bin/v8/depot_tools:$PATH"

# checkout v8 directory
cd bin/v8
~/bin/v8/depot_tools/fetch v8
cd v8
git checkout main
git pull
~/bin/v8/depot_tools/gclient sync

# use gm to build v8 (and d8)
# python3 tools/dev/gm.py x64.release
gn gen out/d8 --args='v8_use_external_startup_data=false'
ninja -C out/d8 d8

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
```shell
# unzip the archive to your file system
tar -xzf <graalvm-archive>.tar.gz

# required for macOS only
sudo mv <graalvm> /Library/Java/JavaVirtualMachines

# set the JAVA_HOME environment variable to resolve to the installation directory
export GRAAL_HOME=<graalvm>
export JAVA_HOME=$GRAAL_HOME

# point the PATH environment variable to the GraalVM bin directory
export PATH=$GRAAL_HOME/bin:$PATH

# run GraalJS
#   -e : execute a string as script
js -e "print(42);"
```

### JavaScriptCore
The [JavaScriptCore](https://developer.apple.com/documentation/javascriptcore)
framework provides the ability to evaluate JavaScript programs from within
Swift, Objective-C, and C-based apps. See
https://github.com/WebKit/WebKit#building-webkit.
```shell
# install Xcode and turn on the developer mode
xcode-select --install
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer

# clone WebKit git repository
git clone https://github.com/WebKit/WebKit.git WebKit

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
```shell
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
Download from https://unpkg.com/@babel/standalone@7.19.1/babel.min.js,
and store it under src/main/resources/babel
```shell
# run babel for directory mode
src/main/resources/babel/babel-d indir outdir
```l

### SWC
[SWC](https://swc.rs/) (stands for Speedy Web Compiler) is a super-fast
TypeScript / JavaScript compiler written in Rust. See
https://github.com/swc-project/swc and https://swc.rs/docs/usage/cli.
```shell
# install SWC
npm i -g @swc/cli @swc/core

# run swc
swc in.js -o out.js
```

### terser
[`terser`](https://terser.org/) is a JavaScript mangler/compressor toolkit for ES6+.
See https://github.com/terser/terser.
```shell
# install terser
npm i -g terser

# run terser
#   -c          : compress
#   --ecma 2022 : for ES13 (ES2022)
terser -c --ecma 2022 in.js -o out.js
```

### JavaScript Obfuscator
[JavaScript Obfuscator](https://obfuscator.io/) is a free and efficient
obfuscator for JavaScript (including support of ES2022). See
https://www.npmjs.com/package/javascript-obfuscator.
```shell
# install JavaScript Obfuscator
npm i -g javascript-obfuscator

# run JavaScript Obfuscator
javascript-obfuscator in.js -o out.js
```
