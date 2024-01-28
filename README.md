# Whitehole Neo
![Editing Flip-Swap Galaxy](https://github.com/SMGCommunity/Whitehole-Neo/blob/master/ExampleImage.png)
**Whitehole Neo** is a rewrite of *Whitehole*, a level editor for *Super Mario Galaxy* and *Super Mario Galaxy 2* that is over 10 years old. This goal with this version is to make the program more stable and to be properly maintained. It removes a lot of unused code and broken features that were introduced between versions 1.4 through 1.7, and improves on Despaghettification. Several outdated or questionable parts of the code have been upgraded, but there's still a lot more waiting to be improved. Major new additions include support for the [new object database](https://github.com/SunakazeKun/galaxydatabase), simple galaxy names that are displayed on the main window and a completely new Light Mode and Dark Mode UI. It is also fully compatible with all of SMG1's stages. Tons of bugs introduced in older versions of Whitehole have been fixed as well.

This is intended for Java 17, though the program runs fine on newer Java versions as well, but requires the tool to be started using this command: ```java --add-exports=java.desktop/sun.awt=ALL-UNNAMED -jar Whitehole.jar```.<br/>Alternatively, run the included `.bat` file

## Libraries
- **jogamp**: https://jogamp.org/
- **gluegen**: https://jogamp.org/gluegen/www/
- **org.json**: https://github.com/stleary/JSON-java
- **flatlaf**: https://github.com/JFormDesigner/FlatLaf
