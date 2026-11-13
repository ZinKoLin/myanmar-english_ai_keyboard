// Reads SQL literals as data. Never evaluates downloaded JavaScript or executes SQL.
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../artifacts/mon-research');
function rows(file) {
  const data = fs.readFileSync(file, 'utf8');
  const start = data.indexOf(' VALUES');
  if (start < 0) throw new Error('Missing VALUES: ' + file);
  const output = [];
  let row = null, value = '', quoted = false;
  for (let i = start + 7; i < data.length; i++) {
    let c = data[i];
    if (quoted) {
      if (c === '\\') {
        const next = data[++i];
        value += ({n:'\n',r:'\r',t:'\t','0':'\0'})[next] ?? next;
      } else if (c === "'") {
        if (data[i+1] === "'") {value += "'"; i++;} else quoted = false;
      } else value += c;
    } else if (c === "'") quoted = true;
    else if (c === '(' && row === null) {row = []; value = '';}
    else if (row && (c === ',' || c === ')')) {
      row.push(value.trim()); value = '';
      if (c === ')') {output.push(row); row = null;}
    } else if (row) value += c;
    else if (c === ';') break;
  }
  if (quoted || row) throw new Error('Unclosed SQL literal: ' + file);
  return output;
}
const words = new Map();
for (let n = 1; n <= 3; n++) for (const row of rows(path.join(root,`word_${n}.js`))) {
  if (row.length !== 8) throw new Error('Invalid Word row');
  if (row[5] === 'mnw' && ['1','3'].includes(row[7])) words.set(row[0], row[2].normalize('NFC'));
}
const seen = new Set();
const result = [];
for (let n = 1; n <= 24; n++) for (const row of rows(path.join(root,`definition_${n}.js`))) {
  if (row.length !== 9) throw new Error('Invalid Definition row');
  // Nai Sac Lun's Mon–Burmese dictionary only. Exclude automated/community entries.
  const word = words.get(row[1]);
  if (!word || row[2] !== 'mya' || row[8] !== '1') continue;
  const definition = row[5].normalize('NFC').replace(/[\t\r\n]+/g, ' ').trim();
  if (!definition || definition === 'NULL' || definition.length > 400) continue;
  const line = word.replace(/[\t\r\n]+/g, ' ') + '\t' + definition;
  if (!seen.has(line)) {seen.add(line); result.push(line);}
}
fs.writeFileSync(path.resolve(__dirname,'../app/src/main/assets/mon_burmese_lexicon.tsv'), result.join('\n')+'\n');
fs.copyFileSync(path.join(root,'MonDictDB.LICENSE'),path.resolve(__dirname,'../app/src/main/assets/mon_burmese_lexicon.LICENSE'));
console.log(JSON.stringify({entries:result.length,headwords:words.size,commit:fs.readFileSync(path.join(root,'commit.txt'),'utf8').trim()}));
