import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const toolDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(toolDirectory, "..", "..", "..");
const controllers = path.join(root, "src", "main", "java", "veterinaria", "vargasvet", "controller");
const output = path.resolve(toolDirectory, "..", "config", "all-endpoints-inventory.csv");
const mappingPattern = /@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)(?:\s*\((.*?)\))?/gs;

function annotationPath(argumentsText) {
  if (!argumentsText) return "";
  const match = argumentsText.match(/(?:value\s*=\s*)?"([^"]*)"/s);
  return match ? match[1] : "";
}

function joinPath(base, child) {
  const result = `/${base.replace(/^\/+|\/+$/g, "")}/${child.replace(/^\/+|\/+$/g, "")}`
    .replace(/\/{2,}/g, "/")
    .replace(/\/$/, "");
  return result || "/";
}

const rows = [];
for (const fileName of fs.readdirSync(controllers).filter((name) => name.endsWith(".java")).sort()) {
  const text = fs.readFileSync(path.join(controllers, fileName), "utf8");
  const classMarker = text.indexOf("public class ");
  const prefix = classMarker >= 0 ? text.slice(0, classMarker) : text;
  const baseMatch = prefix.match(/@RequestMapping\s*\(\s*"([^"]*)"/s);
  const base = baseMatch ? baseMatch[1] : "";
  for (const match of text.matchAll(mappingPattern)) {
    const tail = text.slice(match.index + match[0].length, match.index + match[0].length + 1800);
    const handler = tail.match(/\bpublic\s+(?:<[^>]+>\s+)?[\w.<>, ?\[\]]+\s+(\w+)\s*\(/s);
    if (!handler) throw new Error(`No se identificó el handler después de ${match[0]} en ${fileName}`);
    rows.push({
      controller: fileName.replace(/\.java$/, ""),
      handler: handler[1],
      method: match[1].replace(/Mapping$/, "").toUpperCase(),
      path: joinPath(base, annotationPath(match[2])),
    });
  }
}

rows.sort((a, b) => `${a.path}|${a.method}|${a.controller}|${a.handler}`.localeCompare(
  `${b.path}|${b.method}|${b.controller}|${b.handler}`,
));

const escapeCsv = (value) => `"${String(value).replaceAll('"', '""')}"`;
const lines = ["id,controller,handler,method,path"];
rows.forEach((row, index) => {
  const id = `EP-${String(index + 1).padStart(3, "0")}`;
  lines.push([id, row.controller, row.handler, row.method, row.path].map(escapeCsv).join(","));
});
fs.writeFileSync(output, `\ufeff${lines.join("\r\n")}\r\n`, "utf8");
console.log(`Inventario generado: ${rows.length} endpoints -> ${output}`);
