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

public class JPLString implements JPLDataType {
    private final String value;

    public JPLString(String value) {
        this.value = value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return new JPLBoolean(Boolean.parseBoolean(value));
    }

    @Override
    public JPLNumber asNumber() {
        return new JPLNumber(Double.parseDouble(value));
    }

    @Override
    public JPLString asString() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JPLString && value.equals(((JPLString) o).value);
    }

    @Override
    public String toString() {
        return value;
    }
}
