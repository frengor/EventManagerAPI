# EventManagerAPI

Event manager API for Spigot plugins. Register only one bukkit listener per event, but run as many method per event as you want.

**JavaDoc:** <https://frengor.com/javadocs/EventManagerAPI/build-server/>

**Get it with maven:**
```xml
<repositories>
    <repository>
        <id>fren_gor</id>
        <url>https://nexus.frengor.com/repository/public/</url>
    </repository>
</repositories>
```   
```xml
<dependency>
    <groupId>com.frengor</groupId>
    <artifactId>eventmanagerapi</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

## Usage

```java
EventManager api = new EventManager(plugin);
Object listener = ... // This can be whatever object you want

// Send a welcome message to the players when they join
api.register(listener, PlayerJoinEvent.class, event -> event.getPlayer().sendMessage("Welcome to our server!"));

// Modify the welcome message
// Note that the event is registered with HIGH priority
api.register(listener, PlayerJoinEvent.class, EventPriority.HIGH, event -> event.setJoinMessage("A player has joined the server!"));

// Disable the API
api.disable();
```
