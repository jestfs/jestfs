#!/usr/bin/env node

const babel = require("./babel@7.19.1.min.js");
const { transpileUsing } = require("./trans-d.js");

/** Do transpile using babel */
let transpile = (input) => {
  return babel.transform(input, {
    presets: ["env"],
    plugins: [
      ["transform-block-scoping", { tdz: true }],
    ],
    sourceType: "script",
    assumptions: {
      noDocumentAll: true,
      noNewArrows: false,
    }
  }).code;
}

transpileUsing(transpile);
