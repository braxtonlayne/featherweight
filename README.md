# Featherweight

Memory fixes for big NeoForge 1.21.1 modpacks. Made so a large Create + medieval pack runs on an 8 GB M2 Mac
with a 4 GB Java heap. Works on client and server, and needs no config.

## What it does

1. **Structure templates expire.** Vanilla keeps every structure template it loads (village houses, towers,
   dungeons, ...) for the whole session. ModernFix's `dynamic_structure_manager` makes them soft references,
   but Java only clears those when the heap is almost full. Featherweight drops a template after 3 minutes
   without use and reloads it from disk when it's needed again. Templates created at runtime (structure blocks)
   are never dropped, so unsaved work is safe.
   Measured in the full pack: 118 MB of template data went to 1 MB after the player stopped exploring.
2. **Iron's Spellbooks' JEI imbue recipes show each spell at its lowest level only.** Iron's builds one recipe per
   imbuable item listing every spell at every level, and JEI loads all of them at startup (about 500,000 item
   stacks in that pack). Imbuing in game is unchanged. Measured: Iron's spell data in memory went from
   15.5 MB to 2.6 MB and its JEI startup from 3.7 s to 1.8 s. If Iron's isn't installed or its code changes,
   this part does nothing.

## Build

```
./gradlew build
```

The jar is `build/libs/featherweight-<version>.jar`.

## Debug

Set the `net.featherweight` logger to DEBUG to log the template cache size every 30 seconds when it changes.
