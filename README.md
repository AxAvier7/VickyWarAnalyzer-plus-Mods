# Victoria II war analyzer

Or the full name Victoria II save game war analyzer. Naming stuff is hard.

Paradox forum thread: [link](https://forum.paradoxplaza.com/forum/index.php?threads/tool-victoria-ii-save-game-war-analyzer.689055/)

## Download links
* Version 1.0.1 that works with Java 7 [link](http://bit.ly/1Q9VicX)
* New version 1.2.1 that works with Java 8 [link](http://bit.ly/1aKLrtk)
* Or you can look inside the dist folder for all the versions. 

## What is it
The analyzer reads the save game produced by Victoria II (it's a strategy game, look it up) and presents all the wars in a family-friendly way. The program retrieves all the data that can be retrieved from the save file, such as total losses in a war, all the battles, wargoals and the war participants.

Most of the counties have a flag with them. Originally I planned to find all of them from the Victori II directory, but it converting .svg to a format usable by JavaFX turned out to be too difficult.

The flags of the mods are found as well. The `gfx/flags` folder of every mod in the install directory and in the Paradox user folder is searched, the mod of the save game first. Mods mostly use the TGA format, which is converted while reading. Extra mod folders can be added in the settings tab, separated with a semicolon.

The names of the countries of a mod are found as well. The `localisation` folder of the mod of the save game is read before the one of the game itself, so a mod that renames its countries no longer ends up with the vanilla names. Mods name a country after its government as well, as `USA_hms_government`, so the government of every country is read from the save game to find the name the game shows for it. A save game without a mod is only given the names of the game itself.

This analyzer is NOT a fully-fledged save game analyzer. It does one thing and does it reasonably well.

### Instructions
1. Make sure you have Java 8 installed on your computer
2. Run the jar file
3. Specify the save game. Usually the save games are in `C:\Users\USERNAME\Documents\Paradox Interactive\Victoria II\save games\`
4. Optionally you can point to the Victoria II install directory. The analyzer will retrieve the country names from there. 
5. Click "Read file" and see how terrible your wars have been. 
6. The analyzer will create a file called "paths.txt" in the jar directory. This stores the path to the last used save game directory.

### Screenshots
All wars tab:
![alt text](http://i.imgur.com/pldQN5y.png "All wars tab")
War details tab:
![alt text](http://i.imgur.com/45GCUTm.png "War details tab")
Battle details tab:
![alt text](http://i.imgur.com/g8TKMQu.png "Battle details tab")
Wargoals tab:
![alt text](http://i.imgur.com/CZWOSRn.png "Wargoals tab")


## Bugs and limitations
* The analyzer will produce many errors when it can't find the flag or the name for a country. Just ignore it.

* When the player country has fought no wars then it's name will be a TAG even if localisation is used.

* Some versions of Modern Age Mod produce save games that won't work with the analyzer. The newest versions should however work. 

* The names of the mods are only used when the save game is one of that mod. A save game of a mod that is not installed is given the names of every mod that is found, which may not be the right ones. Point to the mod folder in the settings tab to get the right names.

* The localisation files are read as ISO-8859-1, the way the game itself reads them.

## Tools
* Java 8
* JavaFX
* Maven 
* Maven JavaFX plugin https://github.com/zonski/javafx-maven-plugin

### Build process
If you really want to build your own version of this then go right ahead. I'm going to assume you know what Maven is and how to install it. You need to have Java 8.

1. Download this repository
2. Run `mvn install` in the base directory. Everything should install without problems.
3. Run `mvn jfx:jar` to compile it into a jar. The result an be found in `target/jfx/app`.
4. Use `mvn eclipse:eclipse` to generate the project files for Eclipse. You might also need the m2e Eclipse plugin.

### Code 
The UI design was made by me and with Java 7 it looked fine. With Java 8 it doesn't. Thanks, Oracle. Due to this, some words will be hidden and some tables will have empty columns.

The internal architecture is horrible and should be rebuilt from the ground up. Also, most comments are now outdated.
## About
This project was my first big Java project. I started working on it in April 2013 and released it in the Paradox forums. Then I forgot (or avoided) this project because I the code was so horrible. But around 1500 people downloaded it from the Paradox forums, so that was nice. 

The save game is read in by a hand-made parser. Suprisingly, it works. 

In April 2015 I finally got around to making it work with Java 8. I replaced the mostly static variables with object-oriented programming. I also replaced the Ant build with Maven.
