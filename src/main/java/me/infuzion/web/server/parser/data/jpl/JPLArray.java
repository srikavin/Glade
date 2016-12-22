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

package me.infuzion.web.server.parser.data.jpl;

import java.util.HashMap;
import java.util.Map;

public class JPLArray implements JPLDataType {
    private Map<String, JPLDataType> value = new HashMap<>();

    public JPLDataType get(String key) {
        return value.getOrDefault(key, new JPLNull());
    }

    public void set(String key, JPLDataType value) {
        this.value.put(key, value);
    }

    @Override
    public JPLBoolean asBoolean() {
        throw new RuntimeException("Cannot convert an array to a boolean!");
    }

    @Override
    public JPLNumber asNumber() {
        throw new RuntimeException("Cannot convert an array to a number!");
    }

    @Override
    public JPLString asString() {
        String toRet = "Array {  ";
        for (Map.Entry<String, JPLDataType> e : value.entrySet()) {
            toRet = toRet.trim() + "\"" + e.getKey() + "\" => \"" + e.getValue().asString() + "\", ";
        }
        return new JPLString(toRet.substring(0, toRet.length() - 2) + "}");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLArray) {
            if (((JPLArray) o).value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
