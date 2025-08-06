import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;


public class SimpleLangCodeGenerator extends AbstractParseTreeVisitor<String> implements SimpleLangVisitor<String>
{
    private static final String stackMachineMacros = """
            .macro    PushImm     $number
                li            t1, $number
                sw            t1, (sp)
                addi          sp, sp, -4
            .end_macro
                                
            .macro    PushRel     $offset
                lw            t1, $offset(fp)
                sw            t1, (sp)
                addi          sp, sp, -4
            .end_macro
                                
            .macro    PopRel      $offset
                lw            t1, 4(sp)
                addi          sp, sp, 4
                sw            t1, $offset(fp)
            .end_macro
                            
            .macro    Reserve     $bytes
                addi          sp, sp, -$bytes
            .end_macro
                                
            .macro    Discard     $bytes
                addi          sp, sp, $bytes
            .end_macro
                                
            .macro    SetFP
                mv            fp, sp
            .end_macro
                                
            .macro    SaveFP
                sw            fp, (sp)
                addi          sp, sp, -4
            .end_macro
                                
            .macro    RestoreFP
                lw            fp, 4(sp)
                addi          sp, sp, 4
            .end_macro
                                
            .macro    Popt1t2
                lw            t1, 4(sp)
                addi          sp, sp, 4
                lw            t2, 4(sp)
                addi          sp, sp, 4
            .end_macro
                    
            .macro    CompGT
                Popt1t2
                li            t0, 1
                sw            t0, (sp)
                bgt           t1, t2, exit
                sw            zero, (sp)
            exit:
                addi          sp, sp, -4
            .end_macro
                                
            .macro    CompGE
                Popt1t2
                li            t0, 1
                sw            t0, (sp)
                bge           t1, t2, exit
                sw            zero, (sp)
            exit:
                addi          sp, sp, -4
            .end_macro
                            
            .macro    CompEq
                Popt1t2
                li            t0, 1
                sw            t0, (sp)
                beq           t1, t2, exit
                sw            zero, (sp)
            exit:
                addi          sp, sp, -4
            .end_macro
                                
            .macro    Invert
                lw            t1, 4(sp)
                li            t0, 1
                sw            t0, 4(sp)
                beqz          t1, exit
                sw            zero, 4(sp)
            exit:
            .end_macro
                                
            .macro    Plus
                Popt1t2
                add           t1, t1, t2
                sw            t1, (sp)
                addi          sp, sp, -4
            .end_macro
                                
            .macro    Minus
                Popt1t2
                sub           t1, t1, t2
                sw            t1, (sp)
                addi          sp, sp, -4
            .end_macro
                                
            .macro    Times
                Popt1t2
                mul           t1, t1, t2
                sw            t1, (sp)
                addi          sp, sp, -4
            .end_macro
                    
            .macro    Jump        $address
                j            $address
            .end_macro
                                
            .macro    JumpTrue    $address
                lw            t1, 4(sp)
                addi          sp, sp, 4
                beqz          t1, exit
                j             $address
            exit:
            .end_macro
                                
            .macro    Invoke      $address
                jal           next
            next:
                mv            t1, ra
                addi          t1, t1, 20
                sw            t1, (sp)
                addi          sp, sp, -4
                j             $address
            .end_macro
                                
            .macro    Return      $bytes
                lw            t1, 4(sp)
                addi          sp, sp, 4
                addi          sp, sp, $bytes
                jr            t1
            .end_macro
                                
            .macro    Print
                li            a7, 1
                lw            a0, 4(sp)
                addi          sp, sp, 4
                ecall
            .end_macro
                                
            .macro    PrintSpace
                li            a7, 11
                li            a0, 32
                ecall
            .end_macro
            .macro    CompLT
                 Popt1t2
                 li            t0, 1
                 sw            t0, (sp)
                 blt           t1, t2, exit
                 sw            zero, (sp)
             exit:
                 addi          sp, sp, -4
            .end_macro
                        
            .macro    CompLE
                 Popt1t2
                 li            t0, 1
                 sw            t0, (sp)
                 ble           t1, t2, exit
                 sw            zero, (sp)
             exit:
                 addi          sp, sp, -4
            .end_macro  
                        
                        
            .macro    AndOp
                Popt1t2              # Pop the top two values from the stack into t1 and t2
                li        t0, 1      # Load 1 into t0 to represent true
                sw        t0, (sp)   # Push true onto the stack by default
                beqz      t1, exit   # If t1 is zero (false), jump to exit
                beqz      t2, exit   # If t2 is zero (false), jump to exit
                j         end        # Both t1 and t2 are non-zero (true), so jump to end
            exit:
                sw        zero, (sp) # One of the operands is false, so replace the top of the stack with false
            end:
                addi      sp, sp, -4 # Adjust stack pointer
            .end_macro
                                                         
                                
            """;

    // This records the offset of each parameter: fp + n
    private final Map<String, Integer> localVars = new HashMap<>();

