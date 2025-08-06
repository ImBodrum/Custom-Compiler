import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SimpleLangInterpreter extends AbstractParseTreeVisitor<Integer> implements SimpleLangVisitor<Integer> {

    private final Map<String, SimpleLangParser.DecContext> global_funcs = new HashMap<>();
    private final Stack<Map<String, Integer>> frames = new Stack<>();
    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args) {
        // Register all functions
        for (SimpleLangParser.DecContext dec : ctx.dec()) {
            String functionName = dec.typed_idfr().Idfr().getText();
            global_funcs.put(functionName, dec);
        }

        // Find the 'main' function
        SimpleLangParser.DecContext main = global_funcs.get("main");
        if (main == null) {
            throw new RuntimeException("Main function not defined.");
        }

        // Prepare the new frame for the 'main' function's scope
        Map<String, Integer> newFrame = new HashMap<>();
        if (main.typed_idfrList() != null) {
            List<SimpleLangParser.Typed_idfrContext> params = main.typed_idfrList().typed_idfr();

            if (params.size() != args.length) {
                throw new RuntimeException("Argument count mismatch in 'main' function call. Expected: "
                        + params.size() + ", Provided: " + args.length);
            }

            // Process and store each argument in the new frame
            for (int i = 0; i < params.size(); ++i) {
                String paramName = params.get(i).Idfr().getText();
                newFrame.put(paramName, parseArgument(args[i]));
            }
        } else {
            if (args.length > 0) {
                throw new RuntimeException("Arguments provided but 'main' function does not take any.");
            }
        }

        // Push the new frame and visit the 'main' function's body
        frames.push(newFrame);
        Integer returnValue = visit(main.body());
        frames.pop();
        return returnValue;
    }

    // Helper method for parsing argument values
    private Integer parseArgument(String arg) {

        if (arg.equals("true")) {
            return 1;
        } else if (arg.equals("false")) {
            return 0;
        } else {
            try {
                return Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid argument format: '" + arg + "'");
            }
        }
    }

/*
    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args)
    {

        // Register all functions
        for (SimpleLangParser.DecContext dec : ctx.dec()) {
            String functionName = dec.typed_idfr().Idfr().getText();
            global_funcs.put(functionName, dec);
        }


        SimpleLangParser.DecContext main = global_funcs.get("main");

        Map<String, Integer> newFrame = new HashMap<>();
        if (main.typed_idfrList() != null) {
            List<SimpleLangParser.Typed_idfrContext> params = main.typed_idfrList().typed_idfr();
            if (params.size() != args.length) {
                throw new RuntimeException("Argument count mismatch in 'main' function call.");
            }

            for (int i = 0; i < params.size(); ++i) {
                String paramName = params.get(i).Idfr().getText();
                if (args[i].equals("true")) {
                    newFrame.put(paramName, 1);
                } else if (args[i].equals("false")) {
                    newFrame.put(paramName, 0);
                } else {
                    try {
                        newFrame.put(paramName, Integer.parseInt(args[i]));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid argument format for parameter '" + paramName + "'");
                    }
                }
            }
        } else {
            if (args.length > 0) {
                throw new RuntimeException("Arguments provided but 'main' function does not take any.");
            }
            // If there are no parameters, you might still want to push a new frame.
            newFrame = new HashMap<>();
        }

        frames.push(newFrame);
        return visit(main.body());

    }
*/

    @Override public Integer visitProg(SimpleLangParser.ProgContext ctx)
    {

        throw new RuntimeException("Should not be here!");

    }

    @Override public Integer visitDec(SimpleLangParser.DecContext ctx)
    {

        // Extract the function name
        String functionName = ctx.typed_idfr().Idfr().getText();

        // Check if the function has parameters and process them
        if (ctx.typed_idfrList() != null) {
            // Here you can handle the parameters.
            // For instance, you might want to add them to a new frame in the stack.
            List<SimpleLangParser.Typed_idfrContext> params = ctx.typed_idfrList().typed_idfr();
            Map<String, Integer> newFrame = new HashMap<>();
            for (SimpleLangParser.Typed_idfrContext param : params) {
                // Initialize parameters in the new frame. You might want to set them to a default value,
                // or handle them based on your language's semantics.
                newFrame.put(param.Idfr().getText(), 0); // Example: initializing parameters to 0
            }
            frames.push(newFrame);
        } else {
            // If there are no parameters, you might still want to push a new frame.
            frames.push(new HashMap<>());
        }

        Integer returnValue = visit(ctx.body());
        frames.pop();
        return returnValue;

    }

    @Override public Integer visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override
    public Integer visitTyped_idfrList(SimpleLangParser.Typed_idfrListContext ctx) {
        // Process each typed_idfr in the list.
        for (SimpleLangParser.Typed_idfrContext typedIdfr : ctx.typed_idfr()) {
            visit(typedIdfr);
        }

        // Return some integer value. In this case, it might be the size of the list,
        // or it could be any other integer value that makes sense in your application's context.
        return ctx.typed_idfr().size();
    }

    @Override public Integer visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
