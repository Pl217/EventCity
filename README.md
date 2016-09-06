# EventCity

![alt text][logo]

[logo]: https://github.com/Pl217/EventCity/blob/master/app/src/main/res/mipmap-hdpi/ic_launcher.png "EventCity logo"


This is retroactively open sourced Android app, **not actively maintained**.

Application EventCity is intended for learning purposes and mostly comprised of native **Android SDK** code, without famous HTTP client libraries.

***
###Description

**EventCity** is Android application intended for city events searching, as well as posting new events. It utilises concept of gamification, where app users are ranked based on number (and type) of events visited. 

App's landing activity is login screen and every user _must have_ account in order to use the app. Upon logging in, user is located using Geolocation API, and user's location is actively tracked. 

User has the power of searching nearby events by radius or by category. There are 8 categories described on a server repo of this app [here](https://github.com/Pl217/EventCityServer "Application server"). Visiting events gives users points, depending on category of visited event. Users are ranked accordingly and everyone can see Top10 of the current users.

Application supports friendship concept between members. This feature is implemented using bluetooth, as users need to be phisically close to each other to become friend. If one gets a new friend at visited event, he receives bonus points.

***
###Server side application
Server side for this application is on a seperate repo, located [here](https://github.com/Pl217/EventCityServer "Application server").
