The project is improved version of my JavaFX minimalistic text editor (https://github.com/OlexYarm/JfxFileContentEditor).
I added a few useful features, and fixed major problem - switched from TextArea to RichTextArea control (it's currently jfx-incubator-richtext).
So, it should be no limit on file size, highlighting for selection, and line numbers ...

1) Overview
The editor has all minimum and necessary features for editing test files small/medium size:
- create new file;
- open existing file with different charsets available for JavaFX installation;
- edit file, search and replace substring in opened file;
- change font size and font family to view file content;
- print file content;
- save file and keep backup copied of old version of files (up to defined number of copies);
- change line ending and charset while saving file;
- add file to favorites menu;
- edit favorites menu;
- re-open files not closed when editor was terminated;
- accessing all functionality with menu and toolbar;
- use tabs to open/modify/save files;
- adjust a few editor settings (number of backups, view font size and family, etc.).

2) Prerequisites
For building editor from source files following programs should be installed:
- Java SDK 24;
- Apache Maven 3.9.3 or later.
After image is created it could be copied and running on any computed supported by Java 24.

3) Building and running editor

3.1) Build and run from IDE using Maven
Download project source code from GitHub, import it to your IDE as project with existing pom.xml file, build and run editor.

3.2) Build from command-line using Maven
Download project source code from GitHub, change directory to project root directory (for example "...\JfxFileContentEditor").
Run below command from command-line to create image:

mvn clean compile javafx:jlink

It will create directory "...\JfxFileContentEditor\target\jfxEditor" with all files needed to run editor.
It will also compress all files to Zip archive jfxEditor.zip in directory "...\JfxFileContentEditor\target".

3.3) Run image from command-line command
Before running editor, the image should be created as explained above.
Copy archive jfxEditor.zip to desired location and unzip all files from it.
Or you can copy all files from directory "...\JfxFileContentEditor\target\jfxEditor" to desired location.
Change directory to "...\jfxEditor\bin".
Run below command from command-line to run editor:

java -m com.olexyarm.jfxfilecontenteditor/com.olexyarm.jfxfilecontenteditor.App %*

or on Windows run bat-file

jfxEditor.bat

The editor will create directory "...\JfxEditor" in user's home directory with files "Settings.properties" and "Favorites.txt" in it.
The editor will create directory "...\jfxEditor\bin\logs" with log files in it.
Log files will be rolled out and Zip compressed daily. The Zip archive files will be deleted after 60 days.

3.4) Build modular jar file from command-line using Maven
Download project source code from GitHub, change directory to project root directory "...\JfxFileContentEditor".
Run below command from command-line to create modular jar file:

mvn clean package

The directory "...\JfxFileContentEditor\target\release" will be created.
It will contain editor modular jar file and all dependencies jar files (JavaFX and logback).

3.5) Run modular jar file from command-line command
Before running editor modular jar file and dependencies jar files should be created as explained above.
Copy all files from directory "...\JfxFileContentEditor\target\release" to desired location.
Change directory to that directory (cd target\release).
Run below command from command-line to run editor:

java --module-path "." --module com.olexyarm.jfxfilecontenteditor/com.olexyarm.jfxfilecontenteditor.App %*

4) Known limitations
- the editor uses RitchTextArea control to show and edit content of file with all limitation on the control;
- a selection with "find/replace" does not change background because of known bug in RitchTextArea (JDK-8356436, JDK-8355774);
- there is no "prompt text" because such feature is missing in RitchTextArea;
- the "print" menu item always print only first page of file opened in editor because of limitation on implementation print functionality in JavaFX;
- the editor uses java.nio.channels.FileChannel for reading files and it locks opened files until editor is terminated,
this is well known bug in Java since 2002 (https://bugs.openjdk.org/browse/JDK-4724038)
and I did not want to use workaround because it could not work in next Java version (https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java).
