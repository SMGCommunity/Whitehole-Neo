[![Java CI](https://github.com/RealTheSunCat/Whitehole/actions/workflows/ant.yml/badge.svg)](https://github.com/RealTheSunCat/Whitehole/actions/workflows/ant.yml) ![Issues](https://img.shields.io/github/issues/RealTheSunCat/Whitehole?color=0088ff) ![Pull Requests](https://img.shields.io/github/issues-pr/RealTheSunCat/Whitehole?color=0088ff)

# Whitehole
Whitehole is a level editor for Super Mario Galaxy 1 and 2.  
All downloads can be found on the [release page](https://github.com/RealTheSunCat/Whitehole/releases).  

## Updating Java

**WHITEHOLE REQUIRES A VERY SPECIFIC JAVA SETUP! PLEASE BE AWARE OF THIS!** <br/>
- Oracle Java jre 8 <br/>
- Oracle Java jdk 11 <br/>
**ADOPTIUM CAN BE USED, BUT IT WILL NOT FIX SOME ISSUES!** <br/>
With this new Whitehole version, Java requires an update.
### How to update Java:

- On Linux, install `jre-openjdk-headless`

- On Windows, use a browser to navigate to https://adoptium.net/ and download the executable. Make sure you selected Java 17 (or whatever the latest version is). You can leave the default options for the installer, but make sure "set JAVA_HOME" is selected and proceed with the install. If Java apps stop opening, you may need to restart your computer to apply the registry changes.

## Troubleshooting
If Galaxies do not open, try this command:
`java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -jar Whitehole.jar`

If Galaxies continue to not open, I have no idea ask TheSunCat or Lord-Gigantucus.

## Help:
**Users:**
- TheSunCat#1007
- IonicPixels#3139
- Lord-Giganticus#7111

**Servers:**
- [Under Mario's Hat](https://discord.gg/TudSfUjHcW)
- [Luma's Workshop](https://discord.gg/k7ZKzSDsVq)
- [Kokiri Village](https://discord.gg/NTyb4sy)
- [Troller's Paradise](https://discord.gg/r8h5vAm2JC)

## Controls:  
  
Pan Camera: Left-mouse drag or arrow keys/numpad  
Rotate Camera: Right-mouse drag  
Zoom: Mousewheel or PageUp/Down or Numpad 3/9  
Select Object: Left click, hold Ctrl or Shift to select multiple  
Move Object: Drag left click on selected object(s)  
or: P + Arrow keys/Numpad  
or: press G and move the mouse  
  
Rotate Object: R + Arrow keys/Numpad  
or: press R and move the mouse  
  
Scale Object: S + Arrow keys/Numpad  
or: press S and move the mouse  
  
Delete Object: Delete  
Copy Selelction: Ctrl + C  
Paste Selection: Ctrl + V  
Undo: Ctrl-Z  
Redo: Ctrl-Y  
Screenshot: Ctrl + Shift + C  

## Changelog
### Version 1.7
**Added:**  
- Some area colors have been changed
- "MessageID.tbl" option added in The BCSV Editor, done by Evanbowl
- More BSCV stuff
- Use proper Shaders

**Bugs:**
- Dark Mode broke in the Galaxy Editor and BCSV Editor
- Little Endian mode does not work (forced off)

**Bug Fixes:**
- Title bar now shows "Whitehole v1.7" rather than "Whitehole"
- "CollisionArea" is now a cube, as it should be
- CloudSky Skybox issues no longer exist

### Version 1.5.5
**Added:**
- *Work in progress* Obj importer
- *Work in progress* Keybinds customizer
- *Work in progress* Riivolution-*like* mod opener: Open a mod on top of the vanilla game files.

**Bug Fixes:**
- N/A

### Version 1.5.4

Now introducing exciting features like... an improved PowerStarLight renderer... and... copy and paste!

**Key new features:**
- Select a mod folder to save any modified files to
- Remap main editor keybinds in the Settings menu
- RARC file association and editor
- A visual MSBT editor
- An MSBF editor (will be updated once the format is properly documented)
- A camera generator, directly integrated with the level editor (creates CAM_TYPE_XZ_PARA)
- A camera previewer
- Copy-and-paste objects at will
- Undo and redo changes
- Up to 3x faster load times
- Many productivity changes, including blender-like shortcuts for common actions
- Discord Rich Presence
- Anti-aliasing
- Screenshot the current view at the press of a button
- A TransparentWall10x10 and 10x20 renderer
- Better power star rendering
- Added openGL compatibility checks to ensure support for older video cards (requires it to be on in the settings)
- Made a ton of productivity changes to allow more control with the keyboard (ex: pressing enter when typing a path in the BCSV editor to open it)
- General system stability improvements to enhance the user's experience.

**Unfinished or semi-implemented features**
- A visual CANM (intro cutscene) editor

**Bugfixes**
- Fixed objects that are in rotated zones not moving in the correct direction when dragged
- Fixed the Map corrupting if cancel is pressed when asked to save changes
- Fixed whitehole not rendering some custom models that will still work ingame
- Fixed general saving bugs and added a handler for failed saving (thanks shibbo!)
- Made all error messages clearer and handled better (and with punctuation!!)
