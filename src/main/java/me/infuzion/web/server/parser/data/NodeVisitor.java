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

package me.infuzion.web.server.parser.data;

import me.infuzion.web.server.parser.Parser;
import me.infuzion.web.server.parser.data.jpl.JPLDataType;
import me.infuzion.web.server.parser.data.node.*;
import me.infuzion.web.server.parser.data.node.Number;

public interface NodeVisitor {
    JPLDataType visitBinOp(BinaryOperator node);

    JPLDataType visitNum(Number node);

    JPLDataType visitVarOp(VariableOperator node);

    default JPLDataType visit(Node node) {
        if (node instanceof BinaryOperator) {
            return visitBinOp((BinaryOperator) node);
        } else if (node instanceof Number) {
            return visitNum((Number) node);
        } else if (node instanceof UnaryOperator) {
            return visitUnOp((UnaryOperator) node);
        } else if (node instanceof VariableOperator) {
            return visitVarOp((VariableOperator) node);
        } else if (node instanceof NoOperator) {
            return visitNoOp((NoOperator) node);
        }
        throw new RuntimeException("No vistor found!");
    }

    default JPLDataType interpret(Parser parser) {
        return visit(parser.calc());
    }

    JPLDataType visitUnOp(UnaryOperator node);

    JPLDataType visitNoOp(NoOperator node);

}
