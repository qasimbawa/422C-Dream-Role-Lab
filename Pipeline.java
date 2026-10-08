import java.nio.file.*;
import java.util.*;

/** Five-stage educational timing model; no branches or functional execution. */
public class Pipeline {
    static class Instruction {
        final String text, op, dest;
        final List<String> sources;
        Instruction(String text, String op, String dest, List<String> sources) {
            this.text=text; this.op=op; this.dest=dest; this.sources=sources;
        }
    }
    static class Cycle {
        final Integer[] stages;
        final boolean stalled;
        Cycle(Integer[] stages, boolean stalled) { this.stages=stages.clone(); this.stalled=stalled; }
    }
    static class Result {
        final List<Cycle> timeline=new ArrayList<>();
        int stalls;
        int cycles() { return timeline.size(); }
        double cpi(int count) { return count==0 ? 0 : (double)cycles()/count; }
    }
    static boolean register(String value) { return value.matches("R[0-7]"); }
    static boolean immediate(String value) { return value.matches("#-?\\d+"); }
    static List<Instruction> parse(String text) {
        List<Instruction> result=new ArrayList<>();
        String[] lines=text.split("\\R",-1);
        for(int i=0;i<lines.length;i++) {
            String line=lines[i].split(";",-1)[0].trim().toUpperCase(Locale.ROOT);
            if(line.isEmpty()) continue;
            String[] p=line.replace(',',' ').split("\\s+");
            if(p.length!=4 || !Arrays.asList("ADD","AND","XOR","LDW","STW").contains(p[0]))
                throw new IllegalArgumentException("Line "+(i+1)+": expected supported opcode and three operands.");
            if(!register(p[1]) || !register(p[2]))
                throw new IllegalArgumentException("Line "+(i+1)+": first two operands must be R0 through R7.");
            boolean memory=p[0].equals("LDW") || p[0].equals("STW");
            if(!(immediate(p[3]) || (!memory && register(p[3]))))
                throw new IllegalArgumentException("Line "+(i+1)+": invalid third operand.");
            List<String> sources=new ArrayList<>();
            String dest=p[1];
            if(p[0].equals("STW")) { dest=null; sources.add(p[1]); sources.add(p[2]); }
            else { sources.add(p[2]); if(register(p[3])) sources.add(p[3]); }
            result.add(new Instruction(line,p[0],dest,sources));
        }
        return result;
    }
    static Result simulate(List<Instruction> instructions,boolean forwarding) {
        Result result=new Result();
        Integer[] stages=new Integer[5]; // IF, ID, EX, MEM, WB; null means empty.
        int next=0;
        while(next<instructions.size() || Arrays.stream(stages).anyMatch(i->i!=null)) {
            boolean hazard=false;
            if(stages[1]!=null) {
                Instruction consumer=instructions.get(stages[1]);
                for(int s=2;s<=3;s++) {
                    if(stages[s]==null) continue;
                    Instruction producer=instructions.get(stages[s]);
                    if(producer.dest!=null && consumer.sources.contains(producer.dest)
                            && (!forwarding || (s==2 && producer.op.equals("LDW")))) hazard=true;
                }
            }
            Integer[] updated=new Integer[5];
            updated[4]=stages[3]; updated[3]=stages[2];
            if(hazard) {
                // Freeze IF/ID and leave EX empty, while older instructions advance.
                updated[0]=stages[0]; updated[1]=stages[1]; result.stalls++;
            } else {
                updated[2]=stages[1]; updated[1]=stages[0];
                if(next<instructions.size()) updated[0]=next++;
            }
            stages=updated;
            // Do not count the final empty retirement step.
            if(Arrays.stream(stages).allMatch(i->i==null)) break;
            result.timeline.add(new Cycle(stages,hazard));
        }
        return result;
    }
    public static void main(String[] args) {
        boolean forwarding=true, fileChosen=false;
        String filename="demo.asm";
        for(String arg:args) {
            if(arg.equals("--no-forwarding")) forwarding=false;
            else if(!arg.startsWith("--") && !fileChosen) { filename=arg; fileChosen=true; }
            else { System.err.println("Usage: java Pipeline [file.asm] [--no-forwarding]"); System.exit(2); }
        }
        try {
            List<Instruction> instructions=parse(Files.readString(Path.of(filename)));
            Result r=simulate(instructions,forwarding);
            System.out.println("CPU Pipeline Timing Explorer\nForwarding: "+forwarding);
            for(int i=0;i<instructions.size();i++) System.out.println("I"+(i+1)+": "+instructions.get(i).text);
            System.out.println("\nCycle    IF    ID    EX   MEM    WB    Event");
            for(int i=0;i<r.timeline.size();i++) {
                Cycle c=r.timeline.get(i); System.out.printf("%5d  ",i+1);
                for(Integer id:c.stages) System.out.printf("%-6s",id==null ? "." : "I"+(id+1));
                System.out.println(c.stalled ? "  STALL" : "");
            }
            System.out.printf(Locale.ROOT,"\nInstructions: %d | Cycles: %d | Stalls: %d | CPI: %.2f%n",
                    instructions.size(),r.cycles(),r.stalls,r.cpi(instructions.size()));
            if(instructions.isEmpty()) System.out.println("No instructions found. Add instructions and run again.");
        } catch(Exception error) { System.err.println("Error: "+error.getMessage()); System.exit(2); }
    }
}
