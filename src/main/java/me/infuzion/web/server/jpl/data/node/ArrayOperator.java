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

package me.infuzion.web.server.jpl.data.node;

import me.infuzion.web.server.jpl.data.jpl.JPLDataType;
import me.infuzion.web.server.jpl.data.jpl.JPLNull;

public class ArrayOperator extends Node {
    public final boolean assigning;
    public final JPLDataType key;
    public final String varName;
    public Node value;

    public ArrayOperator(boolean assigning, JPLDataType key, String varName, Node value) {
        this.assigning = assigning;
        this.key = key == null ? new JPLNull() : key;
        this.varName = varName;
        this.value = value == null ? new Node() : value;
    }

    public ArrayOperator(boolean assigning, JPLDataType key, String varName) {
        this(assigning, key, varName, null);
    }
}
