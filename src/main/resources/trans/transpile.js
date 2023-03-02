Babel.transform(orig, {
  presets: ["env"],
  sourceType: "script",
  assumptions: {
    noDocumentAll: true
  }
}).code;
