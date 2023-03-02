#!/usr/bin/env node

const { obfuscate } = require("/usr/local/lib/node_modules/javascript-obfuscator");
const { transpileUsing } = require("./trans-d.js");

/** Do offuscate */
let transpile = (input) => {
  return obfuscate(input, {
    seed: 1
  }).getObfuscatedCode();
}

transpileUsing(transpile);
