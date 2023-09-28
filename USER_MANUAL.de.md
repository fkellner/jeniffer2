# JENIFFER2 Benutzerhandbuch

![Jeniffer2 Logo](jeniffer2-logo.png)

_English version is available [here](USER_MANUAL.md)._

## Was ist Jeniffer2?

JENIFFER steht für **J**ava **E**nhanced **N**ef **I**mage **F**ile **F**ormat **E**dito**R**.
Version 2 benutzt stattdessen das Adobe-lizensierte DNG-Format mit offener Spezifikation als Input
(NEF ist das proprietäre Format von Nikon).

Jeniffer2 is Open Source Software zur Entwicklung von DNG-Dateien zu TIFF,
JPEG oder PNG und bietet eine große Auswahl an Demosaicing-Algorithmen.

## Systemvoraussetzungen

### Standalone Binaries

Momentan stellen wir 3 ausführbare Dateien zur Verfügung:

- `jeniffer2-linux-x64.bin` für Linux-Rechner mit Intel- oder AMD-Prozessoren
- `jeniffer2-windows-x64.exe` für Windows-Rechner mit Intel- oder AMD-Prozessoren
- `jeniffer2-mac-x64.bin` für Apple-Rechner mit Intel-Prozessoren

Außerdem gibt es ein experimentelles Binary für Apple Silicon (Macs mit M1-Chip).

Auf allen obigen Konfigurationen sollte auch die Jar-Distribution
laufen, zusätzlich deckt sie noch einige ARM-Systeme ab.

_Für andere Distributionen oder 32-Bit-Systeme müssen Sie im Quellcode selbst die Plattform-Spezifischen Teile der Abhängigkeiten hinzufügen und Jeniffer2 selbst kompilieren._

### Jar-Distribution

Jeniffer2 benötigt eine Java Laufzeitumgebung (JRE) der Version 17 oder höher.

Sie können die Version ihrer aktuellen Java Laufzeitumgebung testen, indem sie
ein Teminal (MacOS/Linux) oder eine Eingabeaufforderung (Windows) öffnen und
`java -version` eingeben. Die meisten Linux-Distributionen sollten Java bereits
installiert haben.

Wir empfehlen die Verwendung des OpenJDK JREs:

