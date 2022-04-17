# Whitehole v1.8 -- *The Despaghettification*
**Whitehole** is a level editor for Super Mario Galaxy 1 and 2 that was originally created 10 years ago. This is an experimental fork that aims to make Whitehole somewhat more stable while newer editors are being developed, such as Blackhole or Takochu. It removes a lot of unused code and broken features that were introduced between 1.4 and 1.7. Several parts of the code have been rewritten and there's a lot more waiting to be improved. Major new additions include support for the new object database, simple galaxy names that are displayed on the main window and a proper dark mode.
All downloads can be found on the [release page](https://github.com/RealTheSunCat/Whitehole/releases). It runs off **Java 17**. Due to JOGL being outdated, the program needs to be run using this command: ```java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -jar Whitehole.jar```

## Libraries
This Whitehole fork uses the following libraries:
- **jogamp**: https://jogamp.org/
- **gluegen**: https://jogamp.org/gluegen/www/
- **org.json**: https://github.com/stleary/JSON-java
- **flatlaf**: https://github.com/JFormDesigner/FlatLaf

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
