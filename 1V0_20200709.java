/* 1V0 */

/* - Pronounced as the name "Ivo", in honour of my beloved father. */

/* Enter -10 to exit civilly. */

/* This program is an "interpreted high-level quasi-assembler" for Arduino  */
/* and similarly "low-powered" devices. It provides a virtual machine which */
/* should look similar on all devices. Depending on the configuration, it   */
/* will typically be able to use at most about 64K RAM, but may operate in  */
/* full capacity already with about 16KB. It present the user with a        */
/* facility to enter programming steps and a pair of two addresses on which */
/* the entered operation is to be performed. While the reference            */
/* implementation may use strings, in fact, all I/O should be convertible   */
/* to just 12 symbols: numbers 0-9 and two special signs, e.g. * and #,     */
/* which allows the use of the system also on old-fashioned mobile phones:  */
/* all operations and all data should be possible to enter just in this     */
/* simple fashion. The system is conceived to store instructions in EEPROM  */
/* (or flash, for ARM) and data in SRAM, allowing non-volatile storage for  */
/* the laboriously constructed set of instructions.

/* Every instruction operates on two addresses and is expressed in 32 bit;  */
/* two designs thereby appear most sensible:                                */
/* aaaaaa bbbbbbbbbbbbb ccccccccccccc - 6bit instruction + 2x 13bit data    */
/* aaaaaaaa bbbbbbbbbbbb cccccccccccc - 8bit instruction + 2x 12bit data    */
/* Instructions 0-62 have been implemented, leaving instruction 63 reserved */
/* should the need arise to give it a final instruction e.g. for GPIO       */
/* operations and thelike.                                                  */

/* For the ATTINY85, I assume I will have only a handful instructions, <17, */
/* as the poor thing does not have even enough FLASH memory to accomodate   */
/* anything fancier! Thus, the instruction set will be reduced - I am       */
/* presently considering making it NOOP, JUMP, IADR, SADR, SVAL, IVAS,      */
/* PLUS, MINS, MULS, DIVS, POXY, LOXY, COPY, SUMR and OUTP -- where OUTP    */
/* may actually be supplanted by a user function, if so desired. 6bit data  */
/* addresses will be used - which really gives you 64 floats in 256 bytes   */
/* or 64 doubles in 512 bytes, and I do not think its SRAM will allow       */
/* anything further. This condenses instructions to 16bit, i.e. 256 in the  */
/* available EEPROM. Looks like I will have to cut corners for UNO, too.    */

/* The system has two parts: a sort of REPL for interaction with the user   */
/* right after startup, and also offers the facility to launch composed     */
/* sets of instructions to be interpreted in a sort of "virtual machine".   */

/* All operation is geared towards handling floating point values. Nobody   */
/* "in real life" really cares all that much about integers. Already it can */
/* be convincingly used as a sort of "scientific calculator", but this is   */
/* really just a gross oversimplification of the possibilities it offers,   */
/* as entire programs can be composed and run. */

/* All of this really follows the "philosophy of a spreadsheet":            */
/* where a user can solve rather complex problems by immediate intervention */
/* or with few commands, each command being quite complex in itself. This   */
/* is in contrast to the "high art of programming", where a user solves     */
/* arbitrarily complex tasks by chaining simple fragments, each in itself   */
/* quite incapable. The difference is "programming" requires the user to    */
/* undertake a "leap of faith", whereas users might simply need solutions.  */
/* My design idea is that I do not think "variables" are sensible to work   */
/* with, but rather, I am attempting to operate on "data fields". Allowing  */
/* involved operations with floats or more, I can get much faster practical */
/* results than by fiddling with integers variable by variable. Complex     */
/* operations better utilise the chip memory, too, and make "flash" usable  */
/* for things for which otherwise "SRAM" would be needed - and flash it is  */
/* that is plentiful. The functions should allow it to work like a sort of  */
/* "microcontroller spreadsheet" where operations are done on entire fields */
/* of cells. Shortening the needed operations with unfamiliar code should   */
/* also be more "pleasant" for the programmer. Using flash vs. SRAM thus    */
/* it becomes possible to "offload" some computational complexity from SRAM */
/* to "pre-fixated" memory - compared to ancient computers, this is a sort  */
/* of "wiring" rather than "keeping instructions in memory". Personally,    */
/* this let me understand why old computers were CISC: nothing you do is    */
/* very obvious, so whatever you do - better do it with fewer instructions. */
/* This makes an "assembler" more "intelligible" than a long chain of       */
/* cryptic minimal operations, like bit-negations and additions to form a   */
/* subtraction or thelike, which make no immediate sense in a listing.      */

/* I assume people PARTICULARLY hate "looping in a strange language",       */
/* and secondly, "conditions in a strange language". "Range" operations     */
/* seek to ameliorate that. Each "range" should be "padded" by an address   */
/* "below" it and an address "above" it for all range operations to work,   */
/* as these locations can be used to facilitate the range operation.        */

/* I had enough of watching "Arduino projects" take a perfect little        */
/* computer and turn it into a piece of junk, executing just one or two     */
/* "if-then"-s.                                                             */

/* Data Address 0 and Command Address 0 are reserved.                       */
/* A command with address 0 means "immediate execution".                    */

/* The system does not prefer "immediate failure", but tries to             */
/* "tolerate bugs". For complex programs, this is a dangerous design        */
/* decision, as they will still "run", but buggily. - The advantage is to   */
/* make it easy "to run anything at all", so some tolerance is built-in...  */
/* crashes are still possible, though.                                      */

/* A programmed space with jumps to exec0 has a similar effect to a         */
/* "file system" with "different programs". The user decides to which       */
/* executable section to "jump to", and the "returns" let him have more     */
/* than one composition of logical steps on a single computing device.      */

/* If you enter "#" in the string in the comment prompt, your previously    */
/* entered instruction will be turned into NOOP. This is a safety measure.  */

import java.util.Scanner;

