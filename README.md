# SIRS
DriveKeeper project repository

## How to run
* Start the bluetooth server on any linux machine:


```sh
cd server
mvn clean install
mvn -e exec:java -Dexec.mainClass="server.RemoteBluetoothServer"
```
* It may be necessary to restart bluetooth drivers:
```sh
sudo systemctl daemon-reload
sudo systemctl restart bluetooth
```
* and give UDP local permissions:
```sh
sudo chmod 777 /var/run/sdp
```
* Once the server is running start the mobile application in any android device and follow the instructions on screen
