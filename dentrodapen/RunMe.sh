#!/bin/bash
systemctl daemon-reload
systemctl restart bluetooth
chmod 777 /var/run/sdp
cd server
mvn -e exec:java -Dexec.mainClass="server.RemoteBluetoothServer"
