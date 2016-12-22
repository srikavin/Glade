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

public class JPLBoolean implements JPLDataType {
    private final boolean value;

    public JPLBoolean(boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return this;
    }

    @Override
    public JPLNumber asNumber() {
        return value ? new JPLNumber(1) : new JPLNumber(0);
    }

    @Override
    public JPLString asString() {
        return new JPLString(String.valueOf(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLBoolean) {
            if (((JPLBoolean) o).getValue() == value) {
                return true;
            }
        }
        return false;
    }
}
