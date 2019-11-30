#!/bin/bash
sudo systemctl daemon-reload
sudo systemctl restart bluetooth
sudo chmod 777 /var/run/sdp
cd server
mvn clean install
mvn -e exec:java -Dexec.mainClass="server.RemoteBluetoothServer"
