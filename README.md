# Glade [![Build Status](https://travis-ci.org/srikavin/Glade.svg?branch=master)](https://travis-ci.org/srikavin/Glade) [![codecov](https://codecov.io/gh/srikavin/Glade/branch/master/graph/badge.svg)](https://codecov.io/gh/srikavin/Glade) [![](https://jitpack.io/v/srikavin/Glade.svg)](https://jitpack.io/#srikavin/Glade)


An extensible and event-driven web server written with Java 11.


## Features
 - Automatic conversion to/from Objects in HTTP requests and responses
 - Fast response times with minimal overhead
 - Websocket support (WIP)

## Usage

Glade uses [JitPack.io](https://jitpack.io/#srikavin/Glade/master-SNAPSHOT) as a package repository. To add Glade to a Maven or
Gradle project use the following details:

__Maven__
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
	<dependency>
	    <groupId>com.github.srikavin</groupId>
	    <artifactId>Glade</artifactId>
	    <version>master-SNAPSHOT</version>
	</dependency>
</dependencies>
```

__Gradle__
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
        implementation 'com.github.srikavin:Glade:master-SNAPSHOT'
}
```

## Quick Start

```java
import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.Server;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.Route;
import me.infuzion.web.server.event.reflect.param.mapper.impl.QueryParam;
import me.infuzion.web.server.event.reflect.param.mapper.impl.Response;

import java.io.IOException;
import java.net.InetSocketAddress;

class Example {
    public static void main(String[] args) throws IOException {
        // Creates a server that listens to port 8971 on all interfaces
        Server server = new Server(new InetSocketAddress("0.0.0.0", 8971));

        EventManager manager = server.getEventManager();

        // Registers an event listener that responds to all incoming requests
        // Visiting '0.0.0.0:8971/?name=World' will display 'Hello, World' in a web browser
        manager.registerListener(new EventListener() {
            @EventHandler
            @Route("/")
            @Response("text/plain")
            public String handler(PageRequestEvent e, @QueryParam("name") String name) {
                return "Hello, " + name;
            }
        });

        // Starts the server (Note that this blocks while the server is running)
        server.start();
    }
}
```

### Contributing

Contributions are welcome! If you notice a bug or have a feature request feel free to open an 
[issue](https://github.com/srikavin/Glade/issues) or a [pull request](https://github.com/srikavin/Glade/pulls).