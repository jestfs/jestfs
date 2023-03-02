#!/usr/bin/env node

const swc = require("/usr/local/lib/node_modules/@swc/core");
const { transpileUsing } = require("./trans-d.js");

/** Do transpile using swc */
let transpile = (input) => {
  return swc.transformSync(input, {
    isModule: false,
  }).code;
}

transpileUsing(transpile);
