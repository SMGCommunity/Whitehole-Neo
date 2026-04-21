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
- Left Click: Select/Deselect object (hold <kbd>Shift</kbd> or <kbd>Ctrl</kbd> to select multiple)
- Left Click Drag: Pan camera, Move object
- Right Click Drag: Rotate camera
- Scroll Wheel: Move camera forward/backward, Move object forward/backward
- Arrow Keys + <kbd>PageUp</kbd>/<kbd>PageDown</kbd>: (Can switch to <kbd>W</kbd><kbd>A</kbd><kbd>S</kbd><kbd>D</kbd> + <kbd>E</kbd>/<kbd>Q</kbd> in the settings)
  - Hold <kbd>G</kbd> to move selected objects (Letter can be changed in the settings)
  - Hold <kbd>R</kbd> to rotate selected objects (Letter can be changed in the settings)
  - Hold <kbd>S</kbd> to scale selected objects (Letter can be changed in the settings)

## Useful Keyboard Shortcuts
- <kbd>Ctrl</kbd>+<kbd>C</kbd>: Copy selected objects
- <kbd>Ctrl</kbd>+<kbd>V</kbd>: Paste copied objects (positioned at the mouse)
- <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>V</kbd>: Paste copied objects (positioned at the position in the copy data)
- <kbd>Ctrl</kbd>+<kbd>Z</kbd>: Undo previous action
- <kbd>Shift</kbd>+<kbd>A</kbd>: Add object quick access menu
- <kbd>Space</kbd>: Jump camera to selected object(s)
- <kbd>Shift</kbd>+<kbd>Space</kbd>: Jump camera to selected zone
- <kbd>H</kbd>: Hide/Unhide selected objects
- <kbd>Alt</kbd>+<kbd>H</kbd>: Unhide All hidden objects
- <kbd>Delete</kbd>: Delete selected objects
- <kbd>Ctrl</kbd>+<kbd>N</kbd>: Truncate an object's positional values to remove the decimal parts
- <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>Alt</kbd>+<kbd>C</kbd>: Reset the selected path point control handles
- <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>R</kbd>: Reverse the selected path points
- <kbd>L</kbd>: Link the selected worldmap points together
- <kbd>P</kbd>: Switch worldmap points and links between their Yellow and Pink variants

## Libraries
- **jogamp**: https://jogamp.org/
- **gluegen**: https://jogamp.org/gluegen/www/
- **org.json**: https://github.com/stleary/JSON-java
- **flatlaf**: https://github.com/JFormDesigner/FlatLaf
- **JWindowsFileDialog**: https://github.com/JacksonBrienen/JWindowsFileDialog
- **DiscordIPC**: https://github.com/LogicismDev/DiscordIPC

