BSFinder - Paper add-on for BetterStructures 2.7.12

What it does:
- Listens for BetterStructures BuildPasteCompleteEvent.
- Records the center-bottom coordinates of each completed regular structure.
- Saves records in plugins/BSFinder/config.yml, so they survive restarts.
- Notifies online users with bsfind.use permission when a structure is generated.
- Provides /bsfind list, /bsfind latest, and /bsfind tp <number>.
- Permission defaults to OP.

Requirements:
- Paper 1.21.11 / Java 21
- BetterStructures 2.7.12 installed

Build:
1. Open this folder in your Gradle project or run `gradle build` in it.
2. The jar will be in build/libs/BSFinder-1.0.0.jar
3. Put that jar in the server's plugins folder and restart the server.

Important:
- This uses BetterStructures' documented BuildPasteCompleteEvent, so it records structures when their paste finishes.
- It does not retroactively discover structures that were generated before BSFinder was installed.
- This tracks regular pasted structures. BetterStructures modular dungeons generated in their own separate worlds use a different event and are not included in this first version.


OP-only access: Only in-game players who are server OPs can use /bsfind or receive structure notifications. Console use is denied.