/*
    @Override public Integer visitBody(SimpleLangParser.BodyContext ctx) {

        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;

    }

*/
    @Override
    public Integer visitBody(SimpleLangParser.BodyContext ctx) {
        Integer returnValue = null;
        for (SimpleLangParser.StmtContext stmt : ctx.stmt()) {
            returnValue = visit(stmt);
        }
        return returnValue;
    }
    @Override
    public Integer visitStmt(SimpleLangParser.StmtContext ctx) {
        if (ctx.varDecl() != null) {
            return visitVarDecl(ctx.varDecl());
        } else if (ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return null;
    }
    @Override
    public Integer visitVarDecl(SimpleLangParser.VarDeclContext ctx) {
        // Assuming a Map<String, Integer> for variable values.
        // This will add the variable to the current frame with its initialized value.
        String varName = ctx.Idfr().getText();
        Integer varValue = visit(ctx.exp()); // Evaluate the expression for the initial value
        frames.peek().put(varName, varValue);
        return varValue;
    }


    @Override public Integer visitBlock(SimpleLangParser.BlockContext ctx)
    {
        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;
    }

    @Override public Integer visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {

        SimpleLangParser.ExpContext rhs = ctx.exp();
        frames.peek().replace(ctx.Idfr().getText(), visit(rhs));
        return null;

    }

    @Override public Integer visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx) {

        SimpleLangParser.ExpContext operand1 = ctx.exp(0);
        Integer oprnd1 = visit(operand1);
        SimpleLangParser.ExpContext operand2 = ctx.exp(1);
        Integer oprnd2 = visit(operand2);

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {

            case SimpleLangParser.Eq ->  {

                return ((Objects.equals(oprnd1, oprnd2)) ? 1 : 0);

            }
            case SimpleLangParser.Less -> {

                return ((oprnd1 < oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.LessEq -> {

                return ((oprnd1 <= oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.Plus -> {

                return oprnd1 + oprnd2;

            }
            case SimpleLangParser.Minus -> {

                return oprnd1 - oprnd2;

            }
            case SimpleLangParser.Times -> {

                return oprnd1 * oprnd2;
            }
            case SimpleLangParser.And-> {
                return (oprnd1 != 0 && oprnd2 != 0) ? 1 : 0;
            }
            case SimpleLangParser.Greater -> {
                return (oprnd1 > oprnd2) ? 1 : 0;
            }
            case SimpleLangParser.Div -> {
                if (oprnd2 == 0) {
                    throw new RuntimeException("Division by zero");
                }
                return oprnd1 / oprnd2;
            }
            case SimpleLangParser.GreaterEq -> {
                return (oprnd1 >= oprnd2) ? 1 : 0;
            }

            default -> {
                throw new RuntimeException("Shouldn't be here - wrong binary operator.");
            }

        }

    }
    @Override public Integer visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {

        String functionName = ctx.Idfr().getText();
        SimpleLangParser.DecContext dec = global_funcs.get(functionName);
        if (dec == null) {
            throw new RuntimeException("Function " + functionName + " not defined.");
        }

        // Directly access the typed_idfrList from the DecContext
        List<SimpleLangParser.Typed_idfrContext> paramList = dec.typed_idfrList().typed_idfr();

        if (paramList.size() != ctx.args.size()) {
            throw new RuntimeException("Argument count mismatch in function call " + functionName);
        }

        Map<String, Integer> newFrame = new HashMap<>();
        for (int i = 0; i < paramList.size(); i++) {
            String paramName = paramList.get(i).Idfr().getText();
            newFrame.put(paramName, visit(ctx.args.get(i)));
        }

        frames.push(newFrame);
        Integer returnValue = visit(dec.body()); // Assuming the body method returns the value of the function
        frames.pop();
        return returnValue;

    }

    @Override public Integer visitBlockExpr(SimpleLangParser.BlockExprContext ctx) {
        return visit(ctx.block());
    }


    @Override
    public Integer visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {
        // Evaluate the condition. Assuming visit(ctx.exp()) returns an Integer.
        while (visit(ctx.exp()) > 0) {
            // Execute the block within the while loop.
            visit(ctx.block());
        }
        return null; // Return null or appropriate value for your language semantics.
    }

    @Override public Integer visitIfExpr(SimpleLangParser.IfExprContext ctx)
    {

        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);
        if (condValue > 0) {

            SimpleLangParser.BlockContext thenBlock = ctx.block(0);
            return visit(thenBlock);

        } else {

            SimpleLangParser.BlockContext elseBlock = ctx.block(1);
            return visit(elseBlock);

        }

    }

    @Override public Integer visitPrintExpr(SimpleLangParser.PrintExprContext ctx) {

        SimpleLangParser.ExpContext exp = ctx.exp();

        if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.Space) {

            System.out.print(" ");

        } else if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.NewLine) {

            System.out.println();

        } else {

            System.out.print(visit(exp));

        }

        return null;

    }

    @Override public Integer visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        return null;
    }

    @Override public Integer visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
        return frames.peek().get(ctx.Idfr().getText());
    }

    @Override public Integer visitIntExpr(SimpleLangParser.IntExprContext ctx)
    {

        return Integer.parseInt(ctx.IntLit().getText());

    }
    @Override public Integer visitEqBinop(SimpleLangParser.EqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitLessBinop(SimpleLangParser.LessBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitPlusBinop(SimpleLangParser.PlusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitMinusBinop(SimpleLangParser.MinusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitTimesBinop(SimpleLangParser.TimesBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override
    public Integer visitAndBinop(SimpleLangParser.AndBinopContext ctx) {
        // Since the actual logic for handling the AND operation is in visitBinOpExpr,
        // this method can simply return null or a default value as it won't be used.
        return null;
    }

    @Override public Integer visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx){
        throw new RuntimeException("Should not be here!");
    };

    @Override public Integer visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx){
        throw new RuntimeException("Should not be here!");
    };
    @Override
    public Integer visitBoolExpr(SimpleLangParser.BoolExprContext ctx) {
        // Convert the boolean literal to its integer representation
        return ctx.getText().equals("true") ? 1 : 0;
    }

    @Override
    public Integer visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        // Since 'skip' does nothing, just return null or a default value
        return null;
    }

    @Override public Integer visitDivBinop(SimpleLangParser.DivBinopContext ctx)
    { throw new RuntimeException("Should not be here!");

    };

    @Override
    public Integer visitRepeatUntilExpr(SimpleLangParser.RepeatUntilExprContext ctx) {
        do {
            visit(ctx.block());
        } while (visit(ctx.exp()) == 0); // Assuming the expression returns 0 for false, 1 for true
        return null;
    }

}
