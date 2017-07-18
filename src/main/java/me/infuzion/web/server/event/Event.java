/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.event;

import me.infuzion.web.server.event.reflect.HandlerList;
import me.infuzion.web.server.response.ResponseGenerator;
import me.infuzion.web.server.router.Router;

public abstract class Event {
    private long creationTime = System.currentTimeMillis();

    private ResponseGenerator responseGenerator;

    public static HandlerList getHandler() {
        return HandlerList.getHandlerList();
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public final ResponseGenerator getResponseGenerator() {
        return responseGenerator;
    }

    public final void setResponseGenerator(ResponseGenerator generator) {
        this.responseGenerator = generator;
    }

    public abstract Router getRouter();

    public long getStartTime() {
        return creationTime;
    }
}
