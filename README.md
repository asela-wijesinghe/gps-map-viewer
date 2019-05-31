# gps-map-viewer
Android application for visualizing custom GPS coordinates

Installation

1. import Database in to local MySql
2. run the node server app with "npm install" and "npm start"
3. after server is on get the api URL from browser and insert it to the "url" variable in LocationTracker android app -> DrawerActivity.java
4. Run the android application and data will be loaded soon

Due to some reasons with limitations with their quota given to free users, Direction API will not provide results to draw polygons (lines between locations)

![Imgur](https://i.imgur.com/MkbbOLYm.jpg)

![Imgur](https://i.imgur.com/7loCCrBm.jpg)
