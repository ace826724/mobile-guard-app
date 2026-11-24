
import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";

const source = "android-icons/play-store-icon-512.png";
const androidRes = "android/app/src/main/res";

const densities = {
  "mipmap-mdpi": 48,
  "mipmap-hdpi": 72,
  "mipmap-xhdpi": 96,
  "mipmap-xxhdpi": 144,
  "mipmap-xxxhdpi": 192
};

if (!fs.existsSync(source)) {
  throw new Error(`Ikon dosyasi bulunamadi: ${source}`);
}

for (const [folder, size] of Object.entries(densities)) {
  const outputDir = path.join(androidRes, folder);
  fs.mkdirSync(outputDir, { recursive: true });

  for (const name of ["ic_launcher", "ic_launcher_round"]) {
    await sharp(source)
      .resize(size, size, {
        fit: "contain",
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .png()
      .toFile(path.join(outputDir, `${name}.png`));
  }

  console.log(`${folder}: ${size}x${size} ikonlar olusturuldu.`);
}

console.log("MGA ikonlari basariyla olusturuldu.");
