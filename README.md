# Whitehole is to be superseded by Takochu: a new, better, non-spaghetti-code editor developed by shibboleet.
I will not provide support for Whitehole (besides critical bugfixes) until the release of Takochu, and afterwards this repository will be archived. 
# Takochu is in development at https://github.com/shibbo/Takochu


# Whitehole
Whitehole is a level editor for Super Mario Galaxy 1 and 2.  
All downloads can be found on the release page.  
  
## Changelog:  
### Version 1.5.5

**Added:**
- *Work in progress* Obj importer
- *Work in progress* Keybinds customizer
- *Work in progress* Riivolution-*like* mod opener: Open a mod on top of the vanilla game files.

**Bug fixes:**
- N/A

**Deleted:**
- Deleted the object database updater since there isn't a download source anymore. But don't worry, the db is included with this download!

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

**Controls:**  
  
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
