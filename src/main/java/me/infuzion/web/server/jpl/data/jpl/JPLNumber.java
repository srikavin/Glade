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

package me.infuzion.web.server.jpl.data.jpl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class JPLNumber implements JPLDataType {
    private static final DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));


    private double value;

    public JPLNumber(double v) {
        this.value = v;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public JPLBoolean asBoolean() {
        return value == 1 ? new JPLBoolean(true) : new JPLBoolean(false);
    }

    @Override
    public JPLNumber asNumber() {
        return this;
    }

    @Override
    public JPLString asString() {
        df.setMaximumFractionDigits(340);
        return new JPLString(df.format(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JPLNumber) {
            if (((JPLNumber) o).value == value) {
                return true;
            }
        }
        return false;
    }
}
