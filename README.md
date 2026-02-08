# Whitehole Neo
![Editing Flipswitch and Flip-Swap Galaxy](https://github.com/SMGCommunity/Whitehole-Neo/blob/master/ExampleImage.png)
**Whitehole Neo** is a rewrite of *Whitehole*, which was a level editor for *Super Mario Galaxy* and *Super Mario Galaxy 2* that is over 10 years old.
This goal with this version is to make the program more stable and to be properly maintained.
It removes a lot of unused code and broken features that were introduced between versions 1.4 through 1.7, and improves on Despaghettification.
Several outdated or questionable parts of the code have been upgraded.

Major new additions include:
- Support for the new [Object Database](https://github.com/SMGCommunity/galaxydatabase)
- Simple galaxy names that are displayed on the main window
- A reworked and simple Worldmap editor for SMG2
- A completely new Light Mode and Dark Mode UI
- The ability to Undo your actions
- Copy & Paste for all objects
- Vastly improved rendering capabilities
- Full compatability with all of SMG1's stages
- Tons of bugs introduced in older versions of Whitehole have been fixed

This is intended for Java 11, though the program runs fine on newer Java versions as well, but requires the tool to be started using this command: ```java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -Dsun.java2d.uiScale=1.0 -Dsun.awt.noerasebackground=true -jar Whitehole.jar```.<br/>Alternatively, run the included `.bat` file.

`-Dsun.java2d.uiScale=1.0` can be modified to scale the UI if it is too small (ex. `-Dsun.java2d.uiScale=1.5` will scale it 1.5x).

## Controls
- Left Click: Select/Deselect object (hold Shift/Ctrl to select multiple)
- Left Click Drag: Pan camera, Move object
- Right Click Drag: Rotate camera
- Scroll Wheel: Move camera forward/backward, Move object forward/backward
- Arrow Keys + PageUp/PageDown: (Can switch to WASD + EQ in the settings)
  - Hold G to move selected objects (Letter can be changed in the settings)
  - Hold R to rotate selected objects (Letter can be changed in the settings)
  - Hold S to scale selected objects (Letter can be changed in the settings)

## Useful Keyboard Shortcuts
- `Ctrl+C`: Copy selected objects
- `Ctrl+V`: Paste copied objects (positioned at the mouse)
- `Ctrl+Shift+V`: Paste copied objects (positioned at the position in the copy data)
- `Ctrl+Z`: Undo previous action
- `Shift+A`: Add object quick access menu
- `Spacebar`: Jump camera to selected object(s)
- `Shift+Spacebar`: Jump camera to selected zone
- `H`: Hide/Unhide selected objects
- `Alt+H`: Unhide All hidden objects
- `Delete`: Delete selected objects
- `Ctrl+N`: Truncate an object's positional values to remove the decimal parts
- `Ctrl+Shift+Alt+C`: Reset the selected path point control handles
- `Ctrl+Shift+R`: Reverse the selected path points
- `L`: Link the selected worldmap points together
- `P`: Switch worldmap points and links between their Yellow and Pink variants

## Libraries
- **jogamp**: https://jogamp.org/
- **gluegen**: https://jogamp.org/gluegen/www/
- **org.json**: https://github.com/stleary/JSON-java
- **flatlaf**: https://github.com/JFormDesigner/FlatLaf
- **JWindowsFileDialog**: https://github.com/JacksonBrienen/JWindowsFileDialog
- **DiscordIPC**: https://github.com/LogicismDev/DiscordIPC

