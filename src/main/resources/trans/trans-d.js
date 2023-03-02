const fs = require("fs");
const path = require("path");

/** warn user about the usage */
let warnUsage = () => {
  console.log(`Usage: ${process.argv[1]} inputDir outputDir`);
  process.exit(1);
}

/** Transpile directory using a given transpile fucntion */
let transpileUsing = (transpile) => {
  const inputDir = process.argv[2];
  const outputDir = process.argv[3]

  if (inputDir === undefined || outputDir === undefined) {
    warnUsage();
  }

  fs.rmSync(outputDir, {recursive: true, force: true});
  fs.mkdirSync(outputDir, {recursive: true});

  for(let filename of fs.readdirSync(inputDir)) {
    let input = fs.readFileSync(path.join(inputDir, filename), "utf8")
    let output;
    try {
      output = transpile(input);
    } catch (e) {
      output = `throw "TRANSPILE_FAILURE";\n/* ${e} */\n`;
    }
    fs.writeFileSync(path.join(outputDir, filename), output);
  }
}

/** Transpile directory asynchronously using a given transpile fucntion */
let transpileAsyncUsing = async (transpileAsync) => {
  const inputDir = process.argv[2];
  const outputDir = process.argv[3]

  if (inputDir === undefined || outputDir === undefined) {
    warnUsage();
  }

  fs.rmSync(outputDir, {recursive: true, force: true});
  fs.mkdirSync(outputDir, {recursive: true});

  for(let filename of fs.readdirSync(inputDir)) {
    let input = fs.readFileSync(path.join(inputDir, filename), "utf8")
    let output;
    try {
      output = await transpileAsync(input);
    } catch (e) {
      output = `throw "TRANSPILE_FAILURE";\n/* ${e} */\n`;
    }
    fs.writeFileSync(path.join(outputDir, filename), output);
  }
}

module.exports = { transpileUsing, transpileAsyncUsing };
