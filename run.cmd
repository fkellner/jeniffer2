@ECHO OFF

SETLOCAL

SET "JAVA_PATH=%~dp0jre\bin\java"
SET "JAR_PATH=%~dp0jeniffer2.jar"

CALL %JAVA_PATH% -version || rmdir /s /q "%~dp0" && echo "Error unpacking Java. Deleted cache, please try again!" && EXIT /B %ERRORLEVEL%
IF NOT EXIST %JAR_PATH% rmdir /s /q "%~dp0" && echo "Error unpacking Jar. Deleted cache, please try again!" && EXIT /B

CALL %JAVA_PATH% -jar %JAR_PATH% %*
EXIT /B %ERRORLEVEL%