public class IVO {

public static void main (String[] arg) {
Scanner Inpt = new Scanner(System.in);


int DATAMEM = 2001;
int EXECMEM = 1001;
float TOLERANCE = 0.0001f;
float PI = 3.14159265358979323846f;


float[] datum = new float[DATAMEM];
int[] instruction = new int[EXECMEM];
int instr = 0;
/* instruction format: */
/* operator, data address 1 (=result), data address 2 */
/* every operator is at a command address. */
int cmdadr = 0;
int opr = 0;
int datadr1 = 0;
int datadr2 = 0;
int pc = 0;
int h = 0; /* beware: h & k are used for "transcendental" instructions ... */
int i = 0;     /* i & j are auxiliary looping variables */
int j = 0;
int k = 0; /* ... so ZERO THEM OUT after non-transcendental usage */
float f = 0.0f; /* f & g are auxiliary floating point temporary storage */
float g = 0.0f;
char cmnt; /* does NOTHING, serves just as comment facility within terminal */
short tracer = 0;

int xx;
int yy;
int zz;

int runlimit = 0;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */


/* initialise commands and data to zero, which is no-op */
for (i=0; i<EXECMEM; i++) {
  instruction[i] = 0;
}

for (i=0; i<DATAMEM; i++) {
  datum[i] = 0.0f;
}

while (true) {
/* the highest level general loop which oscillates between command mode */
/* and general run mode */
System.out.println("SYSTEM READY");
/* HELP TEXT */



/* ------------------------ SETUP AND MAINTENANCE ------------------------ */
/* Read in instruction orders: */
/* - positive addresses: program for later execution; */
/* - zero-command-address: immediate execution according to operators; */
/* - specific negative addresses: specific immediate actions, not covered */
/*   by the usual operators. */
/* This is a kind of REPL or user command interface prior to actually */
/* "running" any sort of longer program. Here, the user still has more */
/* immediate influence over the machine. */

runlimit = 5 * EXECMEM; /* set it with -9, 0 means infinite. */
/* It is better to have a "fool-proof" default than 0. */
/* This is particularly useful on systems where you cannot save your */
/* instructions - such as your desktop computer - and do not wish to lose */
/* everything just because the machine went into an infinite loop. */

while (true) {
pc = 0; /* safety: nuke the program counter and the immediate instruction */
// EEPROM.put(ZERO_OFFSET, (long) 0);
instruction[0] = 0;

/* Arduino: flush here any read-cache before reading instructions. */

/* First, get the command address - to determine is it +, 0 or - .*/
/* From there it will depend what action is to be undertaken. */
System.out.print("CMD ADR  : ");
cmdadr = Inpt.nextInt();


/* COMMAND SELECTION IN REPL MODE */
/* Positive or zero - give the details of the instruction to be executed. */
if (cmdadr >= 0) {
  /* GIVE INSTRUCTION */

  System.out.print("OPERATION: ");
  opr = Inpt.nextInt();
  System.out.print("DATA ADR1: ");
  datadr1 = Inpt.nextInt();
  System.out.print("DATA ADR2: ");
  datadr2 = Inpt.nextInt();
/* COMMENT OUT, IF DESIRED, FROM HERE ...: */

  // Needed apparently on AVR, but not on ARM
  cmnt = 0;

  // The comment is optional - just press Enter if you do not want it.
  System.out.print("CMNT OR *: "); // A comment is not saved, merely printed
  while (true) {   // to the I/O dialogue. This itself may be useful.

    cmnt = Inpt.next().charAt(0);
    // System.out.print(cmnt);

    if (cmnt == '#') { // CANCEL 
      opr = 0;
      datadr1 = 0;
      datadr2 = 0;
      System.out.print("CANCELLED# ");
      // break ... no, not immediately, let the user explain why!
    }

    if ((char) cmnt == '*') {
      break; // Write just * and press Enter to exit the comment facility.
    }
  }
//  System.out.println();
/* ... TO HERE. */

  if ((datadr1 >= DATAMEM) || (datadr2 >= DATAMEM) ||
     (datadr1 < 0) || (datadr1 < 0) || (cmdadr >= EXECMEM)) {
    System.out.print("CORR RANGE. DATA 0 ... ");
    System.out.print((DATAMEM - 1));
    System.out.print(", INSTR 0 ... ");
    System.out.print((EXECMEM -1));
    System.out.println();
  }
  /* only positive data addresses - and 0 - are allowed: */
  datadr1 = java.lang.Math.abs(datadr1);
  datadr2 = java.lang.Math.abs(datadr2);

  /* force everything to be within range: */
  if (cmdadr >= EXECMEM) {
    cmdadr = EXECMEM - 1;
  }
  if (datadr1 >= DATAMEM) {
    datadr1 = DATAMEM - 1;
  }
  if (datadr2 >= DATAMEM) {
    datadr2 = DATAMEM - 1;
  }

  /* Having read the instruction, compose it and save it. */
  
  xx = opr & 63;
  yy = datadr1 & 8191;
  zz = datadr2 & 8191;
  instr = (xx << 26) | (yy << 13) | zz;

  instruction[cmdadr] = instr;
// EEPROM.put((INSTRUCTION_BYTE_LENGTH * (cmdadr - 0)) + ZERO_OFFSET, instr);

  /* Show again the user what was ordered, remind of possible correction. */
  /* First, print the address and the instruction. */
  /* Correction will only be possible if it is not for immediate execution. */
  if (cmdadr < 1000) { System.out.print("0"); } /* "leading zeroes. */
  if (cmdadr < 100) { System.out.print("0"); }
  if (cmdadr < 10) { System.out.print("0"); }
  System.out.print(cmdadr);
  System.out.print(" ");
  opr = (instr >> 26) & 63;
    if (opr == 0) {
      System.out.print("NOOP");
    } else if (opr == 1) {
      System.out.print("JUMP");
    } else if (opr == 2) {
      System.out.print("IADR");
    } else if (opr == 3) {
      System.out.print("OUTP");
    } else if (opr == 4) {
      System.out.print("INPT");
    } else if (opr == 5) {
      System.out.print("SADR");
    } else if (opr == 6) {
      System.out.print("SVAL");
    } else if (opr == 7) {
      System.out.print("IAAS");
    } else if (opr == 8) {
      System.out.print("IVAS");
    } else if (opr == 9) {
      System.out.print("PLUS");
    } else if (opr == 10) {
      System.out.print("MINS");
    } else if (opr == 11) {
      System.out.print("MULS");
    } else if (opr == 12) {
      System.out.print("DIVS");
    } else if (opr == 13) {
      System.out.print("POXY");
    } else if (opr == 14) {
      System.out.print("LOXY");
    } else if (opr == 15) {
      System.out.print("IFRA");
    } else if (opr == 16) {
      System.out.print("REMN");
    } else if (opr == 17) {
      System.out.print("AMNT");
    } else if (opr == 18) {
      System.out.print("PERD");
    } else if (opr == 19) {
      System.out.print("PCNT");
    } else if (opr == 20) {
      System.out.print("SWAP");
    } else if (opr == 21) {
      System.out.print("FACT");
    } else if (opr == 22) {
      System.out.print("COPY");
    } else if (opr == 23) {
      System.out.print("FRIS");
    } else if (opr == 24) {
      System.out.print("MNMX");
    } else if (opr == 25) {
      System.out.print("SORT");
    } else if (opr == 26) {
      System.out.print("CORS");
    } else if (opr == 27) {
      System.out.print("TURN");
    } else if (opr == 28) {
      System.out.print("SUMR");
    } else if (opr == 29) {
      System.out.print("SUSQ");
    } else if (opr == 30) {
      System.out.print("IXTH");
    } else if (opr == 31) {
      System.out.print("ABSR");
    } else if (opr == 32) {
      System.out.print("SQRT");
    } else if (opr == 33) {
      System.out.print("SQUA");
    } else if (opr == 34) {
      System.out.print("CBRT");
    } else if (opr == 35) {
      System.out.print("CUBE");
    } else if (opr == 36) {
      System.out.print("LNRN");
    } else if (opr == 37) {
      System.out.print("EXPR");
    } else if (opr == 38) {
      System.out.print("RADE");
    } else if (opr == 39) {
      System.out.print("DERA");
    } else if (opr == 40) {
      System.out.print("SIND");
    } else if (opr == 41) {
      System.out.print("COSD");
    } else if (opr == 42) {
      System.out.print("TAND");
    } else if (opr == 43) {
      System.out.print("ASND");
    } else if (opr == 44) {
      System.out.print("ACSD");
    } else if (opr == 45) {
      System.out.print("ATND");
    } else if (opr == 46) {
      System.out.print("MSTD");
    } else if (opr == 47) {
      System.out.print("ZERO");
    } else if (opr == 48) {
      System.out.print("RAND");
    } else if (opr == 49) {
      System.out.print("RUND");
    } else if (opr == 50) {
      System.out.print("CEIL");
    } else if (opr == 51) {
      System.out.print("TANH");
    } else if (opr == 52) {
      System.out.print("DTNH");
    } else if (opr == 53) {
      System.out.print("PLUR");
    } else if (opr == 54) {
      System.out.print("MINR");
    } else if (opr == 55) {
      System.out.print("MULR");
    } else if (opr == 56) {
      System.out.print("DIVR");
    } else if (opr == 57) {
      System.out.print("PLUN");
    } else if (opr == 58) {
      System.out.print("MINN");
    } else if (opr == 59) {
      System.out.print("MULN");
    } else if (opr == 60) {
      System.out.print("DIVN");
    } else if (opr == 61) {
      System.out.print("PROB");
    } else if (opr == 62) {
      System.out.print("STDD");
    } else if (opr == 63) {
      System.out.print("USER");
    } else {
      /* Such an "else" MAY come into existence if not ALL possible */
      /* instructions have been implemented, either theoretically, or */
      /* practically - simply due to a lack of flash space. */
      System.out.print("NOKO");
    }
    System.out.print(" ");

  /* Then, print the data addresses and the numbers these point to, and, */
  /* finally, the POSSIBLE numbers in turn these point to. */
  /* If no data has been entered "batch wise" manually, these will */
  /* all be zero. But if data has been entered manually, this may help the */
  /* user to keep track whether the program is being setup as is supposed. */
  /* Obviously, particularly each third number will often be meaningless, */
  /* unless the specific instruction really relates that far */
  if (datadr1 < 1000) { System.out.print("0"); } /* "leading zeroes" */
  if (datadr1 < 100) { System.out.print("0"); }
  if (datadr1 < 10) { System.out.print("0"); }
  datadr1 = (instr >> 13) & 8191;
  System.out.print(datadr1);
  System.out.print(">");
  System.out.print(datum[datadr1]);
  /* scientific, big E, sign */
  /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

  if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
    j = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    System.out.print(">");
    System.out.print(datum[j]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
  }
  System.out.print(" ");

  if (datadr2 < 1000) { System.out.print("0"); } /* "leading zeroes" */
  if (datadr2 < 100) { System.out.print("0"); }
  if (datadr2 < 10) { System.out.print("0"); }
  datadr2 = instr & 8191;
  System.out.print(datadr2);
  System.out.print(">");
  System.out.print(datum[datadr2]);
  /* scientific, big E, sign */
  /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

  if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
    j = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    System.out.print(">");
    System.out.print(datum[j]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
  }
  System.out.println();

if (cmdadr == 0) {
    break; /* Go execute the command and return, unless it is a jump: */
           /* a jump does not necessarily return, unless the execution */
           /* either reaches the end of the instruction space - */
           /* which it totally may without encountering any error, */
           /* given that 0 is "no operation" and the memory is filled */
           /* with it at the beginning - or the execution encounters */
           /* a jump back to command address 0. Such jumps may indeed */
           /* be used to "partition" the execution space into */
           /* "different programs" if desired, which are separated from */
           /* each other by "forced returns". In the immediate interpreter, */
           /* the user may then decide which "entry point" to jump to */
           /* in order to trigger execution at the desired section. */
  }

} else if (cmdadr == -1) {
  /* LIST INSTRUCTION RANGE */
  /* Give an output of the entered instructions between two addresses, */
  /* these addresses considered inclusive. Like "LIST" in BASIC, */
  /* to "let you see what you did" or "what you are about to run". */

  System.out.println("LIST INSTRUCTIONS");
  System.out.print("1ST CMD ADR: ");
  datadr1 = Inpt.nextInt();
  
  System.out.print("2ND CMD ADR: ");

  datadr2 = Inpt.nextInt();
  

  /* To save to time, if the listing is undesired, the user may */
  /* terminate this by giving any negative address. */
  if ((datadr1 < 0) || (datadr2 < 0)) { break; } /* fast eject */

  /* If both addresses are 0, this is a shorthand for "print everything" */
  if ((datadr1 == 0) && (datadr2 == 0)) {
    datadr1 = 1;
    datadr2 = EXECMEM - 1;
  } else if (datadr2 == 0) {
    /* A zero in the second address means "simply print one instruction" */
    datadr2 = datadr1;
  }

  /* Normalise ranges: always print low to high */
  if (datadr2 < datadr1) {
    k = datadr2;
    datadr2 = datadr1;
    datadr1 = k;
  }

  h = datadr1;
  k = datadr2;
  if (h < 1) { h = 1;}
  /* Don't print the "immediate execution" address. */
  /* This should remind the user that this address is special, */
  /* and that it should not be relied on for program execution. */

  /* Moreover, help the user if an out-of-range-address has been given. */
  if (k >= EXECMEM) { k = EXECMEM - 1; }

  System.out.println("INSTRUCTIONS:");
  for (i = h; i <= k; i++) {
    instr = instruction[i];
    // EEPROM.get((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
    xx = (instr >> 26) & 63; /* 4095:255:24:12 or 8191:63:26:13 */
    yy = (instr >> 13) & 8191;
    zz = instr & 8191;
    opr = xx;
    datadr1 = yy;
    datadr2 = zz;

    if (i < 1000) { System.out.print("0"); } /* "leading zeroes" */
    if (i < 100) { System.out.print("0"); }
    if (i < 10) { System.out.print("0"); }
    System.out.print(i);
    System.out.print(" ");
    if (opr < 10) { System.out.print("0"); }
    System.out.print(opr);
    System.out.print("=");
    if (opr == 0) {
      System.out.print("NOOP");
    } else if (opr == 1) {
      System.out.print("JUMP");
    } else if (opr == 2) {
      System.out.print("IADR");
    } else if (opr == 3) {
      System.out.print("OUTP");
    } else if (opr == 4) {
      System.out.print("INPT");
    } else if (opr == 5) {
      System.out.print("SADR");
    } else if (opr == 6) {
      System.out.print("SVAL");
    } else if (opr == 7) {
      System.out.print("IAAS");
    } else if (opr == 8) {
      System.out.print("IVAS");
    } else if (opr == 9) {
      System.out.print("PLUS");
    } else if (opr == 10) {
      System.out.print("MINS");
    } else if (opr == 11) {
      System.out.print("MULS");
    } else if (opr == 12) {
      System.out.print("DIVS");
    } else if (opr == 13) {
      System.out.print("POXY");
    } else if (opr == 14) {
      System.out.print("LOXY");
    } else if (opr == 15) {
      System.out.print("IFRA");
    } else if (opr == 16) {
      System.out.print("REMN");
    } else if (opr == 17) {
      System.out.print("AMNT");
    } else if (opr == 18) {
      System.out.print("PERD");
    } else if (opr == 19) {
      System.out.print("PCNT");
    } else if (opr == 20) {
      System.out.print("SWAP");
    } else if (opr == 21) {
      System.out.print("FACT");
    } else if (opr == 22) {
      System.out.print("COPY");
    } else if (opr == 23) {
      System.out.print("FRIS");
    } else if (opr == 24) {
      System.out.print("MNMX");
    } else if (opr == 25) {
      System.out.print("SORT");
    } else if (opr == 26) {
      System.out.print("CORS");
    } else if (opr == 27) {
      System.out.print("TURN");
    } else if (opr == 28) {
      System.out.print("SUMR");
    } else if (opr == 29) {
      System.out.print("SUSQ");
    } else if (opr == 30) {
      System.out.print("IXTH");
    } else if (opr == 31) {
      System.out.print("ABSR");
    } else if (opr == 32) {
      System.out.print("SQRT");
    } else if (opr == 33) {
      System.out.print("SQUA");
    } else if (opr == 34) {
      System.out.print("CBRT");
    } else if (opr == 35) {
      System.out.print("CUBE");
    } else if (opr == 36) {
      System.out.print("LNRN");
    } else if (opr == 37) {
      System.out.print("EXPR");
    } else if (opr == 38) {
      System.out.print("RADE");
    } else if (opr == 39) {
      System.out.print("DERA");
    } else if (opr == 40) {
      System.out.print("SIND");
    } else if (opr == 41) {
      System.out.print("COSD");
    } else if (opr == 42) {
      System.out.print("TAND");
    } else if (opr == 43) {
      System.out.print("ASND");
    } else if (opr == 44) {
      System.out.print("ACSD");
    } else if (opr == 45) {
      System.out.print("ATND");
    } else if (opr == 46) {
      System.out.print("MSTD");
    } else if (opr == 47) {
      System.out.print("ZERO");
    } else if (opr == 48) {
      System.out.print("RAND");
    } else if (opr == 49) {
      System.out.print("RUND");
    } else if (opr == 50) {
      System.out.print("CEIL");
    } else if (opr == 51) {
      System.out.print("TANH");
    } else if (opr == 52) {
      System.out.print("DTNH");
    } else if (opr == 53) {
      System.out.print("PLUR");
    } else if (opr == 54) {
      System.out.print("MINR");
    } else if (opr == 55) {
      System.out.print("MULR");
    } else if (opr == 56) {
      System.out.print("DIVR");
    } else if (opr == 57) {
      System.out.print("PLUN");
    } else if (opr == 58) {
      System.out.print("MINN");
    } else if (opr == 59) {
      System.out.print("MULN");
    } else if (opr == 60) {
      System.out.print("DIVN");
    } else if (opr == 61) {
      System.out.print("PROB");
    } else if (opr == 62) {
      System.out.print("STDD");
    } else if (opr == 63) {
      System.out.print("USER");
    } else {
      /* Such an "else" MAY come into existence if not ALL possible */
      /* instructions have been implemented, either theoretically, or */
      /* practically - simply due to a lack of flash space. */
      System.out.print("NOKO");
    }
    System.out.print(" ");

    if (datadr1 < 1000) { System.out.print("0"); } /* "leading zeroes". */
    if (datadr1 < 100) { System.out.print("0"); }
    if (datadr1 < 10) { System.out.print("0"); }
    System.out.print(datadr1);
    System.out.print(">");
    System.out.print(datum[datadr1]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
      j = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
      System.out.print(">");
      System.out.print(datum[j]);
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    System.out.print(" ");

    if (datadr2 < 1000) { System.out.print("0"); } /* "leading zeroes" */
    if (datadr2 < 100) { System.out.print("0"); }
    if (datadr2 < 10) { System.out.print("0"); }
    System.out.print(datadr2);
    System.out.print(">");
    System.out.print(datum[datadr2]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
      j = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
      System.out.print(">");
      System.out.print(datum[j]);
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    System.out.println();

    System.out.print("DECIMAL REPRESENTATION: ");
    System.out.println(instr);

  } /* End of the instruction print. */

  /* Always zero out h & k, they have a special meaning otherwise: */
  /* they signal that datadr1 and datadr2 should be replaced by them */
  /* in the next instruction. */
  h = 0;
  k = 0;

  j = 0;
  i = 0;

} else if (cmdadr == -2) {
  /* LIST DATA RANGE */
  /* Same as above, just this time, for data, not instructions. */
  /* Listing data is simpler, just the numbers are given */
  /* - without the pointer interpretation attempted above. */

  System.out.println("LIST DATA");
  System.out.print("1ST ADR: ");
  datadr1 = Inpt.nextInt();
  System.out.print("2ND ADR: ");
  datadr2 = Inpt.nextInt();
  if ((datadr1 < 0) || (datadr2 < 0)) { break; } /* fast eject */

  if ((datadr1 == 0) && (datadr2 == 0)) {
    /* print EVERYTHING */
    datadr1 = 1;
    datadr2 = DATAMEM - 1;
  } else if (datadr2 == 0) {
    datadr2 = datadr1;
  }

  /* Normalise ranges: low to high */
  if (datadr2 < datadr1) {
    k = datadr2;
    datadr2 = datadr1;
    datadr1 = k;
  }

  h = datadr1;
  k = datadr2;
  if (h < 1) { h = 1;} /* Do not print datum[0], it is reserved. */
  if (h >= DATAMEM - 2) { h = DATAMEM - 3; } /* correct maximum range */
  if (k >= DATAMEM - 2) { k = DATAMEM - 3; } /* correct maximum range */

  System.out.println("DATA:");
  for (i = h; i <= k; i = i + 2) { /* Print as many as the terminal allows. */
    if (i < 1000) { System.out.print("0"); } /* "Leading zeroes". */
    if (i < 100) { System.out.print("0"); }
    if (i < 10) { System.out.print("0"); }
    System.out.print(i);
    System.out.print(" ");
    System.out.print(datum[i]);
    System.out.print(" ");
    System.out.println(datum[i+1]);
  }

  /* As always, zero out h & k, to not trigger data address replacement */
  /* in the next instruction. */
  h = 0;
  k = 0;
  i = 0;

} else if (cmdadr == -3) {
  /* ENTER INSTRUCTIONS AS NUMBERS, UNTIL YOU SUBMIT A NEGATIVE INSTRUCTION */

  /* It may happen, e.g. due to a print-out, that you already KNOW what */
  /* decimal numbers the entire instructions will have, and that you */
  /* want to enter them quickly. This is possible by this interaction. */
  /* A negative instruction is impossible and terminates this interaction. */
  /* In other words, this range is "flexible" - enter as many as you wish. */
  /* The flexibility may be needed in case you want to enter a few 0s */
  /* - i.e. "no operation". */

  System.out.println("ENTER FULL INSTRUCTIONS AS DECIMALS, 67108863 TO END");
  System.out.print("START ADR: ");
  cmdadr = Inpt.nextInt(); /* From here upwards you enter instructions. */
  if (cmdadr < 0) { break; } /* fast eject */
  while (cmdadr < EXECMEM) {
    if (pc < 1000) { System.out.print("0"); } /* "Leading zeroes. */
    if (pc < 100) { System.out.print("0"); }
    if (pc < 10) { System.out.print("0"); }
    System.out.print(cmdadr);
    System.out.print(": INSTRUCTION: ");
    instr = Inpt.nextInt();

    if (instr == 67108863) { /* eject */
      break;
    } else {
      instruction[cmdadr] = instr;
 // EEPROM.put((INSTRUCTION_BYTE_LENGTH * (cmdadr - 0)) + ZERO_OFFSET, instr);
    }
    cmdadr++;
  }

} else if (cmdadr == -4) {
  /* ENTER DATA AS FLOATS WITHIN A PRE-SPECIFIED RANGE */
  /* Same as above, just this time, you enter data. */
  /* What is different: obviously, negative numbers are totally allowed. */
  /* So this time, the range is not flexible, but determined by two limits. */

  System.out.println("ENTER DATA FLOATS");
  System.out.print("1ST ADR: ");
  datadr1 = Inpt.nextInt();
  System.out.print("2ND ADR: ");
  datadr2 = Inpt.nextInt();
  if ((datadr1 < 0) || (datadr2 < 0) || (datadr2 < datadr1)) { break; }
  /* fast eject - and ONLY here; below, you are expected to give data */

  while (datadr1 <= datadr2) {
    if (datadr1 < 1000) { System.out.print("0"); } /* "leading zeroes" */
    if (datadr1 < 100) { System.out.print("0"); }
    if (datadr1 < 10) { System.out.print("0"); }
    System.out.print(datadr1);
    System.out.print(": DATUM: ");

    f = Inpt.nextFloat();
    // System.out.print(f);
    // System.out.println();
    
    datum[datadr1] = f;
    datadr1++;
  }

} else if (cmdadr == -5) {
  /* CLEAR INSTRUCTION RANGE */
  /* Simply erase to zero an entire section of program space, i.e. to */
  /* "no operation". */

  System.out.println("CLEAR INSTRUCTION RANGE");
  System.out.print("1ST CMD ADR: ");
  datadr1 = Inpt.nextInt();
  System.out.print("2ND CMD ADR: ");
  datadr2 = Inpt.nextInt();
  if ((datadr1 < 0) || (datadr2 < 0)) { break; } /* fast eject */

  if ((datadr1 == 0) && (datadr2 == 0)) {
    /* clear EVERYTHING */
    datadr1 = 1;
    datadr2 = DATAMEM - 1;
  } else if (datadr2 == 0) {
    datadr2 = datadr1; /* clear one single instruction */
  }

  /* Normalise ranges: low to high */
  if (datadr2 < datadr1) {
    k = datadr2;
    datadr2 = datadr1;
    datadr1 = k;
  }

  h = datadr1;
  k = datadr2;
  if (h < 1) { h = 1;} /* Do not clear address 0, it is reserved. */
  if (k >= EXECMEM) { k = EXECMEM - 1; } /* out-of-range-correction */

  instr = 0; /* Just to make sure it "understands" the size as 32bit. */
  for (i = h; i <= k; i++) {
    instruction[i] = instr;
    // EEPROM.put((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
  }
  System.out.println("INSTRUCTIONS CLEARED");

  h = 0;
  k = 0;
  i = 0;

} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0.0), not instructions. */

  System.out.println("CLEAR DATA RANGE");
  System.out.print("1ST ADR: ");
  datadr1 = Inpt.nextInt();
  System.out.print("2ND ADR: ");
  datadr2 = Inpt.nextInt();
  if ((datadr1 < 0) || (datadr2 < 0)) { break; } /* fast eject */

  if ((datadr1 == 0) && (datadr2 == 0)) {
    /* clear EVERYTHING */
    datadr1 = 1;
    datadr2 = DATAMEM - 1;
  } else if (datadr2 == 0) {
    datadr2 = datadr1; /* i.e. clear only one datum */
  }

  /* Normalise ranges: low to high */
  if (datadr2 < datadr1) {
    k = datadr2;
    datadr2 = datadr1;
    datadr1 = k;
  }

  h = datadr1;
  k = datadr2;
  if (h < 1) { h = 1;}
  if (k >= DATAMEM) { k = DATAMEM - 1; }

  for (i = h; i <= k; i++) {
    datum[i] = 0.0f;
  }
  System.out.println("DATA CLEARED");

  h = 0;
  k = 0;
  i = 0;

} else if (cmdadr == -7) {
  /* TRACING: show - or do not - which command is executed, */
  /* as the program runs. */
  System.out.println("TRACE ON");
  tracer = 1;
} else if (cmdadr == -8) {
  System.out.println("TRACE OFF");
  tracer = 0;
} else if (cmdadr == -9) {
System.out.print("SET RUNLIMIT, UP TO 32767, 0=INFINITE, TERMINATING WHEN 1: "
                );
  runlimit = Inpt.nextInt();
} else if (cmdadr == -10) {
  System.exit(0);

} /* end of command selection */

} /* end of setup and maintenance */












/* ----------------------- COMMAND EXECUTION PHASE ----------------------- */
/* All sails are set and you are now in the hands of Fate. The REPL has */
/* been exited, and whatever the Program Counter pc encounters is what */
/* shall be executed. If you enter into an infinite loop - tough luck! */

/* As described, all commands are defined by an opcode operating on two */
/* addresses, whereby the first one also, generally, bears the result and */
/* is, roughly speaking, "more important". Often, the second address may be */
/* set to 0 as a short hand to signify that it does not matter. */


while (pc < EXECMEM) {
  if (runlimit == 1) {
    System.out.print("RUN EXHAUSTED PC=");
    System.out.print(pc);
    System.out.print("STOPPED");
    System.out.println();
    break; /* ejection if the runlimit is over, pc printed for re-entry */
  } else if (runlimit > 1) {
    runlimit--;
    if (((runlimit - 1) % 100) == 0) {
      System.out.print("RUNLIMIT = ");
      System.out.print(runlimit);
      System.out.println();

      /* Give an indication of progress without annoying too often. */
    }
  } /* and do nothing if runlimit is 0. */

  /* Arduino: some sort of interruption due to reading the serial port */
  /* here would be desirable, for instance when reading "***".*/
  /* Either *** or ### are desirable, as they are similar to +++ */
  /* in modems, and moreover, are likely available on a mobile phone - */
  /* should this system ever be ported to a Java midlet. */
  /* .... Not necessary any more, since I implemented the run limit. */

  /* safety: erase the reserved datum[0]. */
  /* Do NOT erase here instruction[0], or you will prevent all execution: */
  /* whatever was there to be executed, would be annihilated! */
  datum[0] = 0.0f; /* Useful for unconditional jumps. */

  if (pc < 0) {
    pc = 0;
  }

  instr = instruction[pc];

  /* 63 operations possible, on 8191 places of data for each address; 26,13 */
  /* or: 255 operations, on 4095 data places for each address; 24,12 */
  opr = (instr >> 26) & 63;
  datadr1 = (instr >> 13) & 8191;
  datadr2 = instr & 8191;

  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) && (k > 0)) {
    datadr1 = h;
    datadr2 = k;
    h = 0;
    k = 0;
  }

  /* TRACE EXECUTION OF EACH PRESENT INSTRUCTION */
  /* This is actually rather similar to "LIST INSTRUCTION RANGE", */
  /* only now, not a "range" is listed, but each instruction as it is */
  /* readied for execution. The tracer must be set for this to happen, */
  /* as it will likely impact speed - particularly on Arduino, this may */
  /* involve printing delays to a terminal - and will generate A LOT */
  /* of output. */
  if (tracer != 0) {
  /* Arduino will have delays here, due to printing. */
    if (pc < 1000) { System.out.print("0"); } /* "Leading zeroes. */
    if (pc < 100) { System.out.print("0"); }
    if (pc < 10) { System.out.print("0"); }
    System.out.print(pc);
    System.out.print(" ");
    // if (cmdadr < 100) { System.out.print("0"); } /* if >99 instr. */
    if (cmdadr < 10) { System.out.print("0"); }
    System.out.print(opr);
    System.out.print("=");
    if (opr == 0) {
      System.out.print("NOOP");
    } else if (opr == 1) {
      System.out.print("JUMP");
    } else if (opr == 2) {
      System.out.print("IADR");
    } else if (opr == 3) {
      System.out.print("OUTP");
    } else if (opr == 4) {
      System.out.print("INPT");
    } else if (opr == 5) {
      System.out.print("SADR");
    } else if (opr == 6) {
      System.out.print("SVAL");
    } else if (opr == 7) {
      System.out.print("IAAS");
    } else if (opr == 8) {
      System.out.print("IVAS");
    } else if (opr == 9) {
      System.out.print("PLUS");
    } else if (opr == 10) {
      System.out.print("MINS");
    } else if (opr == 11) {
      System.out.print("MULS");
    } else if (opr == 12) {
      System.out.print("DIVS");
    } else if (opr == 13) {
      System.out.print("POXY");
    } else if (opr == 14) {
      System.out.print("LOXY");
    } else if (opr == 15) {
      System.out.print("IFRA");
    } else if (opr == 16) {
      System.out.print("REMN");
    } else if (opr == 17) {
      System.out.print("AMNT");
    } else if (opr == 18) {
      System.out.print("PERD");
    } else if (opr == 19) {
      System.out.print("PCNT");
    } else if (opr == 20) {
      System.out.print("SWAP");
    } else if (opr == 21) {
      System.out.print("FACT");
    } else if (opr == 22) {
      System.out.print("COPY");
    } else if (opr == 23) {
      System.out.print("FRIS");
    } else if (opr == 24) {
      System.out.print("MNMX");
    } else if (opr == 25) {
      System.out.print("SORT");
    } else if (opr == 26) {
      System.out.print("CORS");
    } else if (opr == 27) {
      System.out.print("TURN");
    } else if (opr == 28) {
      System.out.print("SUMR");
    } else if (opr == 29) {
      System.out.print("SUSQ");
    } else if (opr == 30) {
      System.out.print("IXTH");
    } else if (opr == 31) {
      System.out.print("ABSR");
    } else if (opr == 32) {
      System.out.print("SQRT");
    } else if (opr == 33) {
      System.out.print("SQUA");
    } else if (opr == 34) {
      System.out.print("CBRT");
    } else if (opr == 35) {
      System.out.print("CUBE");
    } else if (opr == 36) {
      System.out.print("LNRN");
    } else if (opr == 37) {
      System.out.print("EXPR");
    } else if (opr == 38) {
      System.out.print("RADE");
    } else if (opr == 39) {
      System.out.print("DERA");
    } else if (opr == 40) {
      System.out.print("SIND");
    } else if (opr == 41) {
      System.out.print("COSD");
    } else if (opr == 42) {
      System.out.print("TAND");
    } else if (opr == 43) {
      System.out.print("ASND");
    } else if (opr == 44) {
      System.out.print("ACSD");
    } else if (opr == 45) {
      System.out.print("ATND");
    } else if (opr == 46) {
      System.out.print("MSTD");
    } else if (opr == 47) {
      System.out.print("ZERO");
    } else if (opr == 48) {
      System.out.print("RAND");
    } else if (opr == 49) {
      System.out.print("RUND");
    } else if (opr == 50) {
      System.out.print("CEIL");
    } else if (opr == 51) {
      System.out.print("TANH");
    } else if (opr == 52) {
      System.out.print("DTNH");
    } else if (opr == 53) {
      System.out.print("PLUR");
    } else if (opr == 54) {
      System.out.print("MINR");
    } else if (opr == 55) {
      System.out.print("MULR");
    } else if (opr == 56) {
      System.out.print("DIVR");
    } else if (opr == 57) {
      System.out.print("PLUN");
    } else if (opr == 58) {
      System.out.print("MINN");
    } else if (opr == 59) {
      System.out.print("MULN");
    } else if (opr == 60) {
      System.out.print("DIVN");
    } else if (opr == 61) {
      System.out.print("PROB");
    } else if (opr == 62) {
      System.out.print("STDD");
    } else if (opr == 63) {
      System.out.print("USER");
    } else {
      /* Such an "else" MAY come into existence if not ALL possible */
      /* instructions have been implemented, either theoretically, or */
      /* practically - simply due to a lack of flash space. */
      System.out.print("NOKO");
    }
    System.out.print(" ");

    if (datadr1 < 1000) { System.out.print("0"); } /* "leading zeroes" */
    if (datadr1 < 100) { System.out.print("0"); }
    if (datadr1 < 10) { System.out.print("0"); }
    System.out.print(datadr1);
    System.out.print(">");
    System.out.print(datum[datadr1]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
      j = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
      System.out.print(">");
      System.out.print(datum[j]);
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    System.out.print(" ");

    if (datadr2 < 1000) { System.out.print("0"); } /* "leading zeroes" */
    if (datadr2 < 100) { System.out.print("0"); }
    if (datadr2 < 10) { System.out.print("0"); }
    System.out.print(datadr2);
    System.out.print(">");
    System.out.print(datum[datadr2]);
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
      j = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
      System.out.print(">");
      System.out.print(datum[j]);
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    System.out.println();

    i = 0;
  } /* end of instruction tracing */

  /* ---------- DECODE ---------- */
  /* Quite simply: "do different things depending on what operation (opr) */
  /* is presently indicated by the program counter (pc). */
  /* There are two instructions which deal with program CONTROL, and */
  /* all other instructions deal with reading, writing and storing values */
  /* as well as with mathematical operations, which are indeed the focus. */
  /* The one is to jump if at a certain address the value is (close to) */
  /* zero or negative - and this is the ONLY branch instruction, and thus */
  /* loops are to be implemented by "jumping backwards" and "reducing the */
  /* addressed value" - and the other instruction sets datadr1 and datadr2 */
  /* to be overwritten by certain values upon the next instruction; this */
  /* should allow rather flexible programming. - This section "DECODE" it */
  /* is where you may really adjust this whole system to your taste: */
  /* shuffle opr-values, reduce or extend or modify the instructions, etc. */

  /* WHATEVER IS NOT MATCHED: NO OPERATION [NOP] / RESERVED */
  /* This particularly applies to "Operation 0", so all starts with 1. */

  if (opr == 0) {
  /* NOOP -- NO OPERATION: Do nothing. */
  /* Any non-implemented opcode will work as NOP. */
  /* This is also nice if you want to temporarily de-activate certain code. */

  } else if (opr == 1) {
  /* JUMP */
  /* JUMP IF THE FIRST ADDRESS CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
  /* "Nearly" zero, because these are floats and repeated float operations */
  /* may render the results imprecise. */
  /* This can very neatly be translated into a high language's IF & WHILE. */
  /* Anyway, welcome to "spaghetti programming". You will use this sole */
  /* condition until you learn to LOVE IT! */

    if (datum[datadr1] <= TOLERANCE) {
      /* pc = java.lang.Math.abs(java.lang.Math.round(datum[datadr2])); */
      /* However, such a default is */
      /* NOT a good idea, actually: with a default COMPUTED GOTO, you */
      /* normally straight GOTO HELL, and have NO adequate way to trace it. */
      /* Instead, let the default be OBVIOUS: */

      pc = datadr2; 
      /* On purpose NOT "computed", but IADR still allows it. */
      /* BEWARE: the program counter adjustment below needed to be RESET, */
      /* otherwise it was sending you ONE AFTER your desired jump address! */
    } else { pc++; } /* without this, it gets stuck! */
    /* This pc adjustment above makes jumps NOT return to immediate mode: */
    /* pc is set to, essentially, an integer indicated by the float value */
    /* at an address. But a return to immediate mode is only possible if */
    /* pc stays 0. This is why a jump can serve as an "entry" to exection. */

  } else if (opr == 2) {
  /* IADR -- INDIRECT ADDRESSING: */
  /* SUBSTITUTE datadr1 AND datadr2 IN THE NEXT INSTRUCTION BY h AND k. */
  /* Whatever the next operation is, datum[datadr1] shall give */
  /* the next datadr1, and datum[datadr2] shall give the next datadr2. */
  /* This shall serve for a rather powerful "indirect addressing" */
  /* whenever such is needed, in a flexible manner. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    /* Thereby, h & k are "charged" and will trigger a modification of */
    /* the next instruction, substituting its addresses one time. */
    /* You can obviously cancel this with a NOP. */

    /* Make sure the addresses are within the allowed address range. */
    if (h < 1) {
      h = 1;
    }

    if (k < 1) {
      k = 1;
    }

    if (h >= DATAMEM) {
      h = DATAMEM - 1;
    }

    if (k >= DATAMEM) {
      k = DATAMEM - 1;
    }

  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */
  } else if (opr == 3) {
  /* OUTP -- write 0 for OUTPUT if you have a numeric display. */
  /* This instruction will print all data in a range, unless the second */
  /* address is seither the same as the first address or 0, indicating */
  /* that only the first address is to be printed. */

    System.out.print("DATA RANGE ");
    System.out.print(datadr1);
    System.out.print(" TO ");
    System.out.print(datadr2);
    System.out.println();
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
      f = datum[datadr1];
      System.out.print("OUTPUT: ADR=");
      if (datadr1 < 1000) { System.out.print("0"); } /* "leading zeroes" */
      if (datadr1 < 100) { System.out.print("0"); }
      if (datadr1 < 10) { System.out.print("0"); }
      System.out.print(datadr1);
      System.out.print(" DATUM=");
      System.out.print(f);
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
      System.out.println();

    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        f = datum[i];
        System.out.print("OUTPUT: ADR=");
        if (i < 1000) { System.out.print("0"); } /* "leading zeroes" */
        if (i < 100) { System.out.print("0"); }
        if (i < 10) { System.out.print("0"); }
        System.out.print(i);
        System.out.print(" DATUM=");
        System.out.print(f);
        /* scientific, big E, sign */
        /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
        System.out.println();
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        f = datum[i];
        System.out.print("OUTPUT: ADR=");
        if (i < 1000) { System.out.print("0"); } /* "leading zeroes" */
        if (i < 100) { System.out.print("0"); }
        if (i < 10) { System.out.print("0"); }
        System.out.print(i);
        System.out.print(" DATUM=");
        System.out.print(f);
        /* scientific, big E, sign */
        /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
        System.out.println();
      }
    }

  } else if (opr == 4) {
  /* INPT -- write 1 for INPUT if you have a numeric display. */
  /* This instruction will read a range of numbers, unless */
  /* the second address is the same as the first address or 0, */
  /* indicating that only a single number is to be read. */

    System.out.print("DATA INPUT INTO RANGE ");
    System.out.print(datadr1);
    System.out.print(" TO ");
    System.out.print(datadr2);
    System.out.println();
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
      System.out.print("INPUT:  ADR=");
      System.out.print(datadr1);
      System.out.print(" DATUM=");

      f = Inpt.nextFloat();
      
      datum[datadr1] = f;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        System.out.print("INPUT:  ADR=");
        System.out.print(i);
        System.out.print(" DATUM=");
        
        f = Inpt.nextFloat();
        
        datum[i] = f;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        System.out.print("INPUT:  ADR=");
        System.out.print(i);
        System.out.print(" DATUM=");
        
        f = Inpt.nextFloat();
        
        datum[i] = f;
      }
    }

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */

    datum[datadr1] = 0.0f + datadr2;
    /* obviously, if datadr2 == datadr1, this will make an address */
    /* "hold itself" */

  } else if (opr == 6) {
  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
  /* Did not call it EQUL or thelike, to help visually differentiate */
  /* values and addresses. */
    datum[datadr1] = datum[datadr2];

  } else if (opr == 7) {
  /* HOW IxAS WORKS: */
  /* datadr1 contains an address X, i.e. it points to it; */
  /* X is at first expressed as a float to be converted to an integer. */
  /* datum[X] shall either be set to an address or a value that is defined */
  /* in datadr2, i.e. datadr2 itself or the datum at datadr2, respectively. */
  /* In other words, datum[datum[datadr1]] = datadr2, from now on */
  /* Indirect Address Assignment or IAAS, or Indirect Value Assignemnt */
  /* IVAS signifying datum[datum[datadr1]] = datum[datadr2]. These may look */
  /* pretty strange, but they appeared practical for certain loops. */ 

  /* IAAS */
    i = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    datum[i] = 0.0f + datadr2;

  } else if (opr == 8) {
  /* IVAS -- see above. */

    i = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    datum[i] = datum[datadr2];

  /* ---- FROM NOW ON FOLLOW COMMANDS FOR SINGLE NUMERIC MANIPULATION ----- */
  } else if (opr == 9) {
  /* PLUS -- PLUS FOR SINGLE NUMBERS, NOT RANGE. */

    datum[datadr1] = datum[datadr1] + datum[datadr2];

  } else if (opr == 10) {
  /* MINS -- MINUS FOR SINGLE NUMBERS, NOT RANGE. */

    datum[datadr1] = datum[datadr1] - datum[datadr2];

  } else if (opr == 11) {
  /* MULS -- MULTIPLY SINGLE NUMBERS, NOT RANGE. */

    datum[datadr1] = datum[datadr1] * datum[datadr2];

  } else if (opr == 12) {
  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
  /* This makes loops much easier than "having to look out for the */
  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */

    if (java.lang.Math.abs(datum[datadr2]) != 0.0f) {
      datum[datadr1] = datum[datadr1] / datum[datadr2];
    } else {
      datum[datadr1] = 0.0f;
    }

  } else if (opr == 13) {
  /* POXY -- Power Of X: Y, that is, X ^ Y, BASED ON |X| WITH SIGN EX POST. */
  /* This is a rather powerful instruction, because 1/Y is the Yth root */
  /* (so Y = 0.5 is a square root, Y= 0.3333 is a cube root, and so on),
  /* Y = -1 sets X to 1/X, and so forth. - All this operates on the */
  /* ABSOLUTE VALUE OF X, and preserving the sign, to prevent imaginary */
  /* number results. Unfortunately, this has the funny effect that -3^2=-9. */

    /* A ^ B =  E ^ (B * ln(A)), but that is not even necessary due to pow. */
    if (datum[datadr1] == 0.0f) {
      datum[datadr1] = 0.0f;
      /* Wanton value because it is undefined for negative exponents */
      /* and for positive exponents, 0^whatever=0. */
    } else if (datum[datadr1] < 0.0f) {
      datum[datadr1] = -1.0f * 
      (float) 
      java.lang.Math.pow(java.lang.Math.abs(datum[datadr1]), datum[datadr2]);
    } else {
      datum[datadr1] = 
      (float) java.lang.Math.pow(datum[datadr1], datum[datadr2]);
    }

  } else if (opr == 14) {
  /* LOXY -- LogY X, i.e. of X to the base of Y. X needs not be an integer. */
  /* This is based on the effect of loga (b) = ln (b) / ln (a), */
  /* b is in datadr1, a is in datadr2, and both are forced to be positive. */
  /* If any of the two numbers is 0.0, the result is set to be 0.0, too. */
    if ((java.lang.Math.abs(datum[datadr1]) != 0) && 
        (java.lang.Math.abs(datum[datadr2]) != 0)) {
      datum[datadr1] =
        (float) java.lang.Math.log(java.lang.Math.abs(datum[datadr1])) / 
        (float) java.lang.Math.log(java.lang.Math.abs(datum[datadr2]));
    } else {
      datum[datadr1] = 0.0f;
    }

  } else if (opr == 15) {
  /* IFRA -- Integral and Fractional part of a number stored in datadr1. */

    g = datum[datadr1];
    datum[datadr2] = g % 1; /* the fractional part */
    datum[datadr1] = g - datum[datadr1]; /* the integral part */

  } else if (opr == 16) {
  /* REMN -- Remainer of the division between datum[datadr1] and */
  /* datum[datadr2], whereby 0 is assumed in case of division by 0. */
    if (java.lang.Math.abs(datum[datadr2]) != 0.0f) {
      datum[datadr1] = datum[datadr1] % datum[datadr2];
    } else {
      datum[datadr1] = 0.0f;
    }

  } else if (opr == 17) {
  /* NEXT THREE INSTRUCTIONS: PERCENT CALCULATIONS OVER TIME. */
  /* Each instruction is named for what is SOUGHT: */
  /* amount, period or percentage, assuming that */
  /* amount = (1 + (percentage / 100)) ^ period. */

  /* AMNT -- Find the amount, whereby */
  /* datadr1 contains the periods */
  /* datadr2 contains the percent (not as, say, 0.25, but as 25). */

    f = datum[datadr1];
    g = datum[datadr2];
    g = (float) java.lang.Math.pow(1 + (g / 100), f);
    datum[datadr1] = g;
    /* you overwrite the period, but keep the percent untouched */
    f = 0.0f;
    g = 0.0f;

  } else if (opr == 18) {
  /* PERD -- Find the period, whereby */
  /* datadr1 contains the final amount */
  /* datadr2 contains the percent. */

    f = datum[datadr1];
    g = datum[datadr2];
    g = (1 + (g / 100));
    g = (float) java.lang.Math.log(g);
    f = (float) java.lang.Math.log(f);
    g = f / g;
    datum[datadr1] = g;
    /* you overwrite the final amount, but keep the percent untouched */
    f = 0.0f;
    g = 0.0f;

  } else if (opr == 19) {
  /* PCNT -- Find the percent, whereby */
  /* datadr1 contains the final amount */
  /* datadr2 contains the period */

    f = datum[datadr1];
    g = datum[datadr2];
    g = 1/g;
    g = 100 * ((float) java.lang.Math.pow(f, g) - 1);
    datum[datadr1] = g;
    /* you overwrite the final amount, but keep the period untouched */
    f = 0.0f;
    g = 0.0f;

  } else if (opr == 20) {
  /* SWAP -- Swap two numbers. While trivial, it is annoying to novices. */
  /* Mentally, you have to consider a third number or an idiotic trick, */
  /* none of which is beneficial to "not getting interrupted" and focusing. */
  /* So there is now an extra instruction for that. */

    f = datum[datadr2];
    datum[datadr2] = datum[datadr1];
    datum[datadr1] = f;

  } else if (opr == 21) {
  /* FACT -- Factorial of the SECOND number, i.e. of datum[datadr2]. */
  /* The factorial of the second number ends up in the FIRST location. */
  /* This is not a "self-modifying-data" command, because the factorial */
  /* easily gets out of range, in which case the program gives zero. */
  /* Force numbers to be positive. */

    j = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if (j < 2) {
      datum[datadr1] = 1.0f;
    } else if (j > 34) {
      datum[datadr1] = 0.0f; /* signalling you messed it up */
    } else {
      f = 1.0f;
      for (i = 1; i <= j; i++) {
        f = f * i;
      }
      datum[datadr1] = f;
    }

  /* ---- FROM NOW ON FOLLOW COMMANDS FOR NUMERIC RANGE MANIPULATION ------ */
  /* Many of these actually have single-number application facilities, too. */

  } else if (opr == 22) {
  /* COPY -- COPY ADDRESS RANGE: */
  /* Copy INTO the range between datadr1 and datadr2, both inclusive, */
  /* the other range whose beginning and ending addresses are indicated */
  /* upon call within the address datadr1 and the address datadr2. */
  /* Clearly, this overwrites, too, the addresses originally stored in */
  /* datadr1 and datadr2 at the call of the instruction. */
  /* If the ranges are of unequal length, copy revolvingly or partially. */
  /* Normally, first the low address, then the high addresses should be */
  /* given - otherwise, assume a reversal of the range is desired. */

  /* val is the source, adr is the destination - and will be OVERWRITTEN. */
  /* Hence parameters can be placed under the destination-range */

  /* adr1(val1) (low) adr2(val2) (high) is the "default" */

    /* Determine the SOURCE range h & k that shall be copied into */
    /* datadr1 and datadr2. (You CAN operate here with h & k, because IADR */
    /* would have taken effect ALREADY, should it have been issued. */
    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));

    /* Keep them within the allowed range. */
    if (h >= DATAMEM) {
      h = DATAMEM - 1;
    }
    if (k >= DATAMEM) {
      k = DATAMEM - 1;
    }

    j = h;
    if (datadr2 == datadr1) {
      /* Then only one number is to be copied. */
      datum[datadr1] = datum[h];
      /* i.e. datum[datadr1] = datum[datum[datadr1]] */
    } else if (h == k) { /* then fill: */
      /* You can use this to initialise a range to a value. */
      f = datum[h];
      if (datadr2 > datadr1) {
        for (i = datadr1; i <= datadr2; i++) {
          datum[i] = f;
        }
      } else {
        for (i = datadr2; i <= datadr1; i++) {
          datum[i] = f;
        }
      }
    } else if ((datadr2 > datadr1) && (k > h)) {
      /* The "normal" case - make it revolving instead of "cutting" it. */
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[j];
        j++;
        if (j > k) {
          j = h;
        }
      }
    } else if ((datadr2 < datadr1) && (k < h))  {
      /* The countdown case - here, the revolutions may fill differently. */
      for (i = datadr1; i >= datadr2; i--) {
        datum[i] = datum[j];
        j--;
        if (j < k) {
          j = h;
        }
      }
    } else if ((datadr2 < datadr1) && (k > h)) {
      /* this and the next will "reverse" a range of equal size */
      /* they really only differ in how they would handle revolutions */
      for (i = datadr1; i >= datadr2; i--) {
        datum[i] = datum[j];
        j++;
        if (j > k) {
          j = h;
        }
      }
    } else if ((datadr2 > datadr1) && (k < h)) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[j];
        j--;
        if (j < k) {
          j = h;
        }
      }

    }
    h = 0;
    k = 0;

  } else if (opr == 23) {
  /* FRIS -- FILL RANGE FROM INITIAL-VALUE BY STEP */
  /* This is like looping a[n + 1] = a[n] + x.*/
  /* datadr1 and datadr2 describe the range to be filled, */
  /* datadr1 holds the initial value and datadr2 holds the step. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
      /* Do nothing - datadr1 has the init value, done!  */
    } else if (datadr1 < datadr2) {
      f = datum[datadr2];
      for (i = datadr1 + 1; i <= datadr2; i++) {
      /* datadr1 + 1 as datadr1 already contains the initial value */
        datum[i] = datum[i - 1] + f;
      }
    } else if (datadr1 > datadr2) { /* then do a count-down */
      f = datum[datadr2];
      for (i = datadr1 - 1; i >= datadr2; i--) {
        datum[i] = datum[i + 1] - f;
      }
    }
    /* Giving a step-value of zero, this of course will just copy */
    /* the initial value onto an entire range. An initial value of zero */
    /* and a step of zero can be thus used to erase a memory section. */

  } else if (opr == 24) {
  /* MNMX -- "Minimax", determine the minimum and the maximum value in a */
  /* range. Again adr1(adrA) adr2(adrB), where adr1 & adr2 contain */
  /* the addresses of the range, signified by adrA and adrB, and afterwards */
  /* adr1 and adr2 contain minimum and maximum numbers of said range. */
  /* This variant takes sign into account - make the range positive if you */
  /* actually care about the largest and smallest absolute values. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    f = datum[h];
    g = datum[h]; /* Not a mistake - you start with the SAME number. */
    if (h < k) {
      for (i = h + 1; i <= k; i++) { /* after the first number */
        if (f < datum[i]) {
          f = datum[i];
        }
        if (g > datum[i]) {
          g = datum[i];
        }
      }
    } else if (h > k) {
      for (i = k; i < h; i++) { /* before the last number */
        if (f < datum[i]) {
          f = datum[i];
        }
        if (g > datum[i]) {
          g = datum[i];
        }
      }
    }
    datum[datadr1] = f; /* maximum */
    datum[datadr2] = g; /* minimum */

    h = 0;
    k = 0;

  } else if (opr == 25) {
  /* SORT -- SORT RANGE */
  /* This is pretty naive and sacrifices run time to save memory. */
  /* Basically, each time when numbers are "unsorted", swap them, */
  /* and run through the whole array until no more swaps are done. */
  /* If nothing is left to swap - then necessarily all must be in order. */
  /* I am retaining this function despite the more interesting "coupled */
  /* sort" below, because it is simply quite easy to use. */

    if (datadr1 == datadr2) {
    /* Do nothing at all. */
    } else if (datadr2 == (datadr1 + 1)) {
      /* If only two numbers are given - either swap once, or not at all. */
      if (datum[datadr1] > datum[datadr2]) {
        /* The next three operations are the swap. */
        f = datum[datadr2];
        datum[datadr2] = datum[datadr1];
        datum[datadr1] = f;
      }
    } else if (datadr1 == (datadr2 + 1)) {
      if (datum[datadr1] < datum[datadr2]) {
        f = datum[datadr2];
        datum[datadr2] = datum[datadr1];
        datum[datadr1] = f;
      }
    } else if (datadr2 > datadr1) {
      k = 1; /* flag: Do we have to sort? k = 1 means "yes". */
      while (k == 1) {
        k = 0; /* Try to say you are done, and see is this contradicted. */
        for (i = datadr1; i < datadr2; i++) {
          if (datum[i] > datum[i + 1]) {
            /* Then swap, so the lower address contains the lower value. */
            f = datum[i + 1];
            datum[i + 1] = datum[i];
            datum[i] = f;
            k = 1; /* Still work to do - contradicting k = 0 above. */
          }
        }
      }
    } else if (datadr2 < datadr1) {
      k = 1; /* flag: Do we have to sort? */
      while (k == 1) {
        k = 0; /* Try to say you are done. */
        for (i = datadr2; i < datadr1; i++) {
          if (datum[i] < datum[i + 1]) {
            f = datum[i + 1];
            datum[i + 1] = datum[i];
            datum[i] = f;
            k = 1; /* Still work to do. */
          }
        }
      }
    }

  } else if (opr == 26) {
  /* CORS - COUPLED RANGE SORTING: The first range, starting from */
  /* datadr1 + 1 and continuing to datum[datadr1], both inclusive, */
  /* determines how datadr2 + 1 till datum[datadr2] is sorted. */
  /* datadr1 is used as the "sorting index" for datadr2. */
  /* This is very much like the general sorting routine. */
  /* What this is for: to sort something "according to" something else. */
  /* This should help solve "best"-value-tasks. */
  /* If datadr2 is determined to be 0, then CORS really works like SORT, */
  /* sorting the first range datum[datadr1 + 1] ... datum[datum[datadr1]]. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    /* Only datum[datadr1] deterines range. */
    /* k is used for flagging only, and not for range determination, and */
    /* instead it is assumed that the range in datadr2 is of the same */
    /* nature as the range signified by datadr1; in other words, */
    /* datum[datadr2] is simply ignored. This is not a disadvantage - this */
    /* way, a range can be secondarily sorted that begins at datum[1]. */

    if ((datadr1 == h) || (datadr1 == h + 1) || (datadr1 == h - 1)) {
    /* DO NOTHING AT ALL 0 or 1 element "range" is not to be "sorted". */
    } else if (datadr1 == h - 2) {

      if (datum[datadr1 + 1] > datum[datadr1 + 2]) {
        f = datum[datadr1 + 2];
        datum[datadr1 + 2] = datum[datadr1 + 1];
        datum[datadr1 + 1] = f;
        /* Do the same thing in the other range */
        if (datadr2 != 0) {
          f = datum[datadr2 + 2];
          datum[datadr2 + 2] = datum[datadr2 + 1];
          datum[datadr2 + 1] = f;
        }
      }

    } else if (datadr1 == h + 2) {

      if (datum[datadr1 - 2] > datum[datadr1 - 1]) {
        f = datum[datadr1 - 2];
        datum[datadr1 - 2] = datum[datadr1 - 1];
        datum[datadr1 - 1] = f;
        /* Do the same thing in the other range */
        if (datadr2 != 0) {
          f = datum[datadr2 + 2];
          datum[datadr2 + 2] = datum[datadr2 + 1];
          datum[datadr2 + 1] = f;
        }
      }

    } else if (datadr1 + 1 < h) {
      k = 1; /* flag: do we have to sort */
      while (k == 1) {
        k = 0; /* try to say you are done */
        for (i = datadr1 + 1; i < h; i++) {
          if (datum[i] > datum[i + 1]) {
            f = datum[i + 1];
            datum[i + 1] = datum[i];
            datum[i] = f;
            k = 1; /* still work to do */
            if (datadr2 != 0) {
              j = i - datadr1; /* determine how the offset from datadr1 ... */
              j = j + datadr2; /* ... and apply it to datadr2 ... */
             /* ... using it to do the same thing in the other range */
              f = datum[j + 1];
              datum[j + 1] = datum[j];
              datum[j] = f;
            }
          }
        }
      }
    } else if (datadr1 + 1 > h) {
      k = 1; /* flag: do we have to sort */
      while (k == 1) {
        k = 0; /* try to say you are done */
        for (i = h + 1; i < datadr1; i++) {
          if (datum[i] < datum[i + 1]) {
            f = datum[i + 1];
            datum[i + 1] = datum[i];
            datum[i] = f;
            k = 1; /* still work to do */
            if (datadr2 != 0) {
              j = i - datadr1; /* determine how the offset from datadr1 ... */
              j = j + datadr2; /* ... and apply it to datadr2 ... */
             /* ... using it to do the same thing in the other range */
              f = datum[j + 1];
              datum[j + 1] = datum[j];
              datum[j] = f;
            }
          }
        }
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 27) {
  /* TURN -- TURN A RANGE UPSIDE DOWN. */
  /* If in the range the numbers were 1.0 3.0 -3.8 2.1, make them be */
  /* instead 2.1 -3.8 3.0 1.0. The range is assumed between datadr1 and */
  /* datadr2, both inclusive. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        /* do nothing */
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= (datadr1 + ((datadr2 - datadr1) / 2)); i++) {
        f = datum[i];
        datum[i] = datum[datadr2 - (i - datadr1)];
        datum[datadr2 - (i - datadr1)] = f;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= (datadr2 + ((datadr1 - datadr2) / 2)); i++) {
        f = datum[i];
        datum[i] = datum[datadr1 - (i - datadr2)];
        datum[datadr1 - (i - datadr2)] = f;
      }
    }

  } else if (opr == 28) {
  /* SUMR -- SUM OF ADDRESS RANGE - RESULT ALWAYS IN FIRST ADDRESS. */
  /* Sum up the range signified by datum[datum[datadr1]] and */
  /* datum[datum[datadr2]], and place the result into datum[datadr1]. */
    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == k) || (k == 0)) { /* nothing to sum up if only one number */
        datum[datadr1] = datum[h];
    } else if (h < k) {
      datum[datadr1] = 0.0f;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    } else if (h > k) {
      datum[datadr1] = 0.0f;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    }
    h = 0;
    k = 0;

  } else if (opr == 29) {
  /* SUSQ -- SUM OF SQUARES AND SQUARE ROOTED SUM OF SQUARES */
  /* For vectors, statistics,... The range ADDRESSES are held in */
  /* datadr1 and datadr2, i.e. this is again indirect, and after execution, */
  /* datadr1 has the sum of squares and datadr2 has the square root of it. */
  /* Only anything to do if the addresses really are in a RANGE: */
    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == k) || (k == 0)) {
        datum[datadr1] = java.lang.Math.abs(datum[h]);
        /* (float) java.lang.Math.sqrt(x^2) = java.lang.Math.abs(x) */
    } else if (h < k) {
      datum[datadr1] = 0.0f;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
      }
    } else if (h > k) {
      datum[datadr1] = 0.0f;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
      }
    }
      datum[datadr2] = (float) java.lang.Math.sqrt(datum[datadr1]);
    h = 0;
    k = 0;

  } else if (opr == 30) {
  /* The next commands focus on a SINGLE range - and often have */
  /* meaning even if used on a single number, by setting datadr2 to */
  /* the same value as datadr1 or to zero (which is shorthand for that). */
  /* This is in contrast to actual range-range-commands, which */
  /* operate on TWO ranges, not on ONE range. */

  /* IXTH -- turn each X into 1/Xth in a range. X=0 gives 0, as always.  */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. */
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        if (datum[datadr1] != 0.0f) {
          datum[datadr1] = 1.0f/datum[datadr1];
        } else {
          datum[datadr1] = 0.0f;
        }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if (datum[i] != 0.0f) {
          datum[i] = 1.0f/datum[i];
        } else {
          datum[i] = 0.0f;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if (datum[i] != 0.0f) {
          datum[i] = 1.0f/datum[i];
        } else {
          datum[i] = 0.0f;
        }
      }
    }

  } else if (opr == 31) {
  /* ABSR -- ABSOLUTE VALUE OF RANGE. */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = java.lang.Math.abs(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = java.lang.Math.abs(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = java.lang.Math.abs(datum[i]);
      }
    }

  } else if (opr == 32) {
  /* SQRT -- SQUARE ROOT ABSOLUTES IN RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] =
        (float) java.lang.Math.sqrt(java.lang.Math.abs(datum[datadr1]));
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.sqrt(java.lang.Math.abs(datum[i]));
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.sqrt(java.lang.Math.abs(datum[i]));
      }
    }

  } else if (opr == 33) {
  /* SQUA -- SQUARE RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * datum[datadr1];
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * datum[i];
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * datum[i];
      }
    }

  } else if (opr == 34) {
  /* CBRT -- CUBE ROOT IN RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    f = 1.0f/3.0f;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = (float) java.lang.Math.pow(datum[datadr1], f);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.pow(datum[i], f);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.pow(datum[i], f);
      }
    }

  } else if (opr == 35) {
  /* CUBE RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * datum[datadr1] * datum[datadr1];
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * datum[i] * datum[i];
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * datum[i] * datum[i];
      }
    }

  } else if (opr == 36) {
  /* LNRN -- NATURAL LOGARITHM OF RANGE. */
  /* The numbers are forced into being positive, and a logarithm of zero */
  /* gives zero. */

  /* You can obtain any logarithm or power you like using formulae such as */
  /* A ^ B =  X ^ (B * logX (A)) as well as logA (B) = logX (B) / logX (A). */
  /* Obviously, that ominous "X" can also be "e". */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        if (datum[datadr1] != 0.0f) {
          datum[datadr1] = 
          (float) java.lang.Math.log(java.lang.Math.abs(datum[datadr1]));
        } else {
          datum[datadr1] = 0.0f;
        }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if (datum[i] != 0.0f) {
          datum[i] = (float) java.lang.Math.log(java.lang.Math.abs(datum[i]));
        } else {
          datum[i] = 0.0f;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if (datum[i] != 0.0f) {
          datum[i] = (float) java.lang.Math.log(java.lang.Math.abs(datum[i]));
        } else {
          datum[i] = 0.0f;
        }
      }
    }

  } else if (opr == 37) {
  /* EXPR -- e^X of range. */

  /* You can obtain any logarithm or power you like using formulae such as */
  /* A ^ B =  X ^ (B * logX (A)) as well as logA (B) = logX (B) / logX (A). */
  /* Obviously, that ominous "X" can also be "e". See above. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = (float) java.lang.Math.exp(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.exp(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.exp(datum[i]);
      }
    }

  } else if (opr == 38) {
  /* Now follow a few trigonometric functions. It my experience, */
  /* in the vast majority of cases when you are dealing with */
  /* trigonometric functions, practically you are dealing with degrees, */
  /* not radian. Hence, these functions are all oriented to degrees. */
  /* If, for any reason, a conversion between radian and degrees becomes */
  /* necessary, this function and the next one will take care of it. */

  /* RADE -- CONVERT RANGE FROM RADIANS TO DEGREES */
  /* degrees = radians * 180 / pi ... radians = degrees * pi / 180 */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * 180 / PI;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * 180 / PI;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * 180 / PI;
      }
    }

  } else if (opr == 39) {
  /* DERA -- CONVERT RANGE FROM DEGREES TO RADIANS */
  /* degrees = radians * 180 / pi ... radians = degrees * pi / 180 */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * PI / 180;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * PI / 180;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * PI / 180;
      }
    }

  } else if (opr == 40) {
  /* SIND -- Sinus of degrees in a range. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * PI / 180;
        datum[datadr1] = (float) java.lang.Math.sin(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = (float) java.lang.Math.sin(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = (float) java.lang.Math.sin(datum[i]);
      }
    }

  } else if (opr == 41) {
  /* COSD -- Cosinus of degrees in a range.  */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * PI / 180;
        datum[datadr1] = (float) java.lang.Math.cos(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = (float) java.lang.Math.cos(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = (float) java.lang.Math.cos(datum[i]);
      }
    }

  } else if (opr == 42) {
  /* TAND -- Tangens of degrees in a range.  */
  /* The cotangens you find simply by turning this to 1/X. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
      datum[datadr1] = datum[datadr1] * PI / 180;
      if ((float) java.lang.Math.cos(datum[datadr1]) != 0.0f) {
        datum[datadr1] = (float) java.lang.Math.sin(datum[datadr1])/
                         (float) java.lang.Math.cos(datum[datadr1]);
      } else {
        datum[datadr1] = 0.0f;
      }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if ((float) java.lang.Math.cos(datum[i]) != 0.0f) {
          datum[i] = datum[i] * PI / 180;
          datum[i] = (float) java.lang.Math.sin(datum[i])/
                     (float) java.lang.Math.cos(datum[i]);
        } else {
          datum[i] = 0.0f;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if ((float) java.lang.Math.cos(datum[i]) != 0.0f) {
          datum[i] = datum[i] * PI / 180;
          datum[i] = (float) java.lang.Math.sin(datum[i])/
                     (float) java.lang.Math.cos(datum[i]);
        } else {
          datum[i] = 0.0f;
        }
      }
    }

  } else if (opr == 43) {
  /* ASND -- Arcussinus of a range, giving degrees.  */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] =
        (float) java.lang.Math.asin((datum[datadr1]) * 180.0 / PI);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.asin((datum[i]) * 180.0 / PI);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.asin((datum[i]) * 180.0 / PI);
      }
    }

  } else if (opr == 44) {
  /* ACSD -- Arcuscosinus of a range, giving degrees. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = 
        (float) java.lang.Math.acos((datum[datadr1]) * 180.0 / PI);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.acos((datum[i]) * 180.0 / PI);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.acos((datum[i]) * 180.0 / PI);
      }
    }

  } else if (opr == 45) {
  /* ATND -- Arcustangens of a range, giving degrees. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = 
        (float) java.lang.Math.atan((datum[datadr1]) * 180.0 / PI);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.atan((datum[i]) * 180.0 / PI);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.atan((datum[i]) * 180.0 / PI);
      }
    }

  } else if (opr == 46) {
  /* MSTD -- MEAN AND STANDARD DEVIATION ON A SAMPLE. */
  /* I.e. this is WITH "Bessel's correction" for the standard deviation. */
  /* A statistical function. datadr1 and datadr2 indicate a range, */
  /* datum[datum[datadr1]] till datum[datum[datadr2]]. After application, */
  /* the mean of the range is contained in datadr1 and the standard */
  /* deviation in datadr2. A Gaussian distribution is assumed. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    j = java.lang.Math.abs(h - k); /* how many cells minus 1*/

    if ((h == k) || (k == 0)) { /* "very funny" - no range */
      datum[datadr1] = datum[h];
    } else if (h < k) {
      datum[datadr1] = 0.0f;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    } else if (h > k) {
      datum[datadr1] = 0.0f;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    }

    datum[datadr1] = datum[datadr1]/(j + 1);

    if (datadr2 != datadr1) {
      if ((h == k) || (k == 0)) { /* "very funny" - no range */
        datum[datadr2] = 0;
        /* No standard deviation can be given. */
      } else if (h < k) {
        datum[datadr2] = 0.0f;
        for (i = h; i <= k; i++) {
          datum[datadr2] = datum[datadr2]
            + ((datum[i] - datum[datadr1]) * (datum[i] - datum[datadr1]));
        }
      } else if (h > k) {
        datum[datadr2] = 0.0f;
        for (i = k; i <= h; i++) {
          datum[datadr2] = datum[datadr2]
            + ((datum[i] - datum[datadr1]) * (datum[i] - datum[datadr1]));
        }
      }

      datum[datadr2] = (float) java.lang.Math.sqrt(datum[datadr2]/j);
    }
    h = 0;
    k = 0;

  } else if (opr == 47) {
  /* ZERO -- ZERO OUT A DATA RANGE. */
  /* While there are numerous ways to do it, this will be the most obvious */
  /* choice for the user. - As there is no provision for changing */
  /* instructions in a running program, there is no corresponding memory */
  /* clearance for instructions; this must be done manually at the REPL. */
  /* NOTE THE SHORTHAND OF 0 0 TO CLEAR ALL MEMORY. This may be useful in */
  /* order to clear residual values or begin a calculation anew, e.g. */
  /* asking the user for input. */

    if ((datadr1 == 0) && (datadr2 == 0)) {
    /* If BOTH numbers are zero, not just datadr2 but also datadr1, */
    /* then see this as shorthand for "CLEAR ALL MEMORY". */
      for (i = 1 ; i < DATAMEM ; i++) {
        datum[i] = 0.0f;
      }
    } else if ((datadr1 == datadr2) || (datadr2 == 0)) {
    /* If JUST datadr2 is 0, see it as shorthand to just clear datadr1. */
        datum[datadr1] = 0.0f;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) { 
        datum[i] = 0.0f;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) { 
        datum[i] = 0.0f;
      }
    }

  } else if (opr == 48) {
  /* RAND -- GENERATE RANDOM NUMBERS. */
  /* Whereby datadr1 contains the minimum allowed number and datadr2 */
  /* contains the maximum allowed number of the range. The numbers are then */
  /* generated within that range, eventually also overwriting the minimum */
  /* and the maximum values with random numbers within the desired limits. */

    f = datum[datadr1]; /* MIN */
    g = datum[datadr2]; /* MAX */
    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = (float) java.lang.Math.random() * f;
        /* i.e. if there is no "range", one random number between 0 and f. */
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = f + (float) java.lang.Math.random() * (g - f);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = f + (float) java.lang.Math.random() * (g - f);
      }
    }

  } else if (opr == 49) {
  /* RUND -- java.lang.Math.round RANGE. */

  /* This and the following two functions attempt to turn floats into */
  /* integers. Again, they can be applied to a single number or a range. */

    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = java.lang.Math.round(datum[i]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = java.lang.Math.round(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = java.lang.Math.round(datum[i]);
      }
    }

  } else if (opr == 50) {
  /* CEIL -- CEILING RANGE */

    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = (float) java.lang.Math.ceil(datum[i]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.ceil(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.ceil(datum[i]);
      }
    }

  } else if (opr == 51) {
  /* TANH -- TANH OF RANGE */
  /* (float) java.lang.Math.tanh(x) = (e^x - e^(-x)) / (e^x + e^(-x)) */
  /* Just in case anybody does anything logistic or wants a neural network */
  /* to be programmed dynamically - this is a famous activation function. */

    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = (float) java.lang.Math.tanh(datum[i]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = (float) java.lang.Math.tanh(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = (float) java.lang.Math.tanh(datum[i]);
      }
    }

  } else if (opr == 52) {
  /* DTNH -- DERIVATIVE OF TANH */
  /* deriv(float) java.lang.Math.tanh(x) =    */
  /*  1 - (float) java.lang.Math.tanh(x)      */
  /*  * (float) java.lang.Math.tanh(x)        */
  /* This is usually used in conjunction with the above. */

    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = 1.0f - (float) java.lang.Math.tanh(datum[i]) * 
                          (float) java.lang.Math.tanh(datum[i]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = 1.0f - (float) java.lang.Math.tanh(datum[i]) * 
                          (float) java.lang.Math.tanh(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = 1.0f - (float) java.lang.Math.tanh(datum[i]) * 
                          (float) java.lang.Math.tanh(datum[i]);
      }
    }

  /* ---- FROM NOW ON FOLLOW RANGE-WITH-RANGE-OPERATIONS ------------------ */
  } else if (opr == 53) {
  /* RANGE-RANGE-COMMANDS (AND RANGE-NUMBER-COMMANDS): */

  /* Essentially, with ranges, do something for each corresponding position */
  /* in two ranges, and end when the shorter range has finished. - A number */
  /* to range application means to modify each position in a range by said */
  /* number in the manner specified by the respective operator. */

  /* The first address will be one BEFORE or the one AFTER the range, and */
  /* it will contain the pointer to the end (or beginning) of the */
  /* respective range. That is: A[-1] --> An ; B[-1] --> Bn. */
  /* The result will be in the first range. */
  /* To keep things simple, if the second address is NOT */
  /* higher or equal to the first address, simply DO NOTHING. */

  /* I was totally unsure shouldn't I use datum[0] as "accumulator" here, */
  /* but it would make tracing errors more difficult, as datum[0] would get */
  /* all the time overwritten. But e.g. supplying the two addresses and */
  /* have datum[0] supply the length would be totally feasible, */
  /* as an alternative - just then avoid "nuking" it with 0 all the time. */
  /* However, this would NOT make it easier for the targeted user base, */
  /* and "ease of use by the user" is a design priority: without this */
  /* permanent overwriting, the "accumulator" of each range may be printed */
  /* to see actually where you may have messed up, not as with datum[0]. */
  /* For the user, the advice is to leave at least one number BELOW and */
  /* at least one number ABOVE each interesting range unused, so as to */
  /* be able to use range commands (both top-down and bottom-up). */

  /* datum[datadr1 + 1] is the range begin */
  /* datum[h] is the range end; for k & datadr2, respectively */
  /* So each material range actually begins NOT at datadr1 and datadr2, but */
  /* at one address HIGHER than them. */

  /* PLUR -- ADD TWO RANGES. */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] + datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADDRESSES and cannot be used themselves */
      j = datadr2 - 1;
      for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
        datum[i] = datum[i] + datum[j];
        j--;
      }
    } else if ((h > datadr1) && (k < datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 - 1;
      for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
        datum[i] = datum[i] + datum[j];
        j--;
      }
    } else if ((h < datadr1) && (k > datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 + 1;
      for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
        datum[i] = datum[i] + datum[j];
        j++;
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 54) {
  /* MINR -- SUBTRACT A RANGE FROM A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] - datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADDRESSES and cannot be used themselves */
      j = datadr2 - 1;
      for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
        datum[i] = datum[i] - datum[j];
        j--;
      }
    } else if ((h > datadr1) && (k < datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 - 1;
      for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
        datum[i] = datum[i] - datum[j];
        j--;
      }
    } else if ((h < datadr1) && (k > datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 + 1;
      for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
        datum[i] = datum[i] - datum[j];
        j++;
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 55) {
  /* MULR -- MULTIPLY A RANGE WITH A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] * datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADDRESSES and cannot be used themselves */
      j = datadr2 - 1;
      for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
        datum[i] = datum[i] * datum[j];
        j--;
      }
    } else if ((h > datadr1) && (k < datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 - 1;
      for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
        datum[i] = datum[i] * datum[j];
        j--;
      }
    } else if ((h < datadr1) && (k > datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 + 1;
      for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
        datum[i] = datum[i] * datum[j];
        j++;
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 56) {
  /* DIVR -- DIVIDE A RANGE BY A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. A division by 0 nullifies the dividend, */
  /* as before on single-number-operations. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    k = java.lang.Math.abs(java.lang.Math.round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        if (datum[j] != 0.0f) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0f;
        }
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADDRESSES and cannot be used themselves */
      j = datadr2 - 1;
      for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
        if (datum[j] != 0.0f) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0f;
        }
        j--;
      }
    } else if ((h > datadr1) && (k < datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 - 1;
      for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
        if (datum[j] != 0.0f) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0f;
        }
        j--;
      }
    } else if ((h < datadr1) && (k > datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 + 1;
      for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
        if (datum[j] != 0.0f) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0f;
        }
        j++;
      }
    }

    h = 0;
    k = 0;

  /* ---- FROM NOW ON FOLLOW RANGE-WITH-SINGLE-NUMBER-OPERATIONS ---------- */
  } else if (opr == 57) {
  /* PLUN -- ADD A NUMBER TO A RANGE. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    if (h == datadr1) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if (h > datadr1) {
      for (i = datadr1 + 1; i <= h; i++) {
        datum[i] = datum[i] + datum[datadr2];
      }
    } else if (h < datadr1) {
      for (i = datadr1 - 1; i >= h; i--) {
        datum[i] = datum[i] + datum[datadr2];
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 58) {
  /* MINN -- SUBTRACT A NUMBER FROM A RANGE. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    if (h == datadr1) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if (h > datadr1) {
      for (i = datadr1 + 1; i <= h; i++) {
        datum[i] = datum[i] - datum[datadr2];
      }
    } else if (h < datadr1) {
      for (i = datadr1 - 1; i >= h; i--) {
        datum[i] = datum[i] - datum[datadr2];
      }
    }

    h = 0;
    k = 0;

  } else if (opr == 59) {
  /* MULN -- MULTIPLY A RANGE WITH A NUMBER. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    if (h == datadr1) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if (h > datadr1) {
      for (i = datadr1 + 1; i <= h; i++) {
        datum[i] = datum[i] * datum[datadr2];
      }
    } else if (h < datadr1) {
      for (i = datadr1 - 1; i >= h; i--) {
        datum[i] = datum[i] * datum[datadr2];
      }
    }

    h = 0;
    k = 0;


  } else if (opr == 60) {
  /* DIVN -- DIVIDE A RANGE BY A NUMBER. */
  /* A division by 0 nullifies the dividend, */
  /* as before on single-number-operations. */

    h = java.lang.Math.abs(java.lang.Math.round(datum[datadr1]));
    if (h == datadr1) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if (h > datadr1) {
      if (datum[datadr2] != 0.0f) {
        for (i = datadr1 + 1; i <= h; i++) {
          datum[i] = datum[i] / datum[datadr2];
        }
      } else {
        for (i = datadr1 + 1; i <= h; i++) {
          datum[i] = 0.0f;
        }
      }
    } else if (h < datadr1) {
      if (datum[datadr2] != 0.0f) {
        for (i = datadr1 - 1; i >= h; i--) {
          datum[i] = datum[i] / datum[datadr2];
        }
      } else {
        for (i = datadr1 - 1; i >= h; i--) {
          datum[i] = 0.0f;
        }
      }
    }

    h = 0;
    k = 0;

  /* ---- FROM NOW ON FOLLOW LOOK-UP TABLES FOR STATISTICAL PURPOSES ------ */
  /* The question to include them or not may be, trivially, flash space. */
  /* PHI Z LOOKUP TABLES - WORKING BOTH WAYS */
  /* FIRST KISS: java.lang.Math.round TO NEAREST VALUE, DO NOT INTERPOLATE. */
  } else if (opr == 61) {

  /* PROB */
  /* FOR A GIVEN STANDARD DEVIATION DISTANCE, FIND THE PROBABILITY COVERED. */
  /* You have a range defined by datadr1 and datadr2, */
  /* containing the deviations. */
  /* After calling the function, you have the same range filled with the */
  /* probabilities covered at each respective deviation. */

    if (datadr2 == 0) { datadr2 = datadr1; }

    /* normalise addresses */
    if (datadr1 > datadr2) { i = datadr2; datadr2 = datadr1; datadr1 = i; }

    
    for (i = datadr1; i <= datadr2; i++) {
      k = 32000; /* way more than greatest possible difference */
      h = 0; /* prepare below-0-flag */
      f = (float) datum[i];
      if (f < 0.0f) {h = 1;} /* flag adjustment */
      f = (float) java.lang.Math.abs(f); /* make the deviation positive */
  
      if (f > 3.90f) { datum[i] = 1.0f; }
      else {
    datum[i] = f;
    g = (float) 0.00; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5000; }
    g = (float) 0.01; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5040; }
    g = (float) 0.02; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5080; }
    g = (float) 0.03; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5120; }
    g = (float) 0.04; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5160; }
    g = (float) 0.05; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5199; }
    g = (float) 0.06; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5239; }
    g = (float) 0.07; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5279; }
    g = (float) 0.08; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5319; }
    g = (float) 0.09; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5359; }
    g = (float) 0.10; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5398; }
    g = (float) 0.11; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5438; }
    g = (float) 0.12; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5478; }
    g = (float) 0.13; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5517; }
    g = (float) 0.14; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5557; }
    g = (float) 0.15; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5596; }
    g = (float) 0.16; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5636; }
    g = (float) 0.17; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5675; }
    g = (float) 0.18; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5714; }
    g = (float) 0.19; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5753; }
    g = (float) 0.20; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5793; }
    g = (float) 0.21; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5832; }
    g = (float) 0.22; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5871; }
    g = (float) 0.23; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5910; }
    g = (float) 0.24; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5948; }
    g = (float) 0.25; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.5987; }
    g = (float) 0.26; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6026; }
    g = (float) 0.27; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6064; }
    g = (float) 0.28; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6103; }
    g = (float) 0.29; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6141; }
    g = (float) 0.30; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6179; }
    g = (float) 0.31; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6217; }
    g = (float) 0.32; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6255; }
    g = (float) 0.33; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6293; }
    g = (float) 0.34; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6331; }
    g = (float) 0.35; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6368; }
    g = (float) 0.36; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6406; }
    g = (float) 0.37; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6443; }
    g = (float) 0.38; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6480; }
    g = (float) 0.39; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6517; }
    g = (float) 0.40; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6554; }
    g = (float) 0.41; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6591; }
    g = (float) 0.42; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6628; }
    g = (float) 0.43; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6664; }
    g = (float) 0.44; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6700; }
    g = (float) 0.45; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6736; }
    g = (float) 0.46; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6772; }
    g = (float) 0.47; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6808; }
    g = (float) 0.48; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6844; }
    g = (float) 0.49; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6879; }
    g = (float) 0.50; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6915; }
    g = (float) 0.51; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6950; }
    g = (float) 0.52; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.6985; }
    g = (float) 0.53; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7019; }
    g = (float) 0.54; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7054; }
    g = (float) 0.55; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7088; }
    g = (float) 0.56; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7123; }
    g = (float) 0.57; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7157; }
    g = (float) 0.58; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7190; }
    g = (float) 0.59; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7224; }
    g = (float) 0.60; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7257; }
    g = (float) 0.61; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7291; }
    g = (float) 0.62; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7324; }
    g = (float) 0.63; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7357; }
    g = (float) 0.64; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7389; }
    g = (float) 0.65; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7422; }
    g = (float) 0.66; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7454; }
    g = (float) 0.67; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7486; }
    g = (float) 0.68; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7517; }
    g = (float) 0.69; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7549; }
    g = (float) 0.70; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7580; }
    g = (float) 0.71; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7611; }
    g = (float) 0.72; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7642; }
    g = (float) 0.73; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7673; }
    g = (float) 0.74; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7704; }
    g = (float) 0.75; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7734; }
    g = (float) 0.76; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7764; }
    g = (float) 0.77; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7794; }
    g = (float) 0.78; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7823; }
    g = (float) 0.79; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7852; }
    g = (float) 0.80; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7881; }
    g = (float) 0.81; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7910; }
    g = (float) 0.82; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7939; }
    g = (float) 0.83; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7967; }
    g = (float) 0.84; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.7995; }
    g = (float) 0.85; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8023; }
    g = (float) 0.86; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8051; }
    g = (float) 0.87; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8078; }
    g = (float) 0.88; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8106; }
    g = (float) 0.89; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8133; }
    g = (float) 0.90; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8159; }
    g = (float) 0.91; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8186; }
    g = (float) 0.92; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8212; }
    g = (float) 0.93; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8238; }
    g = (float) 0.94; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8264; }
    g = (float) 0.95; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8289; }
    g = (float) 0.96; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8315; }
    g = (float) 0.97; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8340; }
    g = (float) 0.98; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8365; }
    g = (float) 0.99; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8389; }
    g = (float) 1.00; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8413; }
    g = (float) 1.01; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8438; }
    g = (float) 1.02; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8461; }
    g = (float) 1.03; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8485; }
    g = (float) 1.04; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8508; }
    g = (float) 1.05; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8531; }
    g = (float) 1.06; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8554; }
    g = (float) 1.07; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8577; }
    g = (float) 1.08; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8599; }
    g = (float) 1.09; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8621; }
    g = (float) 1.10; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8643; }
    g = (float) 1.11; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8665; }
    g = (float) 1.12; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8686; }
    g = (float) 1.13; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8708; }
    g = (float) 1.14; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8729; }
    g = (float) 1.15; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8749; }
    g = (float) 1.16; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8770; }
    g = (float) 1.17; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8790; }
    g = (float) 1.18; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8810; }
    g = (float) 1.19; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8830; }
    g = (float) 1.20; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8849; }
    g = (float) 1.21; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8869; }
    g = (float) 1.22; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8888; }
    g = (float) 1.23; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8907; }
    g = (float) 1.24; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8925; }
    g = (float) 1.25; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8944; }
    g = (float) 1.26; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8962; }
    g = (float) 1.27; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8980; }
    g = (float) 1.28; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.8997; }
    g = (float) 1.29; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9015; }
    g = (float) 1.30; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9032; }
    g = (float) 1.31; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9049; }
    g = (float) 1.32; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9066; }
    g = (float) 1.33; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9082; }
    g = (float) 1.34; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9099; }
    g = (float) 1.35; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9115; }
    g = (float) 1.36; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9131; }
    g = (float) 1.37; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9147; }
    g = (float) 1.38; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9162; }
    g = (float) 1.39; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9177; }
    g = (float) 1.40; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9192; }
    g = (float) 1.41; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9207; }
    g = (float) 1.42; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9222; }
    g = (float) 1.43; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9236; }
    g = (float) 1.44; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9251; }
    g = (float) 1.45; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9265; }
    g = (float) 1.46; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9279; }
    g = (float) 1.47; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9292; }
    g = (float) 1.48; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9306; }
    g = (float) 1.49; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9319; }
    g = (float) 1.50; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9332; }
    g = (float) 1.51; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9345; }
    g = (float) 1.52; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9357; }
    g = (float) 1.53; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9370; }
    g = (float) 1.54; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9382; }
    g = (float) 1.55; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9394; }
    g = (float) 1.56; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9406; }
    g = (float) 1.57; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9418; }
    g = (float) 1.58; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9429; }
    g = (float) 1.59; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9441; }
    g = (float) 1.60; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9452; }
    g = (float) 1.61; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9463; }
    g = (float) 1.62; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9474; }
    g = (float) 1.63; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9484; }
    g = (float) 1.64; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9495; }
    g = (float) 1.65; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9505; }
    g = (float) 1.66; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9515; }
    g = (float) 1.67; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9525; }
    g = (float) 1.68; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9535; }
    g = (float) 1.69; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9545; }
    g = (float) 1.70; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9554; }
    g = (float) 1.71; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9564; }
    g = (float) 1.72; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9573; }
    g = (float) 1.73; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9582; }
    g = (float) 1.74; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9591; }
    g = (float) 1.75; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9599; }
    g = (float) 1.76; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9608; }
    g = (float) 1.77; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9616; }
    g = (float) 1.78; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9625; }
    g = (float) 1.79; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9633; }
    g = (float) 1.80; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9641; }
    g = (float) 1.81; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9649; }
    g = (float) 1.82; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9656; }
    g = (float) 1.83; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9664; }
    g = (float) 1.84; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9671; }
    g = (float) 1.85; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9678; }
    g = (float) 1.86; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9686; }
    g = (float) 1.87; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9693; }
    g = (float) 1.88; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9699; }
    g = (float) 1.89; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9706; }
    g = (float) 1.90; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9713; }
    g = (float) 1.91; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9719; }
    g = (float) 1.92; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9726; }
    g = (float) 1.93; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9732; }
    g = (float) 1.94; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9738; }
    g = (float) 1.95; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9744; }
    g = (float) 1.96; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9750; }
    g = (float) 1.97; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9756; }
    g = (float) 1.98; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9761; }
    g = (float) 1.99; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9767; }
    g = (float) 2.00; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9772; }
    g = (float) 2.01; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9778; }
    g = (float) 2.02; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9783; }
    g = (float) 2.03; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9788; }
    g = (float) 2.04; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9793; }
    g = (float) 2.05; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9798; }
    g = (float) 2.06; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9803; }
    g = (float) 2.07; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9808; }
    g = (float) 2.08; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9812; }
    g = (float) 2.09; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9817; }
    g = (float) 2.10; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9821; }
    g = (float) 2.11; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9826; }
    g = (float) 2.12; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9830; }
    g = (float) 2.13; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9834; }
    g = (float) 2.14; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9838; }
    g = (float) 2.15; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9842; }
    g = (float) 2.16; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9846; }
    g = (float) 2.17; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9850; }
    g = (float) 2.18; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9854; }
    g = (float) 2.19; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9857; }
    g = (float) 2.20; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9861; }
    g = (float) 2.21; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9864; }
    g = (float) 2.22; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9868; }
    g = (float) 2.23; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9871; }
    g = (float) 2.24; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9875; }
    g = (float) 2.25; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9878; }
    g = (float) 2.26; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9881; }
    g = (float) 2.27; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9884; }
    g = (float) 2.28; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9887; }
    g = (float) 2.29; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9890; }
    g = (float) 2.30; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9893; }
    g = (float) 2.31; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9896; }
    g = (float) 2.32; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9898; }
    g = (float) 2.33; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9901; }
    g = (float) 2.34; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9904; }
    g = (float) 2.35; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9906; }
    g = (float) 2.36; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9909; }
    g = (float) 2.37; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9911; }
    g = (float) 2.38; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9913; }
    g = (float) 2.39; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9916; }
    g = (float) 2.40; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9918; }
    g = (float) 2.41; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9920; }
    g = (float) 2.42; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9922; }
    g = (float) 2.43; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9925; }
    g = (float) 2.44; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9927; }
    g = (float) 2.45; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9929; }
    g = (float) 2.46; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9931; }
    g = (float) 2.47; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9932; }
    g = (float) 2.48; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9934; }
    g = (float) 2.49; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9936; }
    g = (float) 2.50; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9938; }
    g = (float) 2.51; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9940; }
    g = (float) 2.52; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9941; }
    g = (float) 2.53; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9943; }
    g = (float) 2.54; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9945; }
    g = (float) 2.55; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9946; }
    g = (float) 2.56; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9948; }
    g = (float) 2.57; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9949; }
    g = (float) 2.58; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9951; }
    g = (float) 2.59; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9952; }
    g = (float) 2.60; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9953; }
    g = (float) 2.61; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9955; }
    g = (float) 2.62; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9956; }
    g = (float) 2.63; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9957; }
    g = (float) 2.64; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9959; }
    g = (float) 2.65; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9960; }
    g = (float) 2.66; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9961; }
    g = (float) 2.67; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9962; }
    g = (float) 2.68; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9963; }
    g = (float) 2.69; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9964; }
    g = (float) 2.70; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9965; }
    g = (float) 2.71; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9966; }
    g = (float) 2.72; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9967; }
    g = (float) 2.73; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9968; }
    g = (float) 2.74; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9969; }
    g = (float) 2.75; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9970; }
    g = (float) 2.76; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9971; }
    g = (float) 2.77; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9972; }
    g = (float) 2.78; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9973; }
    g = (float) 2.79; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9974; }
    g = (float) 2.80; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9974; }
    g = (float) 2.81; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9975; }
    g = (float) 2.82; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9976; }
    g = (float) 2.83; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9977; }
    g = (float) 2.84; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9977; }
    g = (float) 2.85; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9978; }
    g = (float) 2.86; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9979; }
    g = (float) 2.87; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9979; }
    g = (float) 2.88; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9980; }
    g = (float) 2.89; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9981; }
    g = (float) 2.90; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9981; }
    g = (float) 2.91; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9982; }
    g = (float) 2.92; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9982; }
    g = (float) 2.93; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9983; }
    g = (float) 2.94; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9984; }
    g = (float) 2.95; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9984; }
    g = (float) 2.96; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9985; }
    g = (float) 2.97; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9985; }
    g = (float) 2.98; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9986; }
    g = (float) 2.99; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9986; }
    g = (float) 3.00; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9987; }
    g = (float) 3.01; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9987; }
    g = (float) 3.02; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9987; }
    g = (float) 3.03; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9988; }
    g = (float) 3.04; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9988; }
    g = (float) 3.05; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9989; }
    g = (float) 3.06; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9989; }
    g = (float) 3.07; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9989; }
    g = (float) 3.08; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9990; }
    g = (float) 3.09; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9990; }
    g = (float) 3.10; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9990; }
    g = (float) 3.11; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9991; }
    g = (float) 3.12; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9991; }
    g = (float) 3.13; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9991; }
    g = (float) 3.14; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9992; }
    g = (float) 3.15; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9992; }
    g = (float) 3.16; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9992; }
    g = (float) 3.17; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9992; }
    g = (float) 3.18; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9993; }
    g = (float) 3.19; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9993; }
    g = (float) 3.20; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9993; }
    g = (float) 3.21; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9993; }
    g = (float) 3.22; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9994; }
    g = (float) 3.23; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9994; }
    g = (float) 3.24; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9994; }
    g = (float) 3.25; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9994; }
    g = (float) 3.26; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9994; }
    g = (float) 3.27; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.28; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.29; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.30; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.31; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.32; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9995; }
    g = (float) 3.33; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.34; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.35; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.36; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.37; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.38; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9996; }
    g = (float) 3.39; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.40; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.41; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.42; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.43; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.44; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.45; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.46; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9997; }
    g = (float) 3.47; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.48; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.49; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.50; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.51; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.52; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.53; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.54; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.55; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.56; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.57; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.58; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.59; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.60; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.61; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9998; }
    g = (float) 3.62; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.63; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.64; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.65; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.66; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.67; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.68; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.69; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.70; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.71; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.72; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.73; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.74; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.75; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.76; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.77; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.78; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.79; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.80; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.81; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.82; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.83; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.84; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.85; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.86; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.87; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.88; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.89; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 0.9999; }
    g = (float) 3.90; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.91; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.92; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.93; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.94; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.95; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.96; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.97; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.98; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
    g = (float) 3.99; if ((float) java.lang.Math.abs(g - datum[i]) * 1000 < k)
    { k = (int) (1000 * (float) java.lang.Math.abs(g - datum[i]));
     f = (float) 1.0000; }
  
    datum[i] = f;

      }
  
      k = 0;
  
      if (h == 1) {
        /* reverse the probability adjustment */
        h = 0;
        datum[i] = 1.0f - datum[i];
      }

    }

    h = 0;
    k = 0;

  } else if (opr == 62) {

  /* STDD */
  /* FOR A GIVEN COVERED PROBABILITY, FIND THE STANDARD DEVIATION DISTANCE.*/
  /* You have a range defined by datadr1 and datadr2, */
  /* containing the deviations. */
  /* After calling the function, you have the same range filled with the */
  /* probabilities covered at each respective deviation. */

    if (datadr2 == 0) { datadr2 = datadr1; }

    /* normalise addresses */
    if (datadr1 > datadr2) { i = datadr2; datadr2 = datadr1; datadr1 = i; }

    
    for (i = datadr1; i <= datadr2; i++) {
      k = 32000; /* way more than greatest possible difference */
      h = 0; /* prepare below-0.5-flag */
      f = (float) datum[i];
      f = (float) java.lang.Math.abs(f);
      /* probability is necessarily positive */
  
      if (f < 0.5f) {f = 0.5f + (float) java.lang.Math.abs(f - 0.5f); h = 1;}
      /* flag probability adjustment */
  
      if (f > 1.0f) { datum[i] = 3.9f; }
      else {
  datum[i] = f;
  g = (float) 0.5000; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.00; }
  g = (float) 0.5040; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.01; }
  g = (float) 0.5080; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.02; }
  g = (float) 0.5120; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.03; }
  g = (float) 0.5160; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.04; }
  g = (float) 0.5199; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.05; }
  g = (float) 0.5239; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.06; }
  g = (float) 0.5279; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.07; }
  g = (float) 0.5319; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.08; }
  g = (float) 0.5359; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.09; }
  g = (float) 0.5398; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.10; }
  g = (float) 0.5438; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.11; }
  g = (float) 0.5478; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.12; }
  g = (float) 0.5517; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.13; }
  g = (float) 0.5557; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.14; }
  g = (float) 0.5596; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.15; }
  g = (float) 0.5636; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.16; }
  g = (float) 0.5675; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.17; }
  g = (float) 0.5714; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.18; }
  g = (float) 0.5753; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.19; }
  g = (float) 0.5793; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.20; }
  g = (float) 0.5832; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.21; }
  g = (float) 0.5871; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.22; }
  g = (float) 0.5910; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.23; }
  g = (float) 0.5948; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.24; }
  g = (float) 0.5987; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.25; }
  g = (float) 0.6026; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.26; }
  g = (float) 0.6064; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.27; }
  g = (float) 0.6103; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.28; }
  g = (float) 0.6141; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.29; }
  g = (float) 0.6179; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.30; }
  g = (float) 0.6217; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.31; }
  g = (float) 0.6255; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.32; }
  g = (float) 0.6293; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.33; }
  g = (float) 0.6331; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.34; }
  g = (float) 0.6368; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.35; }
  g = (float) 0.6406; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.36; }
  g = (float) 0.6443; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.37; }
  g = (float) 0.6480; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.38; }
  g = (float) 0.6517; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.39; }
  g = (float) 0.6554; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.40; }
  g = (float) 0.6591; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.41; }
  g = (float) 0.6628; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.42; }
  g = (float) 0.6664; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.43; }
  g = (float) 0.6700; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.44; }
  g = (float) 0.6736; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.45; }
  g = (float) 0.6772; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.46; }
  g = (float) 0.6808; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.47; }
  g = (float) 0.6844; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.48; }
  g = (float) 0.6879; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.49; }
  g = (float) 0.6915; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.50; }
  g = (float) 0.6950; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.51; }
  g = (float) 0.6985; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.52; }
  g = (float) 0.7019; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.53; }
  g = (float) 0.7054; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.54; }
  g = (float) 0.7088; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.55; }
  g = (float) 0.7123; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.56; }
  g = (float) 0.7157; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.57; }
  g = (float) 0.7190; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.58; }
  g = (float) 0.7224; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.59; }
  g = (float) 0.7257; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.60; }
  g = (float) 0.7291; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.61; }
  g = (float) 0.7324; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.62; }
  g = (float) 0.7357; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.63; }
  g = (float) 0.7389; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.64; }
  g = (float) 0.7422; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.65; }
  g = (float) 0.7454; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.66; }
  g = (float) 0.7486; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.67; }
  g = (float) 0.7517; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.68; }
  g = (float) 0.7549; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.69; }
  g = (float) 0.7580; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.70; }
  g = (float) 0.7611; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.71; }
  g = (float) 0.7642; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.72; }
  g = (float) 0.7673; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.73; }
  g = (float) 0.7704; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.74; }
  g = (float) 0.7734; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.75; }
  g = (float) 0.7764; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.76; }
  g = (float) 0.7794; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.77; }
  g = (float) 0.7823; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.78; }
  g = (float) 0.7852; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.79; }
  g = (float) 0.7881; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.80; }
  g = (float) 0.7910; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.81; }
  g = (float) 0.7939; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.82; }
  g = (float) 0.7967; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.83; }
  g = (float) 0.7995; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.84; }
  g = (float) 0.8023; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.85; }
  g = (float) 0.8051; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.86; }
  g = (float) 0.8078; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.87; }
  g = (float) 0.8106; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.88; }
  g = (float) 0.8133; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.89; }
  g = (float) 0.8159; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.90; }
  g = (float) 0.8186; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.91; }
  g = (float) 0.8212; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.92; }
  g = (float) 0.8238; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.93; }
  g = (float) 0.8264; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.94; }
  g = (float) 0.8289; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.95; }
  g = (float) 0.8315; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.96; }
  g = (float) 0.8340; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.97; }
  g = (float) 0.8365; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.98; }
  g = (float) 0.8389; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 0.99; }
  g = (float) 0.8413; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.00; }
  g = (float) 0.8438; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.01; }
  g = (float) 0.8461; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.02; }
  g = (float) 0.8485; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.03; }
  g = (float) 0.8508; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.04; }
  g = (float) 0.8531; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.05; }
  g = (float) 0.8554; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.06; }
  g = (float) 0.8577; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.07; }
  g = (float) 0.8599; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.08; }
  g = (float) 0.8621; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.09; }
  g = (float) 0.8643; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.10; }
  g = (float) 0.8665; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.11; }
  g = (float) 0.8686; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.12; }
  g = (float) 0.8708; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.13; }
  g = (float) 0.8729; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.14; }
  g = (float) 0.8749; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.15; }
  g = (float) 0.8770; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.16; }
  g = (float) 0.8790; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.17; }
  g = (float) 0.8810; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.18; }
  g = (float) 0.8830; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.19; }
  g = (float) 0.8849; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.20; }
  g = (float) 0.8869; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.21; }
  g = (float) 0.8888; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.22; }
  g = (float) 0.8907; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.23; }
  g = (float) 0.8925; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.24; }
  g = (float) 0.8944; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.25; }
  g = (float) 0.8962; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.26; }
  g = (float) 0.8980; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.27; }
  g = (float) 0.8997; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.28; }
  g = (float) 0.9015; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.29; }
  g = (float) 0.9032; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.30; }
  g = (float) 0.9049; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.31; }
  g = (float) 0.9066; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.32; }
  g = (float) 0.9082; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.33; }
  g = (float) 0.9099; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.34; }
  g = (float) 0.9115; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.35; }
  g = (float) 0.9131; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.36; }
  g = (float) 0.9147; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.37; }
  g = (float) 0.9162; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.38; }
  g = (float) 0.9177; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.39; }
  g = (float) 0.9192; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.40; }
  g = (float) 0.9207; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.41; }
  g = (float) 0.9222; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.42; }
  g = (float) 0.9236; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.43; }
  g = (float) 0.9251; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.44; }
  g = (float) 0.9265; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.45; }
  g = (float) 0.9279; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.46; }
  g = (float) 0.9292; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.47; }
  g = (float) 0.9306; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.48; }
  g = (float) 0.9319; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.49; }
  g = (float) 0.9332; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.50; }
  g = (float) 0.9345; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.51; }
  g = (float) 0.9357; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.52; }
  g = (float) 0.9370; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.53; }
  g = (float) 0.9382; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.54; }
  g = (float) 0.9394; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.55; }
  g = (float) 0.9406; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.56; }
  g = (float) 0.9418; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.57; }
  g = (float) 0.9429; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.58; }
  g = (float) 0.9441; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.59; }
  g = (float) 0.9452; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.60; }
  g = (float) 0.9463; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.61; }
  g = (float) 0.9474; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.62; }
  g = (float) 0.9484; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.63; }
  g = (float) 0.9495; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.64; }
  g = (float) 0.9505; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.65; }
  g = (float) 0.9515; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.66; }
  g = (float) 0.9525; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.67; }
  g = (float) 0.9535; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.68; }
  g = (float) 0.9545; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.69; }
  g = (float) 0.9554; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.70; }
  g = (float) 0.9564; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.71; }
  g = (float) 0.9573; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.72; }
  g = (float) 0.9582; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.73; }
  g = (float) 0.9591; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.74; }
  g = (float) 0.9599; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.75; }
  g = (float) 0.9608; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.76; }
  g = (float) 0.9616; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.77; }
  g = (float) 0.9625; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.78; }
  g = (float) 0.9633; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.79; }
  g = (float) 0.9641; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.80; }
  g = (float) 0.9649; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.81; }
  g = (float) 0.9656; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.82; }
  g = (float) 0.9664; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.83; }
  g = (float) 0.9671; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.84; }
  g = (float) 0.9678; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.85; }
  g = (float) 0.9686; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.86; }
  g = (float) 0.9693; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.87; }
  g = (float) 0.9699; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.88; }
  g = (float) 0.9706; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.89; }
  g = (float) 0.9713; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.90; }
  g = (float) 0.9719; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.91; }
  g = (float) 0.9726; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.92; }
  g = (float) 0.9732; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.93; }
  g = (float) 0.9738; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.94; }
  g = (float) 0.9744; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.95; }
  g = (float) 0.9750; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.96; }
  g = (float) 0.9756; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.97; }
  g = (float) 0.9761; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.98; }
  g = (float) 0.9767; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 1.99; }
  g = (float) 0.9772; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.00; }
  g = (float) 0.9778; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.01; }
  g = (float) 0.9783; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.02; }
  g = (float) 0.9788; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.03; }
  g = (float) 0.9793; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.04; }
  g = (float) 0.9798; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.05; }
  g = (float) 0.9803; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.06; }
  g = (float) 0.9808; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.07; }
  g = (float) 0.9812; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.08; }
  g = (float) 0.9817; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.09; }
  g = (float) 0.9821; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.10; }
  g = (float) 0.9826; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.11; }
  g = (float) 0.9830; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.12; }
  g = (float) 0.9834; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.13; }
  g = (float) 0.9838; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.14; }
  g = (float) 0.9842; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.15; }
  g = (float) 0.9846; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.16; }
  g = (float) 0.9850; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.17; }
  g = (float) 0.9854; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.18; }
  g = (float) 0.9857; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.19; }
  g = (float) 0.9861; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.20; }
  g = (float) 0.9864; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.21; }
  g = (float) 0.9868; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.22; }
  g = (float) 0.9871; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.23; }
  g = (float) 0.9875; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.24; }
  g = (float) 0.9878; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.25; }
  g = (float) 0.9881; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.26; }
  g = (float) 0.9884; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.27; }
  g = (float) 0.9887; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.28; }
  g = (float) 0.9890; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.29; }
  g = (float) 0.9893; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.30; }
  g = (float) 0.9896; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.31; }
  g = (float) 0.9898; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.32; }
  g = (float) 0.9901; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.33; }
  g = (float) 0.9904; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.34; }
  g = (float) 0.9906; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.35; }
  g = (float) 0.9909; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.36; }
  g = (float) 0.9911; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.37; }
  g = (float) 0.9913; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.38; }
  g = (float) 0.9916; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.39; }
  g = (float) 0.9918; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.40; }
  g = (float) 0.9920; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.41; }
  g = (float) 0.9922; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.42; }
  g = (float) 0.9925; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.43; }
  g = (float) 0.9927; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.44; }
  g = (float) 0.9929; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.45; }
  g = (float) 0.9931; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.46; }
  g = (float) 0.9932; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.47; }
  g = (float) 0.9934; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.48; }
  g = (float) 0.9936; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.49; }
  g = (float) 0.9938; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.50; }
  g = (float) 0.9940; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.51; }
  g = (float) 0.9941; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.52; }
  g = (float) 0.9943; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.53; }
  g = (float) 0.9945; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.54; }
  g = (float) 0.9946; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.55; }
  g = (float) 0.9948; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.56; }
  g = (float) 0.9949; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.57; }
  g = (float) 0.9951; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.58; }
  g = (float) 0.9952; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.59; }
  g = (float) 0.9953; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.60; }
  g = (float) 0.9955; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.61; }
  g = (float) 0.9956; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.62; }
  g = (float) 0.9957; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.63; }
  g = (float) 0.9959; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.64; }
  g = (float) 0.9960; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.65; }
  g = (float) 0.9961; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.66; }
  g = (float) 0.9962; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.67; }
  g = (float) 0.9963; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.68; }
  g = (float) 0.9964; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.69; }
  g = (float) 0.9965; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.70; }
  g = (float) 0.9966; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.71; }
  g = (float) 0.9967; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.72; }
  g = (float) 0.9968; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.73; }
  g = (float) 0.9969; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.74; }
  g = (float) 0.9970; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.75; }
  g = (float) 0.9971; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.76; }
  g = (float) 0.9972; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.77; }
  g = (float) 0.9973; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.78; }
  g = (float) 0.9974; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.79; }
  g = (float) 0.9975; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.81; }
  g = (float) 0.9976; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.82; }
  g = (float) 0.9977; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.83; }
  g = (float) 0.9978; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.85; }
  g = (float) 0.9979; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.86; }
  g = (float) 0.9980; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.88; }
  g = (float) 0.9981; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.89; }
  g = (float) 0.9982; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.91; }
  g = (float) 0.9983; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.93; }
  g = (float) 0.9984; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.94; }
  g = (float) 0.9985; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.96; }
  g = (float) 0.9986; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 2.98; }
  g = (float) 0.9987; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.00; }
  g = (float) 0.9988; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.03; }
  g = (float) 0.9989; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.05; }
  g = (float) 0.9990; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.08; }
  g = (float) 0.9991; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.11; }
  g = (float) 0.9992; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.14; }
  g = (float) 0.9993; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.18; }
  g = (float) 0.9994; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.22; }
  g = (float) 0.9995; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.27; }
  g = (float) 0.9996; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.33; }
  g = (float) 0.9997; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.39; }
  g = (float) 0.9998; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.47; }
  g = (float) 0.9999; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.62; }
  g = (float) 1.0000; if ((float) java.lang.Math.abs(g - datum[i]) * 10000 < k)
  { k = (int) (10000 * (float) java.lang.Math.abs(g - datum[i]));
   f = (float) 3.90; }

  datum[i] = f;

      }
  
      k = 0;
  
      if (h == 1) {
        /* reverse the probability adjustment */
        h = 0;
        datum[i] = -1.0f * datum[i];
      }
    }

    h = 0;
    k = 0;

  /* ---- LAST COMMAND OF THE SIX-BIT-RANGE IS RESERVED FOR THE USER ------ */
  } else if (opr == 63) {
    /* USER -- RESERVED, perhaps for GPIO */
  }

  /* ------- END DECODING ------- */

  /* Advance to the next instruction UNLESS it was an */
  /* immediate instruction - in such a case, simply return to the REPL. */
  if (opr != 1) {
    if (pc > 0) {
      pc++;
    } else if (pc == 0) {
    /* do not increase pc=0: just execute immediately and return */
    /* you can even "mix" automatic and immediate mode and return or JUMP, */
    /* whereby a jump will change pc and then go on - your entry point is */
    /* thus freely selectable. A program can be set for automatic execution */
    /* perhaps by writing in instruction[0] a jump to the entry point, but */
    /* herein this is not (yet) implemented; the REPL seems a better start. */
      break;
    }
  }

} /* end execution phase */

} /* end highest level general loop */

} /* end main-function */

}

