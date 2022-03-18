cd %~dp0

set moduleDir=..\..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\
set modulePackage=info\kgeorgiy\java\advanced\implementor\
set myPackage=info\kgeorgiy\ja\karaseva\implementor\
set fullPath=..\..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\

javac %fullPath%ImplerException.java -cp %fullPath%ImplerException.class %fullPath%Impler.java -cp %moduleDir% %fullPath%JarImpler.java
javac -cp %moduleDir% %myPackage%Implementor.java

mkdir %modulePackage%
move %fullPath%ImplerException.class %modulePackage%ImplerException.class
move %fullPath%Impler.class %modulePackage%Impler.class
move %fullPath%JarImpler.class %modulePackage%JarImpler.class

jar -cfm implementor.jar MANIFEST.MF %modulePackage%ImplerException.class %modulePackage%Impler.class %modulePackage%JarImpler.class %myPackage%Implementor.class

del %myPackage%Implementor.class
rd /s /q info\kgeorgiy\java\