    // For simplicity, we will just use labels of the form "label_[some integer]"
    private int labelCounter = 0;
    // Member variable in SimpleLangCodeGenerator class
    private int localVarOffset;

    public String visitProgram(SimpleLangParser.ProgContext ctx, String[] args)
    {
        StringBuilder sb = new StringBuilder();

        // return value
        sb.append("""
        .text
                        
        # bootstrap loader that runs main()
        
        boot:
        
            PushImm     0       # return value
        
        """);

        for (int i = args.length - 1; i >= 0; --i) {

            if (args[i].equals("true")) {

                sb.append("""
                    PushImm     1
                """);

            } else if (args[i].equals("false")) {

                sb.append("""
                    PushImm     0
                """);

            } else {

                try {
                    sb.append(String.format("""
                        PushImm     %d
                    """, Integer.parseInt(args[i]))
                    );

                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(nfe);
                }

            }

        }

        sb.append("""
            Invoke      main
            lw          a0, 4(sp)
            addi        sp, sp, 4
            li          a7, 10
            ecall
        """);

        for (int i = 0; i < ctx.dec().size(); ++i) {
            sb.append(visit(ctx.dec().get(i)));
        }

        return (stackMachineMacros + sb.toString());
    }

    @Override public String visitProg(SimpleLangParser.ProgContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public String visitDec(SimpleLangParser.DecContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
        %s:
        """, ctx.typed_idfr(0).Idfr().getText())
        );

        sb.append("""
            SaveFP
            SetFP
        """
        );

        /*
          New FP (and SP initially)
                        points to -> [(available)]
                                     [Old FP]
                                     [Return address]
                                     [Param 0]
                                     ...
                                     [Param (params.size() - 1)]
                                     [Return value]
        */


        // Handle multiple parameters
        for (int i = 0; i < ctx.typed_idfr().size() - 1; ++i) {
            SimpleLangParser.Typed_idfrContext typedIdfr = ctx.typed_idfr().get(i + 1);
            localVars.put(typedIdfr.Idfr().getText(), 4 + 8 + i * 4);
        }


        sb.append(visit(ctx.body()));

        sb.append(String.format("""
        PopRel      %d
    """, 4 + 8 + (ctx.typed_idfr().size() - 1) * 4));

        sb.append(String.format("""
        RestoreFP
        Return      %d
    """, (ctx.typed_idfr().size() - 1) * 4));

        localVars.clear();
        // Initialize local variable offset
        localVarOffset = -4*(ctx.typed_idfr().size() - 1);

        return sb.toString();
    }





    @Override public String visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public String visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public String visitBody(SimpleLangParser.BodyContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ctx.ene.size(); ++i) {
            sb.append(visit(ctx.ene.get(i)));
            if (i != ctx.ene.size() - 1) {
                sb.append("""
                    Discard     4
                """
                );
            }
        }

        return sb.toString();

    }




    @Override public String visitBlock(SimpleLangParser.BlockContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ctx.ene.size(); ++i) {
            sb.append(visit(ctx.ene.get(i)));
            if (i != ctx.ene.size() - 1) {
                sb.append("""
                    Discard     4
                """
                );
            }
        }

        return sb.toString();

    }


    @Override public String visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.exp()));
        sb.append(String.format("""
            PopRel      (%d)
        """, localVars.get(ctx.Idfr().getText()))
        );

        sb.append("""
            PushImm     0       # dummy value
        """);

        return sb.toString();

    }

    @Override
    public String visitVarDeclInitExpr(SimpleLangParser.VarDeclInitExprContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.exp()));

        // Decrement the offset for the new variable
        localVarOffset -= 4;
        localVars.put(ctx.Idfr().getText(), localVarOffset);

        sb.append(String.format("PopRel      %d\n", localVarOffset));

        return sb.toString();
    }


    @Override public String visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.exp(1)));
        sb.append(visit(ctx.exp(0)));

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {

            case SimpleLangParser.Eq -> {

                sb.append("""
                    CompEq
                """
                );

            }

            case SimpleLangParser.Less -> {

                sb.append("""
                    CompGE
                    Invert
                """
                );

            }

            case SimpleLangParser.LessEq -> {

                sb.append("""
                    CompGT
                    Invert
                """
                );

            }

            case SimpleLangParser.Greater -> {
                sb.append("""
            CompLE
            Invert
        """);
            }

            case SimpleLangParser.GreaterEq -> {
                sb.append("""
            CompLT
            Invert
        """);
            }

            case SimpleLangParser.Plus -> {

                sb.append("""
                    Plus
                """
                );

            }

            case SimpleLangParser.Minus -> {

                sb.append("""
                    Minus
                """
                );

            }

            case SimpleLangParser.Times -> {

                sb.append("""
                    Times
                """
                );

            }
            case SimpleLangParser.And-> {
                sb.append("""
                            AndOp
                        """
                );
            }

            default -> {
                throw new RuntimeException("Shouldn't be here - wrong binary operator.");
            }

        }

        return sb.toString();
    }

    @Override public String visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        // return value
        sb.append("""
            PushImm     0       # return value
        """);

        for (int i = ctx.args.size() - 1; i >= 0; --i) {

            sb.append(visit(ctx.args.get(i)));

        }

        sb.append(String.format("""
            Invoke      %s
        """, ctx.Idfr().getText())
        );

        return sb.toString();

    }

    @Override public String visitBlockExpr(SimpleLangParser.BlockExprContext ctx)
    {
        return visit(ctx.block());
    }

    @Override
    public String visitRepeatUntilExpr(SimpleLangParser.RepeatUntilExprContext ctx) {
        StringBuilder sb = new StringBuilder();

        String startLabel = "label_" + labelCounter++;
        String endLabel = "label_" + labelCounter++;

        // Label to start the repeat block
        sb.append(startLabel + ":\n");

        // Visit the block inside the repeat statement
        sb.append(visit(ctx.block()));

        // Evaluate the condition in the until part
        sb.append(visit(ctx.exp()));

        // Use the macro to pop the condition result off the stack
        sb.append("PopRel      0\n"); // Adjust if your PopRel macro requires different parameters

        // Use a macro for conditional jumping. Assuming you have a JumpFalse macro or similar.
        sb.append(String.format("JumpFalse    %s, %s\n", startLabel, endLabel));

        // Label to end the repeat-until loop
        sb.append(endLabel + ":\n");

        return sb.toString();
    }


    @Override public String visitIfExpr(SimpleLangParser.IfExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        String thenLabel = String.format("label_%d", labelCounter++);
        String exitLabel = String.format("label_%d", labelCounter++);

        sb.append(visit(ctx.exp()));

        sb.append(String.format("""
            Invert
            JumpTrue    %s
        """, thenLabel)
        );

        sb.append(visit(ctx.block(0)));

        sb.append(String.format("""
            Jump        %s
        """, exitLabel)
        );

        sb.append(String.format("""
        %s:
        """, thenLabel)
        );

        sb.append(visit(ctx.block(1)));

        sb.append(String.format("""
        %s:
        """, exitLabel)
        );

        return sb.toString();
    }
    @Override public String visitPrintExpr(SimpleLangParser.PrintExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        if (ctx.exp().getClass() == SimpleLangParser.SpaceExprContext.class) {
            sb.append("""
                PrintSpace
            """
            );
        } else {
            sb.append(visit(ctx.exp()));
            sb.append("""
                Print
            """
            );
        }

        sb.append("""
            PushImm     0       # dummy value
        """);

        return sb.toString();
    }
    @Override public String visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append("""
            PushImm     0       # dummy value
        """);

        return sb.toString();
    }

    @Override public String visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
            PushRel     (%d)
        """, localVars.get(ctx.Idfr().getText()))
        );

