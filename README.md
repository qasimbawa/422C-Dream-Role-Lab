# ECE 422C Dream Role Lab - Qasim Bawa

## 1. Direction

I want to be a CPU design verification engineer because I want to build software tools that help engineers test processors and understand their behavior. This combines my interest in computer architecture with programming.

## 2. Role research

Role: CPU Top-Level Design Verification Engineer, Apple.

Company careers link: https://jobs.apple.com/en-us/details/200657917-3760/cpu-top-level-design-verification-engineer?team=HRDWR

Research date: October 7, 2026 (America/Chicago).

The search index currently lists this company posting. The direct page requires JavaScript, so its live application status could not be confirmed. This is an aspirational role, not a claim that I currently meet all its qualifications.

The role involves developing verification environments and tests to check processor designs. Three relevant responsibilities or skills from the indexed company posting are:

1. Develop test plans, test benches, and test environments.
2. Develop tests in assembly and C/C++.
3. Use programming and scripting, including Python, for automation and tool development.

Skill demonstrated here: building and testing a Java tool that models pipeline timing. This is a small educational model, not a production verification environment.

## 3. Brainstorming with an agent

Agent: Codex. Starting context: ECE student interested in computer architecture, familiar with programming; project should be small enough to run and explain in a recitation. No exact time budget was provided, so the scope assumes one short lab session.

| Idea | Role fit | Relative effort | Demo value |
| --- | --- | --- | --- |
| CPU pipeline timing explorer | Strong: timing, dependencies, software modeling | Medium | Strong: compare forwarding modes |
| Assembly register dependency checker | Strong: assembly parsing and RAW dependency analysis | Low | Medium: list dependent instructions |
| Verification log summarizer | Strong: automation and test reporting | Low | Medium: summarize sample pass/fail logs |

Selected project: CPU Pipeline Timing Explorer.

One user: an ECE student studying pipelining.

One workflow: write a short assembly-like instruction sequence, run the simulator, then compare stalls and CPI with forwarding enabled and disabled.

## 4. Implementation plan and files

1. Parse supported instructions into objects with an opcode, destination, and source registers.
2. Represent IF, ID, EX, MEM, and WB as five slots.
3. Advance the pipeline one cycle at a time and insert bubbles for dependencies.
4. Display the timeline, total cycles, stalls, and CPI.
5. Check known timing examples and invalid inputs.

- `Pipeline.java`: instruction objects, parser, timing model, command-line interface.
- `demo.asm`: example with a load-use dependency and an ALU dependency.
- `PipelineTest.java`: standalone Java test runner with 13 checks.
- `verification.txt`: recorded run and test results.

## 5. Run instructions

Requires JDK 11 or newer (tested on Java 17). No third-party libraries, Maven, Gradle, or internet connection needed.

Extract the ZIP, open a terminal in the extracted project folder, and run:

```sh
javac Pipeline.java PipelineTest.java
java Pipeline
java Pipeline --no-forwarding
java PipelineTest
```

Use a JDK rather than only a JRE so `javac` is available. You can also open the folder in IntelliJ or Eclipse and run the `main` methods. Run from the project folder so `demo.asm` is found.

To run your own input:

```sh
java Pipeline my_program.asm
```

Supported syntax: `ADD`, `AND`, `XOR`, `LDW`, and `STW`, each with three operands. Registers are R0 through R7. Immediate values use a leading `#`. Semicolon comments and blank lines are allowed. Example: `ADD R2, R1, #1`.

This tool checks operand shape, not the full LC-3b encoding or immediate ranges.

## 6. Demo and edge case

Main demo:

1. Open `demo.asm`. Show that LDW writes R1 and the next ADD reads R1.
2. Run with forwarding: 4 instructions, 9 cycles, 1 stall, CPI 2.25.
3. Run without forwarding: 4 instructions, 12 cycles, 4 stalls, CPI 3.00.
4. Explain that forwarding resolves the ALU dependency, while the immediate load-use dependency still needs one bubble.

Edge case: create an empty input file and run it. The program reports zero instructions, zero cycles, and CPI 0.00 instead of dividing by zero. Alternatively, use `ADD R9, R0, #1` to show a readable input error.

## 7. Important code to explain

Java/OOP structure: `Instruction` holds an instruction's fields, `Cycle` stores one snapshot, and `Result` holds the timeline and summary methods. `ArrayList` stores a variable-length collection; `Integer[]` permits `null` for an empty stage. `Cycle` clones the stage array so old snapshots do not change when the pipeline advances. Static methods provide parsing and simulation without needing an application object. Input errors use exceptions caught by `main`.

`Instruction` groups information about a single instruction. `parse()` finds which registers it reads and writes. `simulate()` uses that information to detect read-after-write hazards.

In `simulate()`, `stages[1]` is the instruction waiting in ID. The loop checks producers in EX and MEM. Without forwarding, a matching destination causes a stall. With forwarding, only a matching load in EX causes a stall under this model.

During a stall, `updated[0] = stages[0]` and `updated[1] = stages[1]` keeps IF and ID unchanged. EX remains empty, inserting a bubble, while older instructions advance through MEM and WB. This lets the producer finish without allowing its dependent consumer to execute too early.

CPI is total cycles divided by instruction count. It includes initial pipeline filling and draining, so even an independent short program has CPI above one.

## 8. Assumptions and limits

- Single-issue, in-order, five-stage pipeline.
- Each stage takes one cycle; memory always completes in one cycle.
- WB writes are readable by ID in that same cycle.
- Forwarding supplies operands to EX; store data is also assumed required at EX. A machine with later store-data forwarding can have different store timing.
- No branches, condition codes, caches, structural hazards, or variable latencies.
- No actual register values or memory contents are computed. This is a timing model, not a functional LC-3b emulator.

## 9. Changes, verification, and reflection

Codex generated the initial implementation and made these concrete refinements:

- Excluded the final empty retirement step from the cycle count, so one instruction occupies five cycles rather than six.
- Included a defined empty-input CPI to avoid division by zero.
- Added readable parser errors and adjusted timeline spacing for readability.

Verification: all 13 Java checks passed, covering empty input, independent instructions, ALU forwarding, load-use stalls, store dependencies, and malformed input. The included demo was also run in both modes. These tests check selected timing cases, not every possible program.

Suggested explanation to practice after running it yourself:

"My project connects to processor verification because it turns pipeline rules into a software model that I can check with known examples. The most important part is hazard detection: it decides when to freeze younger instructions and insert a bubble. My next step would be adding branch resolution and flushing."

AI disclosure: Codex researched the role and created the project, documentation, and tests. Qasim should run the demo and review the code before presenting it. Any personal reflection about what Qasim learned or changed should be written after that review; the project does not claim he already performed those actions.