- Auf Linux sollten die meisten Distributionen das Paket `openjdk-17-jre` oder `openjdk-19-jre` in den Standard-Paketquellen haben, auf einem Debian-basierten Linux kann die Installation also über `sudo apt-get install openjdk-17-jre` erfolgen.
- Eclipse Temurin stellt [Ausführbare Installer](https://adoptium.net/temurin/releases/) für alle Betriebssysteme zur Verfügung ([Anleitung Windows](https://adoptium.net/installation/windows), [Anleitung MacOS](https://adoptium.net/installation/macOS), [Anleitung Linux](https://adoptium.net/installation/linux))

Wenn Sie Windows verwenden, sollten Sie darauf achten, die Option **`JAVA_HOME` Umgebungsvariable anpassen** auszuwählen. Nur so können Sie Java von der Eingabeaufforderung aus starten und dabei Konfigurationsoptionen wie den verfügbaren Arbeitsspeicher anpassen.

### RAW-Dateien im DNG-Format

Die meisten proprietären RAW-Formate können entweder mithilfe des [Adobe DNG Converter](https://helpx.adobe.com/de/camera-raw/using/adobe-dng-converter.html)s
(läuft nur auf Windows und MacOS) oder einem Online-Tool wie [diesem](https://www.onlineconverter.com/image)
in das DNG-Format konvertiert werden.

## Jeniffer2 ausführen

Wie man Jeniffer2 auführt, hängt vom Format und Betriebssystem ab:

### Standalone Binary

Die Standalone-Binaries können Sie einfach durch Doppelklicken ausführen (_auf MacOS/Linux müssen Sie vorher evtl die Datei noch ausführbar machen mit `chmod +x`_).
Um die Zeitmessungen und Debug-Output zu sehen, können sie auch aus
einem Terminal/Eingabeaufforderung ausgeführt werden.

**Bekannte Probleme:**
- Wenn auf Windows ein Fehler der Art `Error: Custom ( kind: Other, error: StringError("no tarball found inside binary") )` erscheint, sind eventuell die Überreste eines fehlgeschlagenen vorherigen Versuchs, Hilfsdateien zu entpacken, schuld. Wenn Sie in den Ordner `C -> Users -> <Username> -> AppData -> Local` navigieren (versteckter Ordner, die Einstellung, versteckte Ordner anzuzeigen, befindet sich im Windows Explorer im Tab "Ansicht"), können Sie den Ordner `warp` löschen und es nochmal versuchen.

### Jar-Distribution

Um das Java-Archiv auszuführen, müssen
Sie ein Terminal/eine Eingabeaufforderung in dem Ordner, in dem Sie die Datei gespeichert
haben, öffnen, und den folgenden Befehl ausführen:
```sh
# auf Linux und Windows
java -jar jeniffer2.jar
# auf MacOS
java -XstartOnFirstThread -jar jeniffer2.jar
```

### Die richtige Grafikkarte verwenden

Manche Laptops haben eine energiesparende und eine leistungsstarke
Grafikkarte eingebaut, und das Betriebssystem entscheidet anhand
verschiedener Kriterien, welche verwendet wird. Wenn beim Test der
auf der GPU implementierten Algorithmen ein Fehler angezeigt wird,
und sich in der Konsole der Fehlertext `OpenGL Error 1285` (kein Grafikspeicher mehr) befindet,
wird vermutlich die falsche, schwächere Grafikkarte verwendet.

Auf Windows kann für jede Anwendung explizit eingestellt werden, welche
Grafikkarte verwendet wird: Unter `Einstellungen -> Grafikeinstellungen` muss man die Einstellung "Desktop App" auswählen
und dann die Java-Binary der verwendeten Laufzeitumgebung auswählen.

Das ist vermutlich irgendwo im `C:\Users\IHR_NAME\AppData`-Ordner,
also muss im Explorer im Tab "Ansicht" die Option eingestellt sein,
versteckte Dateien anzuzeigen. Für die Standalone Binary ist der Pfad
`C:\Users\IHR_NAME\AppData\Local\warp\packages\jeniffer2-windows-x64.exe\jre\bin\java.exe`.

### Mehr RAM einstellen

Java-Programme haben eine festgelegt maximale RAM-Belegung (Heap Size), der
Standardwert auf Ihrem System ist möglicherweise zu gering. Um Jeniffer2 die Belegung
von mehr Arbeitsspeicher zu ermöglichen, können Sie beim Programmstart explizit ein
Maximum setzen, z.B. so:
```sh
java -Xmx8192M -jar Jeniffer2.1.jar
```
für 8GB oder so:
```sh
java -Xmx4096M -jar Jeniffer2.1.jar
```
für 4GB. **Setzen Sie diesen Wert niemals auf die Menge des im System verbauten RAMs!**
Andere Programme und vor allem Ihr Betriebssystem benötigen ebenfalls Arbeitsspeicher, um
zu funktionieren.

### Gespeicherter Zustand

Jeniffer2 schreibt eine versteckte Datei namens `folderSave` in den Ordner, in dem
es ausgeführt wird. Hier wird der zuletzt in der Baumansicht ausgeklappte Ordner
gespeichert, um diesen beim nächsten Öffnen wieder auszuklappen.

### Logs

Jeniffer2 erstellt einen Ordner namens `jeniffer2-logs` in dem Ordner, in dem es
aufgerufen wird. Hier werden Systeminformationen und Ausführungszeiten einzelner
Verarbeitungsschritte im CSV-Format gespeichert.
Sie können diese Dateien gerne mit einem Text-Editor, Python, R, Excel usw. öffnen,
wenn Sie neugierig sind, aber wenn Sie zur Forschung zur Entwicklung von Jeniffer2
beitragen wollen, ändern Sie diese Dateien bitte nicht, bevor Sie sie abgeben.

## Danksagung

Jeniffer2 wird unter der Leitung von Prof. Thomas Walter an der Uni Tübingen entwickelt.

Credits gehen an:

- Eugen Ljavin
- Joachim Groß
- Michael Kessler
- Claudia Grosch
- Andreas Reiter
- Florian Kellner

## Quellcode

Wir arbeiten gerade daran, den Quellcode von Jeniffer2 zu veröffentlichen.
Bis dahin können Sie [Florian Kellner](mailto:mr.florian.kellner@posteo.de)
kontaktieren, um eine Kopie des Quellcodes zu erhalten oder Feedback zu geben.
