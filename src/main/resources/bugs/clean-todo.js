const fs = require('fs');
const path = require('path');

const dirs = fs.readdirSync(".");
for(dir of dirs) if(fs.lstatSync(dir).isDirectory()) {
  console.log(dir);

  let knowns = fs.readdirSync(dir)
    .filter(name => name.endsWith(".js"))
    .map(js => fs.readFileSync(path.join(dir, js), "utf8").trim().split("\n").map(l => l.trim()))

  let todoDir = path.join(dir, "TODO");
  try {
    const todos = fs.readdirSync(todoDir);
    for(todo of todos) {
      let target = fs.readFileSync(path.join(todoDir, todo), "utf8").trim().split("\n")[1].trim();
      for(lines of knowns) {
        if(lines.includes(target)) {
          console.log(todoDir, todo, target);
          if(process.argv[2] == "-d") {
            fs.rmSync(path.join(todoDir, todo));
            console.log("deleted");
          }
        }
      }
    }
  } catch(e) {
    console.log("no todo");
  }
}
