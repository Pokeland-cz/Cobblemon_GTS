# GTS website

Static Next.js site bundled into the mod jar (under `gts_web/`) and served by the mod's built-in web server.
The JSON API lives in the mod: `common/src/main/java/org/pokesplash/gts/web/`.

- `./gradlew build` runs `npm ci` and `npm run build` here and bundles `out/`. Needs Node.js on PATH.
  Without Node (or with `-PskipWeb`) the website is not rebuilt.
- Frontend development: run a Minecraft server with the website enabled, then `npm run dev`
  and proxy `/api` to it, or copy `out/` into `config/gts/web/site/` on the server to test a build.

Files in `config/gts/web/site/` on the server override the bundled ones.
