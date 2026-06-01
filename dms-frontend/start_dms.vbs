Set WshShell = CreateObject("WScript.Shell")

' 1. Start the Java Backend
WshShell.Run "cmd /k cd ..\dms && mvn spring-boot:run", 1, False

' 2. Start the Angular Frontend
WshShell.Run "cmd /k cd .\ && npm start", 1, False

' 3. Start the Global Tunnel
WshShell.Run "cmd /k npx localtunnel --port 4200", 1, False

' 4. Open the Website
WshShell.Run "explorer https://dirty-pears-stop.loca.lt", 1, False

MsgBox "All systems starting! Your Mobile App and Website will be ready in 60 seconds.", 64, "DMS Master Robot"
