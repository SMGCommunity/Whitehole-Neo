# Whitehole Neo
![Editing Flip-Swap Galaxy](https://github.com/SMGCommunity/Whitehole-Neo/blob/master/ExampleImage.png)
**Whitehole Neo** is a rewrite of *Whitehole*, a level editor for *Super Mario Galaxy* and *Super Mario Galaxy 2* that is over 10 years old.
This goal with this version is to make the program more stable and to be properly maintained.
It removes a lot of unused code and broken features that were introduced between versions 1.4 through 1.7, and improves on Despaghettification.
Several outdated or questionable parts of the code have been upgraded, but there's still a lot more waiting to be improved.
Major new additions include support for the [new object database](https://github.com/SMGCommunity/galaxydatabase), simple galaxy names that are displayed on the main window, a completely new Light Mode and Dark Mode UI, The ability to Undo actions, and Copy & Paste for objects.
It is also fully compatible with all of SMG1's stages. Tons of bugs introduced in older versions of Whitehole have been fixed as well.

This is intended for Java 11, though the program runs fine on newer Java versions as well, but requires the tool to be started using this command: ```java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -Dsun.java2d.uiScale=1.0 -Dsun.awt.noerasebackground=true -jar Whitehole.jar```.<br/>Alternatively, run the included `.bat` file.

`-Dsun.java2d.uiScale=1.0` can be modified to scale the UI if it is too small (ex. `-Dsun.java2d.uiScale=1.5` will scale it 1.5x).

## Controls
- Left Click: Select/Deselect object (hold Shift/Ctrl to select multiple)
- Left Click Drag: Pan camera, Move object
- Right Click Drag: Rotate camera
- Scroll Wheel: Move camera forward/backward, Move object forward/backward
- Ctrl+C: Copy selected objects
- Ctrl+V: Paste selected objects (positioned at the mouse)
- Ctrl+Z: Undo previous action
- Shift+A: Add object quick access menu
- Spacebar: Jump camera to selected object
- H: Hide/Unhide selected objects
- Alt+H: Unhide All hidden objects
- Delete: Delete selected objects
- Arrow Keys + PageUp/PageDown: (Can switch to WASD + EQ in the settings)
  - Hold P to move selected objects
  - Hold R to rotate selected objects
  - Hold S to scale selected objects

## Libraries
- **jogamp**: https://jogamp.org/
- **gluegen**: https://jogamp.org/gluegen/www/
- **org.json**: https://github.com/stleary/JSON-java
- **flatlaf**: https://github.com/JFormDesigner/FlatLaf
- **JWindowsFileDialog**: https://github.com/JacksonBrienen/JWindowsFileDialog
