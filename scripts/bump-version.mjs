/**
 * Increments the Maven patch version in pom.xml (x.y.z → x.y.z+1).
 * Usage: node scripts/bump-version.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const pomPath = join(root, 'pom.xml');
const versionPath = join(root, 'src', 'main', 'resources', 'app-version.properties');

let pom = readFileSync(pomPath, 'utf8');

// Only bump the project version, not the parent Spring Boot version.
const projectVersionRegex =
  /(<artifactId>pieml-backend<\/artifactId>\s*<version>)([^<]+)(<\/version>)/;

const match = pom.match(projectVersionRegex);
if (!match) {
  console.error('Impossible de trouver la version pieml-backend dans pom.xml');
  process.exit(1);
}

const current = match[2].trim();
const parts = current.split('.').map(n => Number.parseInt(n, 10) || 0);
while (parts.length < 3) parts.push(0);
parts[2] += 1;
const next = `${parts[0]}.${parts[1]}.${parts[2]}`;

pom = pom.replace(projectVersionRegex, `$1${next}$3`);
writeFileSync(pomPath, pom, 'utf8');
writeFileSync(versionPath, `app.version=${next}\n`, 'utf8');

process.stdout.write(`${next}\n`);