        return sb.toString();
    }
    @Override public String visitIntExpr(SimpleLangParser.IntExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
            PushImm     %d
        """, Integer.parseInt(ctx.IntLit().getText()))
        );

        return sb.toString();
    }

    @Override
    public String visitBoolExpr(SimpleLangParser.BoolExprContext ctx) {
        String value = ctx.getText().equals("true") ? "1" : "0";
        return String.format("PushImm     %s\n", value);
    }

    @Override
    public String visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        return "# Skip expression (no operation)\n";
    }

    @Override public String visitEqBinop(SimpleLangParser.EqBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitLessBinop(SimpleLangParser.LessBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public String visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx) {
        return null;
    }

    @Override
    public String visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx) {
        return null;
    }

    @Override public String visitPlusBinop(SimpleLangParser.PlusBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitMinusBinop(SimpleLangParser.MinusBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitTimesBinop(SimpleLangParser.TimesBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public String visitAndBinop(SimpleLangParser.AndBinopContext ctx) {
        return """
            Popt1t2
            and t1, t1, t2
            sw t1, (sp)
            addi sp, sp, -4
        """;
    }

    @Override
    public String visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {
        StringBuilder sb = new StringBuilder();

        String loopStartLabel = String.format("label_%d", labelCounter++);
        String loopEndLabel = String.format("label_%d", labelCounter++);

        // Label for the start of the loop (for condition checking)
        sb.append(String.format("%s:\n", loopStartLabel));

        // Generate code for evaluating the loop condition
        sb.append(visit(ctx.exp()));

        // Invert the condition and jump to the end of the loop if false
        sb.append(String.format("""
        JumpTrue    %s
    """, loopEndLabel));

        // Generate code for the loop body
        sb.append(visit(ctx.block()));

        // Jump back to the start of the loop
        sb.append(String.format("""
        Jump        %s
    """, loopStartLabel));

        // Label for the end of the loop
        sb.append(String.format("%s:\n", loopEndLabel));


        sb.append("""
        PushImm     0       # dummy value for while loop result
    """);
        return sb.toString();
    }



}
