## Fork

This is a fork of [bearofbusiness/MoreLoomLayers](https://github.com/bearofbusiness/MoreLoomLayers).

Changes in this fork:
- Updated to Paper 26.2 (Java 25)
- Folia compatible: all scheduling runs on the entity/region schedulers
- Banners with more than 5 layers can be duplicated in a crafting grid again
- Extra layers are stored by registry key instead of config ordinals, so the data
  survives config edits, Minecraft updates and datapack patterns
- `/showbannerlayers` is registered through the Brigadier API and actually works
- `config.yml` keeps the display names used for the extra tooltip lines

# MoreLoomLayers

This plugin allows you to have up to 16 pattern layers on a banner. Works normally using the loom.

![](/images/loom.png)

This works by tricking the loom into thinking that there are only 5 patterns on the banner so you can add more. :)

![](/images/banner.png)

Adds Extra Lore to emulate the Text normal text.

credit to https://www.planetminecraft.com/banner/minecraft-banner-454639/ for the banner

## Commands and permissions

| Command | Alias | Permission | Default |
| --- | --- | --- | --- |
| `/showbannerlayers` | `/sbl` | `moreloomlayers.showlayers` | everyone |
| `/moreloomlayers version` | `/mll version` | `moreloomlayers.version` | everyone |

Shows every layer of the banner in your main hand as a separate banner, and
reports the installed plugin build together with the server version.

## Building

Needs a JDK 25 (paper-api 26.2 ships Java 25 bytecode); Gradle picks it up through
the toolchain, so any JDK that can run Gradle 9 works to start the build.

```
./gradlew build
```

The plugin jar ends up in `build/libs/MoreLoomLayers.jar`.
