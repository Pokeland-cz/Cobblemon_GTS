
# Cobblemon GTS

![GitHub](https://img.shields.io/github/license/PokeSplashDEV/GTS)

**English** | [Česky](README.cs.md)

A Global Trading System mod for [Cobblemon](https://cobblemon.com/) that allows selling of Pokemon and Items.
## Features

- Pokemon and Item listings
- Configurable minimum prices for IVs, Hidden Ability, Specified Items.
- Item Blacklist
- Moderation
- Optional website for browsing, buying and cancelling Pokemon listings and a WTB/WTS trade board

## Website

GTS can serve a website from the Minecraft server itself. Enable it in `config/gts/web.json`:

| Option | Default | Description |
| --- | --- | --- |
| `enabled` | `false` | Starts the web server. |
| `bindAddress` / `port` | `0.0.0.0` / `8080` | Where the web server listens. |
| `publicUrl` | `""` | URL players open, used in the `/gts web` link. Defaults to `http://localhost:<port>`. |
| `secureCookies` | `false` | Set to `true` when the site is served over HTTPS. |
| `trustProxyHeaders` | `false` | Use `X-Forwarded-For` for rate limiting. Only enable behind a reverse proxy. |
| `threads` | `4` | Worker threads for web requests. |
| `sessionHours` | `168` | A web login expires after this many hours without visiting the website. |
| `loginCodeMinutes` | `5` | How long a `/gts web` code stays valid. |
| `discordClientId` / `discordClientSecret` | `""` | Discord application for "Log in with Discord". Empty = disabled. |

Players log in with `/gts web` (permission `gts.user.web`), which gives a one-time link and code.
Every visit renews the login, so it only expires after `sessionHours` without using the website.
Buying from the website requires the player to be online in game. Listings cancelled on the website move to
the seller's expired listings, which can be collected on the website (player online) or with `/gts expired`.
Trade board posts stay until their author deletes them. The board keeps the 200 newest posts.
`/gts reload` restarts the website.
For HTTPS, put a reverse proxy (nginx, Caddy) in front and set `secureCookies` and `trustProxyHeaders`.

### Log in with Discord

After logging in with a code once, players can link their Discord account and later log in with it.
To enable it:

1. Create an application in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Under OAuth2, add the redirect `<publicUrl>/api/auth/discord/callback`, e.g.
   `https://gts.example.com/api/auth/discord/callback`. It must match `publicUrl` exactly.
3. Copy the Client ID and Client Secret into `discordClientId` and `discordClientSecret`, then `/gts reload`.

Only the `identify` scope is used. Linked accounts are stored in `config/gts/web/data/discord_links.json`.

Data of the trade board is stored in `config/gts/web/data/`.
Files in `config/gts/web/site/` override the website bundled in the jar.

## Installation

This mod can be ran on both [Fabric](https://fabricmc.net/) and [NeoForge](https://neoforged.net/). It has a few dependencies:

- Minecraft 1.21.1
- [Cobblemon](https://modrinth.com/mod/cobblemon)
- [Impactor](https://modrinth.com/mod/impactor)
- [Gooeylibs](https://github.com/NickImpact/GooeyLibs)

Building with the website needs Node.js (`npm`). Without it, or with `./gradlew build -PskipWeb`, the website is not rebuilt.


## Screenshots

![Gts Screen](https://i.imgur.com/laMTPyy.png)
![Pokemon Listing](https://i.imgur.com/1znf9Fk.png)
![Own Listing](https://i.imgur.com/sgl9jsN.png)
![History](https://i.imgur.com/m1ne2Li.png)
![Purchase Screen](https://i.imgur.com/U5xBVto.png)
![Moderation](https://i.imgur.com/JaStGsM.png)
## Documentation

[See the wiki.](https://github.com/PokeSplashDEV/GTS/wiki)


## License

[MIT](https://choosealicense.com/licenses/mit/)


## Authors

- [@bencrow11](https://www.github.com/bencrow11)
- [@poqdavid](https://www.github.com/poqdavid)
