# Dark God (Forge 1.20.1)

Devour a Command Block and ascend as a **Dark God** - Darkseid / Thanos style.
Add-on for **Godhood Core** (`viltrumitecore`); it also works without it.

## Becoming the Dark God
Hold a **Command Block** (any of the 3 kinds), **sneak + hold right-click** for 2 seconds.
You eat it, then a 6-second ascension plays: you rise, the screen darkens, lightning falls,
a shockwave launches everything nearby, and the sky turns to storm.
Get one with `/give @s command_block`.

Commands (op): `/darkgod transform [player]` (cinematic), `/darkgod grant [player]` (instant), `/darkgod revoke [player]`.
Eating a second Command Block as a god heals you and resets all cooldowns.

## What you get
Passive: +380 max health, +40 attack, +30 armor, speed, +6 reach, step-up, immunity to
fall/fire/drowning/explosions/lightning/wither/magic, 95% damage reduction on the rest,
no hunger, regeneration, debuffs wiped, night vision, flight (own, or Godhood Core's),
**refuses death** once per 5 minutes ("Darkness cannot be unmade"), and a **Dark Aura**
that withers/weakens/slows everything near you and corrupts terrain (Wither Storm style).

| Key | Power |
|-----|-------|
| R | **Omega Beams** - twin red eye beams, 96 block range, damage + fire + explosion |
| G | **Gravity Crush** - drags and slams everything within 24 blocks |
| Z | **Boom Tube** - teleport to where you look (up to 256 blocks) |
| V | **The Snap** - half of all living things within 96 blocks turn to dust (bosses spared) |
| B | Toggle **Dark Aura** |

All keys are rebindable under *Controls -> Dark God*. Everything is tunable in `config/darkgod-common.toml`.

## Godhood Core integration
If Godhood Core is installed, becoming a Dark God also sets you as a Viltrumite (unlocking its flight,
punch, dash, thunderclap, grab...) and maxes its damage-reduction stats; revoking restores the originals.
Turn it off with `useGodhoodCore = false`.

## Building the jar
Needs JDK 17 and internet (Gradle downloads Forge).

    ./gradlew build          (Windows: gradlew.bat build)

The mod is `build/libs/darkgod-1.0.0.jar`. Put it in your `mods` folder next to Godhood Core
(and its required flight mod). No JDK? Push this folder to a GitHub repo - the included
Actions workflow builds the jar and attaches it as a download.
