/** Standalone checks: no JUnit or -ea required. */
public class PipelineTest {
    static int checks;
    static void equal(int expected,int actual,String label) {
        if(expected!=actual) throw new AssertionError(label+": expected "+expected+", got "+actual);
        checks++;
    }
    public static void main(String[] args) {
        equal(0,Pipeline.simulate(Pipeline.parse(""),true).cycles(),"empty");
        equal(7,Pipeline.simulate(Pipeline.parse("ADD R1,R0,#1\nADD R2,R0,#2\nADD R3,R0,#3"),true).cycles(),"independent");
        var alu=Pipeline.parse("ADD R1,R0,#1\nADD R2,R1,#1");
        equal(6,Pipeline.simulate(alu,true).cycles(),"ALU forwarding");
        equal(8,Pipeline.simulate(alu,false).cycles(),"ALU without forwarding");
        var load=Pipeline.parse("LDW R1,R0,#0\nADD R2,R1,R3");
        equal(1,Pipeline.simulate(load,true).stalls,"load-use stall");
        equal(7,Pipeline.simulate(load,true).cycles(),"load-use cycles");
        equal(1,Pipeline.simulate(Pipeline.parse("LDW R1,R0,#0\nSTW R1,R0,#2"),true).stalls,"store dependency");
        for(String bad:new String[]{"BR #2","ADD R9,R0,#1","LDW R1,R0,R2","ADD R1,R0,nope"}) {
            try { Pipeline.parse(bad); throw new AssertionError("Accepted invalid input: "+bad); }
            catch(IllegalArgumentException expected) { checks++; }
        }
        var demo=Pipeline.parse("LDW R1,R0,#0\nADD R2,R1,R3\nXOR R4,R2,R5\nADD R6,R0,#1");
        equal(9,Pipeline.simulate(demo,true).cycles(),"demo forwarding");
        equal(12,Pipeline.simulate(demo,false).cycles(),"demo without forwarding");
        System.out.println("All "+checks+" checks passed.");
    }
}
