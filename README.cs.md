
# Cobblemon GTS

![GitHub](https://img.shields.io/github/license/PokeSplashDEV/GTS)

[English](README.md) | **Česky**

Global Trading System (GTS) pro [Cobblemon](https://cobblemon.com/): hráči si mezi sebou prodávají Pokémony a předměty.

## Funkce

- Nabídky Pokémonů a předmětů
- Nastavitelné minimální ceny podle IV, skryté schopnosti a konkrétních předmětů
- Seznam zakázaných předmětů
- Moderace
- Volitelný web pro procházení, nákup a rušení nabídek Pokémonů a nástěnka WTB/WTS (koupím/prodám)

## Web

GTS umí spustit web přímo z Minecraft serveru. Zapíná se v `config/gts/web.json`:

| Volba | Výchozí | Popis |
| --- | --- | --- |
| `enabled` | `false` | Spustí webový server. |
| `bindAddress` / `port` | `0.0.0.0` / `8080` | Adresa a port, na kterých web poslouchá. |
| `publicUrl` | `""` | Adresa, kterou hráči otevírají. Použije se v odkazu z `/gts web`. Když je prázdná, použije se `http://localhost:<port>`. |
| `secureCookies` | `false` | Nastav na `true`, když web běží přes HTTPS. |
| `trustProxyHeaders` | `false` | Pro omezení počtu požadavků použije IP adresu z hlavičky `X-Forwarded-For`. Zapínej jen za reverse proxy. |
| `threads` | `4` | Počet vláken pro obsluhu webu. |
| `sessionHours` | `168` | Přihlášení vyprší po tolika hodinách, kdy hráč web nepoužije. |
| `loginCodeMinutes` | `5` | Jak dlouho platí kód z `/gts web`. |
| `discordClientId` / `discordClientSecret` | `""` | Discord aplikace pro přihlášení přes Discord. Prázdné = vypnuto. |

Hráči se přihlašují příkazem `/gts web` (oprávnění `gts.user.web`), který jim dá jednorázový odkaz a kód.
Každá návštěva webu přihlášení prodlouží, takže vyprší až po `sessionHours` bez používání webu.

Pro nákup přes web musí být hráč připojený ve hře. Nabídky zrušené na webu se přesunou mezi prošlé nabídky
prodávajícího. Vyzvednout si je může na webu (musí být ve hře) nebo příkazem `/gts expired`.

Inzeráty na nástěnce zůstávají, dokud je autor nesmaže. Nástěnka drží nejvýš 200 inzerátů, při dalším se smaže nejstarší.

`/gts reload` web restartuje a načte znovu `web.json`.
Pro HTTPS dej před server reverse proxy (nginx, Caddy) a nastav `secureCookies` a `trustProxyHeaders` na `true`.

### Přihlášení přes Discord

Hráč se jednou přihlásí kódem z `/gts web`, propojí si Discord účet a pak se přihlašuje přes Discord.
Zapnutí:

1. Založ aplikaci v [Discord Developer Portalu](https://discord.com/developers/applications).
2. V sekci OAuth2 přidej do Redirects adresu `<publicUrl>/api/auth/discord/callback`, například
   `https://gts.example.com/api/auth/discord/callback`. Musí přesně odpovídat `publicUrl`.
3. Zkopíruj Client ID a Client Secret do `discordClientId` a `discordClientSecret` a dej `/gts reload`.

Web používá jen oprávnění `identify` (Discord ID a jméno). Propojené účty jsou uložené v
`config/gts/web/data/discord_links.json`.

### Data a vlastní úpravy

Data nástěnky jsou v `config/gts/web/data/`.
Soubory v `config/gts/web/site/` přepíšou web přibalený v jaru, takže vzhled jde upravit bez nového buildu.

## Instalace

Mód běží na [Fabricu](https://fabricmc.net/) i [NeoForge](https://neoforged.net/). Potřebuje:

- Minecraft 1.21.1
- [Cobblemon](https://modrinth.com/mod/cobblemon)
- [Impactor](https://modrinth.com/mod/impactor)
- [GooeyLibs](https://github.com/NickImpact/GooeyLibs)

Pro build s webem je potřeba Node.js (`npm`). Bez něj, nebo s `./gradlew build -PskipWeb`, se web znovu nesestaví.

## Snímky obrazovky

![GTS](https://i.imgur.com/laMTPyy.png)
![Nabídka Pokémona](https://i.imgur.com/1znf9Fk.png)
![Vlastní nabídka](https://i.imgur.com/sgl9jsN.png)
![Historie](https://i.imgur.com/m1ne2Li.png)
![Nákup](https://i.imgur.com/U5xBVto.png)
![Moderace](https://i.imgur.com/JaStGsM.png)

## Dokumentace

[Viz wiki (anglicky).](https://github.com/PokeSplashDEV/GTS/wiki)

## Licence

[MIT](https://choosealicense.com/licenses/mit/)

## Autoři

- [@bencrow11](https://www.github.com/bencrow11)
- [@poqdavid](https://www.github.com/poqdavid)
