/*
 * Copyright 2014-2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.parser.impl.listeners;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * position-stmt       = position-keyword sep
 *                       position-value-arg-str stmtend
 * position-value-arg-str = < a string that matches the rule
 *                            position-value-arg >
 * position-value-arg  = non-negative-integer-value
 * non-negative-integer-value = "0" / positive-integer-value
 * positive-integer-value = (non-zero-digit *DIGIT)
 * zero-integer-value  = 1*DIGIT
 *
 * ANTLR grammar rule
 * positionStatement : POSITION_KEYWORD INTEGER STMTEND;
 */

import org.onosproject.yangutils.datamodel.YangBit;
import org.onosproject.yangutils.datamodel.YangBits;
import org.onosproject.yangutils.parser.Parsable;
import static org.onosproject.yangutils.parser.ParsableDataType.POSITION_DATA;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Implements listener based call back function corresponding to the "position"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class PositionListener {

    // Exact message in case position is invalid.
    private static String errMsg;

    /**
     * Creates a new position listener.
     */
    private PositionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (position), perform validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processPositionEntry(TreeWalkListener listener,
                                            GeneratedYangParser.PositionStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, POSITION_DATA, ctx.INTEGER().getText(), ENTRY);

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getParsableDataType()) {
            case BIT_DATA: {
                YangBit bitNode = (YangBit) tmpNode;
                if (!isBitPositionValid(listener, ctx)) {
                    ParserException parserException = new ParserException(errMsg);
                    parserException.setLine(ctx.INTEGER().getSymbol().getLine());
                    parserException.setCharPosition(ctx.INTEGER().getSymbol().getCharPositionInLine());
                    throw parserException;
                }
                bitNode.setPosition(Integer.valueOf(ctx.INTEGER().getText()));
                break;
            }
            default:
                throw new ParserException(
                        constructListenerErrorMessage(INVALID_HOLDER, POSITION_DATA, ctx.INTEGER().getText(), ENTRY));
        }
    }

    /**
     * Validates BITS position value correctness and uniqueness.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     * @return validation result
     */
    private static boolean isBitPositionValid(TreeWalkListener listener,
                                           GeneratedYangParser.PositionStatementContext ctx) {
        Parsable bitNode = listener.getParsedDataStack().pop();

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, POSITION_DATA, ctx.INTEGER().getText(), ENTRY);

        if (Integer.valueOf(ctx.INTEGER().getText()) < 0) {
            errMsg = "Negative value of position is invalid";
            listener.getParsedDataStack().push(bitNode);
            return false;
        }

        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getParsableDataType()) {
            case BITS_DATA: {
                YangBits yangBits = (YangBits) tmpNode;
                for (YangBit curBit : yangBits.getBitSet()) {
                    if (Integer.valueOf(ctx.INTEGER().getText()) == curBit.getPosition()) {
                        errMsg = "Duplicate value of position is invalid";
                        listener.getParsedDataStack().push(bitNode);
                        return false;
                    }
                }
                listener.getParsedDataStack().push(bitNode);
                return true;
            }
            default:
                listener.getParsedDataStack().push(bitNode);
                throw new ParserException(
                        constructListenerErrorMessage(INVALID_HOLDER, POSITION_DATA, ctx.INTEGER().getText(), ENTRY));
        }
    }
}