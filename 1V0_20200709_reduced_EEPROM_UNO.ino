/* 1V0 */

/* - Pronounced as the name "Ivo", in honour of my father and his memory. */

/* 32254 out of 32256 bytes of flash occupied, beware! */

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

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
// #include <string.h>
#include <EEPROM.h>

/* These DATAMEM and EXECMEM hold data and instructions, respectively. */
#define DATAMEM 361
/* I.e. x4 using 1444 bytes of RAM */
#define EXECMEM 241
/* I.e. x4 using 964 bytes of EEPROM, leaving you with 60 bytes or 15 shifts */
/* If any "location 0" lasts you a year or two, you should be good for some */
/* 10-20 years or so of usage. Yeah, go ahead, I dare you! */

#define INSTRUCTION_BYTE_LENGTH 4
#define ZERO_OFFSET 4
/* This would serve to handle wear of the EEPROM. Should be divisible by 4. */
/* It is here totally useful that I can start instructions way behind instruction #1, e.g. at 11 or so. */
/* This way, they can "stay where they are", even if I have to re-flash and change position 0. */
/* All jumps would have to be updated, of course. */
/* If I then jump to the first instruction, still everything will be executed as it should be, skipping 0s along the way. */

#define TOLERANCE 0.0001
/* #define PI M_PI */
/* Is defined already. */
/* If M_PI is unavailable, make it 3.14159265358979323846 or 355/113 or */
/* simply 3 "for sufficiently large values of 3" */


float datum[DATAMEM];
// unsigned long instruction[EXECMEM];

unsigned long instr = 0;
/* instruction format: */
/* operator, data address 1 (=result), data address 2 */
/* every operator is at a command address. */
int cmdadr = 0;
unsigned int opr = 0;
unsigned int datadr1 = 0;
unsigned int datadr2 = 0;
unsigned long xx;
unsigned long yy;
unsigned long zz;

int pc = 0;
int h = 0; /* beware: h & k are used for "transcendental" instructions ... */
int i = 0;     /* i & j are auxiliary looping variables */
int j = 0;
int k = 0; /* ... so ZERO THEM OUT after non-transcendental usage */
float f = 0.0; /* f & g are auxiliary floating point temporary storage */
double g = 0.0;
char cmnt; /* does NOTHING, serves just as comment facility within terminal */
short tracer = 0;

unsigned int runlimit = 0;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */

/* For scientific float printing. */
double x = 0.0;
char bufr[20];


/* needed for I/O */
long p;
/* double q; */





int readint() {
  p = 0;
  while (!Serial.available()) {}
  p = Serial.parseInt();
//  Serial.print("");
  Serial.print(p);
  Serial.println();
  Serial.flush();
  while ((cmnt = Serial.read()) != '\n' && cmnt != EOF ) { }

  return p;
}

/*
float q; // THIS TOOK UP MUCH MORE SPACE THAN ACTUALLY COPYING THE STUFF AROUND!
float readfloat() {
  q = 0.0;
  while (!Serial.available()) {}
  q = atof((Serial.readString()).c_str());
  Serial.print("");
  Serial.print(dtostre(datum[datadr1], bufr, 6, (0x04 | 0x02 | 0x01)));
  Serial.println();
  Serial.flush();

  return q;
}
*/

/* Print the mnemonic - now in a separate function to tak up less space: */
/* NOT IMPLEMENTED INSTRUCTIONS GET AN ASTERISK. */
void printopr() {
    if (opr == 0) {
      Serial.print(F("NOOP"));
    } else if (opr == 1) {
      Serial.print(F("JUMP"));
    } else if (opr == 2) {
      Serial.print(F("IADR"));
    } else if (opr == 3) {
      Serial.print(F("OUTP"));
    } else if (opr == 4) {
      Serial.print(F("INPT"));
    } else if (opr == 5) {
      Serial.print(F("SADR"));
    } else if (opr == 6) {
      Serial.print(F("SVAL"));
    } else if (opr == 7) {
      Serial.print(F("IAAS"));
    } else if (opr == 8) {
      Serial.print(F("IVAS"));
    } else if (opr == 9) {
      Serial.print(F("PLUS"));
    } else if (opr == 10) {
      Serial.print(F("MINS"));
    } else if (opr == 11) {
      Serial.print(F("MULS"));
    } else if (opr == 12) {
      Serial.print(F("DIVS"));
    } else if (opr == 13) {
      Serial.print(F("POXY"));
    } else if (opr == 14) {
      Serial.print(F("LOXY"));
    } else if (opr == 15) {
      Serial.print(F("IFRA"));
    } else if (opr == 16) {
      Serial.print(F("REMN*"));
    } else if (opr == 17) {
      Serial.print(F("AMNT"));
    } else if (opr == 18) {
      Serial.print(F("PERD"));
    } else if (opr == 19) {
      Serial.print(F("PCNT"));
    } else if (opr == 20) {
      Serial.print(F("SWAP"));
    } else if (opr == 21) {
      Serial.print(F("FACT*"));
    } else if (opr == 22) {
      Serial.print(F("COPY"));
    } else if (opr == 23) {
      Serial.print(F("FRIS"));
    } else if (opr == 24) {
      Serial.print(F("MNMX*"));
    } else if (opr == 25) {
      Serial.print(F("SORT*"));
    } else if (opr == 26) {
      Serial.print(F("CORS*"));
    } else if (opr == 27) {
      Serial.print(F("TURN*"));
    } else if (opr == 28) {
      Serial.print(F("SUMR"));
    } else if (opr == 29) {
      Serial.print(F("SUSQ"));
    } else if (opr == 30) {
      Serial.print(F("IXTH"));
    } else if (opr == 31) {
      Serial.print(F("ABSR"));
    } else if (opr == 32) {
      Serial.print(F("SQRT"));
    } else if (opr == 33) {
      Serial.print(F("SQUA"));
    } else if (opr == 34) {
      Serial.print(F("CBRT*"));
    } else if (opr == 35) {
      Serial.print(F("CUBE*"));
    } else if (opr == 36) {
      Serial.print(F("LNRN"));
    } else if (opr == 37) {
      Serial.print(F("EXPR"));
    } else if (opr == 38) {
      Serial.print(F("RADE"));
    } else if (opr == 39) {
      Serial.print(F("DERA"));
    } else if (opr == 40) {
      Serial.print(F("SIND"));
    } else if (opr == 41) {
      Serial.print(F("COSD"));
    } else if (opr == 42) {
      Serial.print(F("TAND"));
    } else if (opr == 43) {
      Serial.print(F("ASND"));
    } else if (opr == 44) {
      Serial.print(F("ACSD"));
    } else if (opr == 45) {
      Serial.print(F("ATND"));
    } else if (opr == 46) {
      Serial.print(F("MSTD"));
    } else if (opr == 47) {
      Serial.print(F("ZERO*"));
    } else if (opr == 48) {
      Serial.print(F("RAND*"));
    } else if (opr == 49) {
      Serial.print(F("RUND"));
    } else if (opr == 50) {
      Serial.print(F("CEIL*"));
    } else if (opr == 51) {
      Serial.print(F("TANH*"));
    } else if (opr == 52) {
      Serial.print(F("DTNH*"));
    } else if (opr == 53) {
      Serial.print(F("PLUR"));
    } else if (opr == 54) {
      Serial.print(F("MINR"));
    } else if (opr == 55) {
      Serial.print(F("MULR"));
    } else if (opr == 56) {
      Serial.print(F("DIVR"));
    } else if (opr == 57) {
      Serial.print(F("PLUN"));
    } else if (opr == 58) {
      Serial.print(F("MINN"));
    } else if (opr == 59) {
      Serial.print(F("MULN"));
    } else if (opr == 60) {
      Serial.print(F("DIVN"));
    } else if (opr == 61) {
      Serial.print(F("PROB*"));
    } else if (opr == 62) {
      Serial.print(F("STDD*"));
    } else if (opr == 63) {
      Serial.print(F("USER*"));
    } else {
      /* Such an "else" MAY come into existence if not ALL possible */
      /* instructions have been implemented, either theoretically, or */
      /* practically - simply due to a lack of flash space. */
      /* NOKO: NO Known Operation, and is skipped like NOOP. */
      Serial.print(F("NOKO"));
    }
    Serial.print(F(" "));
}

void setup() {

Serial.begin(9600);
while (!Serial) {
  ; // Wait for serial port to connect. Needed for Leonardo only.
  ; // Yeah, but I HAVE Leonardos, too!
}
while ((cmnt = Serial.read()) != '\n' && cmnt != EOF ) { } /* Press Enter to start */
// Serial.println(); // Sometimes it prints something BEFORE it is ready -
                  // let it be at least something irrelevant.

/* Initialise commands to zero, which is no-op */

/*
for (i=0; i<EXECMEM; i++) {
  // EEPROM.put((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, 0); // DEACTIVATED
  // EEPROM IS BETTER CLEARED EXPLICITLY.
}
*/


/* Make data zero: */
for (i=0; i<DATAMEM; i++) {
  datum[i] = 0.0;
}

while (1) {
/* the highest level general loop which oscillates between command mode */
/* and general run mode */
Serial.println(F("INIT"));
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

while (1) {
pc = 0; /* safety: nuke the program counter and the immediate instruction */
EEPROM.put(ZERO_OFFSET, (long) 0);


/* Arduino: flush here any read-cache before reading instructions. */

/* First, get the command address - to determine is it +, 0 or - .*/
/* From there it will depend what action is to be undertaken. */
Serial.print(F("CMD ADR  : "));
cmdadr = readint();
/* atof and atoi due to lack of scanf */

/* COMMAND SELECTION IN REPL MODE */
/* Positive or zero - give the details of the instruction to be executed. */
if (cmdadr >= 0) {
  /* GIVE INSTRUCTION */

  Serial.print(F("OPERATION: "));
  opr = readint();
  Serial.print(F("DATA ADR1: "));
  datadr1 = readint();
  Serial.print(F("DATA ADR2: "));
  datadr2 = readint();
/* COMMENT OUT, IF DESIRED, FROM HERE ...: */
  /* "Eat" the return from the last data address. */
  cmnt = 0;
  while ((cmnt = Serial.read()) != '\n' && cmnt != EOF ) { }
  cmnt = 0;

  /* The comment is optional - just press Enter if you do not want it. */
  Serial.print(F("CMNT: ")); /* A comment is not saved, merely printed to the */
  while (cmnt != '\n') {   /* I/O dialogue. This itself may be useful. */
    while (!Serial.available()) {}
    cmnt = Serial.read();
    Serial.print(cmnt);
    Serial.flush();

    if (cmnt == '#') { /* CANCEL */
      opr = 0;
      datadr1 = 0;
      datadr2 = 0;
      Serial.print(F("CANCELLED# "));
      /* break */ /* no, not immediately, let the user explain why! */
    }
  }
//  Serial.println();
/* ... TO HERE. */

  if ((datadr1 >= DATAMEM) || (datadr2 >= DATAMEM) ||
     (datadr1 < 0) || (datadr1 < 0) || (cmdadr >= EXECMEM)) {
    Serial.print(F("CORR RANGE. DATA 0 ... "));
    Serial.print((DATAMEM - 1));
    Serial.print(F(", INSTR 0 ... "));
    Serial.print((EXECMEM - 1));
    Serial.println();
  }
  /* only positive data addresses - and 0 - are allowed: */
  datadr1 = abs(datadr1);
  datadr2 = abs(datadr2);

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
  
  /* BIT SHIFTING DOES NOT WORK! I.e. it works IN THEORY, but in practice, it */
  /* needs larger numbers - and then somehow the sketch explodes in size. */
  /* instr = (opr << 26) | (datadr1 << 13) | datadr2; /* 63:26:13,or 255:24:12 */
  /* So instead, just multiply: */
  /* instr = ((unsigned long) (67108864 * opr)) + ((unsigned long) (8192 * datadr1)) + ((unsigned long) datadr2); */
  xx = opr & 63;
  yy = datadr1 & 8191;
  zz = datadr2 & 8191;
  instr = (xx << 26) | (yy << 13) | zz;
  
  EEPROM.put((INSTRUCTION_BYTE_LENGTH * (cmdadr - 0)) + ZERO_OFFSET, instr);


  /* Show again the user what was ordered, remind of possible correction. */
  /* First, print the address and the instruction. */
  /* Correction will only be possible if it is not for immediate execution. */
  if (cmdadr < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
  if (cmdadr < 100) { Serial.print(F("0")); }
  if (cmdadr < 10) { Serial.print(F("0")); }
  Serial.print(cmdadr);
  Serial.print(F(" "));
  opr = (instr >> 26) & 63; //-----------------------------------------------------------------------------------------------------------------
  printopr();

  /* Then, print the data addresses and the numbers these point to, and, */
  /* finally, the POSSIBLE numbers in turn these point to. */
  /* If no data has been entered "batch wise" manually, these will */
  /* all be zero. But if data has been entered manually, this may help the */
  /* user to keep track whether the program is being setup as is supposed. */
  /* Obviously, particularly each third number will often be meaningless, */
  /* unless the specific instruction really relates that far */
  if (datadr1 < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
  if (datadr1 < 100) { Serial.print(F("0")); }
  if (datadr1 < 10) { Serial.print(F("0")); }
  datadr1 = (instr >> 13) & 8191; //-------------------------------------------------------------------------------------------------------------
  Serial.print(datadr1);
  Serial.print(F(">"));
  Serial.print(dtostre(datum[datadr1], bufr, 6, (0x04 | 0x02 | 0x01)));
  /* scientific, big E, sign */
  /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

  if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
    j = abs(round(datum[datadr1]));
    Serial.print(F(">"));
    Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
  }
  Serial.print(F(" "));

  if (datadr2 < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
  if (datadr2 < 100) { Serial.print(F("0")); }
  if (datadr2 < 10) { Serial.print(F("0")); }
  datadr2 = instr & 8191; // -------------------------------------------------------------------------------------------------------------------
  Serial.print(datadr2);
  Serial.print(F(">"));
  Serial.print(dtostre(datum[datadr2], bufr, 6, (0x04 | 0x02 | 0x01)));
  /* scientific, big E, sign */
  /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

  if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
    j = abs(round(datum[datadr2]));
    Serial.print(F(">"));
    Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
  }
  Serial.println();

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

  Serial.println(F("LIST INSTRUCTIONS"));
  Serial.print(F("1ST CMD ADR: "));
  datadr1 = readint();
  /* atof and atoi due to lack of scanf */
  Serial.print(F("2ND CMD ADR: "));

  datadr2 = readint();
  /* atof and atoi due to lack of scanf */

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

  Serial.println(F("INSTRUCTIONS:"));
  for (i = h; i <= k; i++) {
    EEPROM.get((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
    xx = (instr >> 26) & 63; /* 4095:255:24:12 or 8191:63:26:13 */
    yy = (instr >> 13) & 8191;
    zz = instr & 8191;
    opr = xx;
    datadr1 = yy;
    datadr2 = zz;

    if (i < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
    if (i < 100) { Serial.print(F("0")); }
    if (i < 10) { Serial.print(F("0")); }
    Serial.print(i);
    Serial.print(F(" "));
    if (opr < 10) { Serial.print(F("0")); }
    Serial.print(opr);
    Serial.print(F("="));
    printopr();

    if (datadr1 < 1000) { Serial.print(F("0")); } /* "leading zeroes". */
    if (datadr1 < 100) { Serial.print(F("0")); }
    if (datadr1 < 10) { Serial.print(F("0")); }
    Serial.print(datadr1);
    Serial.print(F(">"));
    Serial.print(dtostre(datum[datadr1], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
      j = abs(round(datum[datadr1]));
      Serial.print(F(">"));
      Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    Serial.print(F(" "));

    if (datadr2 < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
    if (datadr2 < 100) { Serial.print(F("0")); }
    if (datadr2 < 10) { Serial.print(F("0")); }
    Serial.print(datadr2);
    Serial.print(F(">"));
    Serial.print(dtostre(datum[datadr2], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
      j = abs(round(datum[datadr2]));
      Serial.print(F(">"));
      Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    Serial.println();

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

  Serial.println(F("LIST DATA"));
  Serial.print(F("1ST ADR: "));
  datadr1 = readint();
  Serial.print(F("2ND ADR: "));
  datadr2 = readint();
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
  if (h >= DATAMEM - 5) { h = DATAMEM - 6; } /* correct maximum range */
  if (k >= DATAMEM - 5) { k = DATAMEM - 6; } /* correct maximum range */

  Serial.println(F("DATA:"));
  for (i = h; i <= k; i = i + 5) { /* Print as many as the terminal allows. */
    if (i < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
    if (i < 100) { Serial.print(F("0")); }
    if (i < 10) { Serial.print(F("0")); }
    Serial.print(i);
    Serial.print(F(" "));
    Serial.print(dtostre(datum[i], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    Serial.print(F(" "));
    Serial.print(dtostre(datum[i+1], bufr, 6, (0x04 | 0x02 | 0x01)));
    Serial.print(F(" "));
    Serial.print(dtostre(datum[i+2], bufr, 6, (0x04 | 0x02 | 0x01)));
    Serial.print(F(" "));
    Serial.print(dtostre(datum[i+3], bufr, 6, (0x04 | 0x02 | 0x01)));
    Serial.print(F(" "));
    Serial.print(dtostre(datum[i+4], bufr, 6, (0x04 | 0x02 | 0x01)));
    Serial.println();
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

  /* OMITTED FOR SPACE REASONS */

} else if (cmdadr == -4) {
  /* ENTER DATA AS FLOATS WITHIN A PRE-SPECIFIED RANGE */
  /* Same as above, just this time, you enter data. */
  /* What is different: obviously, negative numbers are totally allowed. */
  /* So this time, the range is not flexible, but determined by two limits. */

  Serial.println(F("ENTER DATA FLOATS"));
  Serial.print(F("1ST ADR: "));
  datadr1 = readint();
  Serial.print(F("2ND ADR: "));
  datadr2 = readint();
  if ((datadr1 < 0) || (datadr2 < 0) || (datadr2 < datadr1)) { break; }
  /* fast eject - and ONLY here; below, you are expected to give data */

  while (datadr1 <= datadr2) {
    if (datadr1 < 1000) { Serial.print(F("0")); } /* "Formatting with leading zeroes". */
    if (datadr1 < 100) { Serial.print(F("0")); }
    if (datadr1 < 10) { Serial.print(F("0")); }
    Serial.print(datadr1);
    Serial.print(F(": DATUM: "));

    while (!Serial.available()) {}
    f = atof((Serial.readString()).c_str());
    Serial.print("");
    Serial.print(dtostre(f, bufr, 6, (0x04 | 0x02 | 0x01)));
    Serial.println();
    Serial.flush();
    
    datum[datadr1] = f;
    datadr1++;
  }

} else if (cmdadr == -5) {
  /* CLEAR INSTRUCTION RANGE */
  /* Simply erase to zero an entire section of program space, i.e. to */
  /* "no operation". */

  Serial.println(F("CLEAR INSTRUCTION RANGE"));
  Serial.print(F("1ST CMD ADR: "));
  datadr1 = readint();
  Serial.print(F("2ND CMD ADR: "));
  datadr2 = readint();
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
    EEPROM.put((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
  }
  Serial.println(F("INSTRUCTIONS CLEARED"));

  h = 0;
  k = 0;
  i = 0;

} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0.0), not instructions. */

  Serial.println(F("CLEAR DATA RANGE"));
  Serial.print(F("1ST ADR: "));
  datadr1 = readint();
  Serial.print(F("2ND ADR: "));
  datadr2 = readint();
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
    datum[i] = 0.0;
  }
  Serial.println(F("DATA CLEARED"));

  h = 0;
  k = 0;
  i = 0;

} else if (cmdadr == -7) {
  /* TRACING: show - or do not - which command is executed, */
  /* as the program runs. */
  Serial.println(F("TRACE ON"));
  tracer = 1;
} else if (cmdadr == -8) {
  Serial.println(F("TRACE OFF"));
  tracer = 0;
} else if (cmdadr == -9) {
  Serial.print(F("SET RUNLIMIT, UP TO 32767, 0=INFINITE, TERMINATING WHEN 1: "));
  runlimit = readint();
} else if (cmdadr == -10) {
  /* OMITTED, NOT SENSIBLE - YOU CANNOT "EXIT" TO ANYWHERE. */

} /* end of command selection */

} /* end of setup and maintenance */







/* DEBUG */
/*

xx = (instr >> 26) & 63;
yy = (instr >> 13) & 8191;
zz = instr & 8191;

Serial.print("Mid-air: instr=");
Serial.print(instr);
Serial.print(" EEPROMLONG[0]=");
EEPROM.get(ZERO_OFFSET, p);
Serial.print(p);
Serial.print(" xx=");
Serial.print(xx);
Serial.print(" yy=");
Serial.print(yy);
Serial.print(" zz=");
Serial.print(zz);
Serial.print(" oprBefore=");
Serial.print(opr);
Serial.print(" d1Before=");
Serial.print(datadr1);
Serial.print(" d2Before=");
Serial.print(datadr2);
Serial.println();


opr = xx;
datadr1 = yy;
datadr2 = zz;
*/










/* ----------------------- COMMAND EXECUTION PHASE ----------------------- */
/* All sails are set and you are now in the hands of Fate. The REPL has */
/* been exited, and whatever the Program Counter pc encounters is what */
/* shall be executed. If you enter into an infinite loop - tough luck! */
/* Actually, due to runlimit it should be not so bad now, either... */

/* As described, all commands are defined by an opcode operating on two */
/* addresses, whereby the first one also, generally, bears the result and */
/* is, roughly speaking, "more important". Often, the second address may be */
/* set to 0 as a short hand to signify that it does not matter. */


while (pc < EXECMEM) {
  if (runlimit == 1) {
    Serial.print(F("RUN EXHAUSTED PC="));
    Serial.print(pc);
    Serial.print(F("STOPPED"));
    Serial.println();
    break; /* ejection if the runlimit is over, pc printed for re-entry */
  } else if (runlimit > 1) {
    runlimit--;
    if (((runlimit - 1) % 100) == 0) {
      Serial.print(F("RUNLIMIT = "));
      Serial.print(runlimit);
      Serial.println();

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
  datum[0] = 0.0; /* Useful for unconditional jumps. */

  if (pc < 0) {
    pc = 0;
  }

  EEPROM.get((INSTRUCTION_BYTE_LENGTH * (pc - 0)) + ZERO_OFFSET, instr);

  /* 63 operations possible, on 8191 places of data for each address; 26,13 */
  /* or: 255 operations, on 4095 data places for each address; 24,12 */
  xx = (instr >> 26) & 63;
  yy = (instr >> 13) & 8191;
  zz = instr & 8191;

  opr = xx;
  datadr1 = yy;
  datadr2 = zz;

  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) && (k > 0)) {
    datadr1 = h;
    datadr2 = k;
    h = 0;
    k = 0;
  }

  /* DUBUG */ /*
  Serial.print("h=");
  Serial.print(h);
  Serial.print(" k=");
  Serial.print(k);
  Serial.print(" opr=");
  Serial.print(opr);
  Serial.print(" d1=");
  Serial.print(datadr1);
  Serial.print(" d2=");
  Serial.print(datadr2);
  Serial.print(" pc=");
  Serial.print(pc);
  Serial.println(); */

  /* TRACE EXECUTION OF EACH PRESENT INSTRUCTION */
  /* This is actually rather similar to "LIST INSTRUCTION RANGE", */
  /* only now, not a "range" is listed, but each instruction as it is */
  /* readied for execution. The tracer must be set for this to happen, */
  /* as it will likely impact speed - particularly on Arduino, this may */
  /* involve printing delays to a terminal - and will generate A LOT */
  /* of output. */
  if (tracer != 0) {
  /* Arduino should get delays here, due to printing. */

    if (pc < 1000) { Serial.print(F("0")); } /* "Leading zeroes. */
    if (pc < 100) { Serial.print(F("0")); }
    if (pc < 10) { Serial.print(F("0")); }
    Serial.print(pc);
    Serial.print(F(" "));
    // if (cmdadr < 100) { Serial.print(F("0")); } /* if >99 instr. */
    if (cmdadr < 10) { Serial.print(F("0")); }
    Serial.print(opr);
    Serial.print(F("="));
    printopr();


    if (datadr1 < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
    if (datadr1 < 100) { Serial.print(F("0")); }
    if (datadr1 < 10) { Serial.print(F("0")); }
    Serial.print(datadr1);
    Serial.print(F(">"));
    Serial.print(dtostre(datum[datadr1], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
      j = abs(round(datum[datadr1]));
      Serial.print(F(">"));
      Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    Serial.print(F(" "));

    if (datadr2 < 1000) { Serial.print(F("0")); } /* "leading zeroes" */
    if (datadr2 < 100) { Serial.print(F("0")); }
    if (datadr2 < 10) { Serial.print(F("0")); }
    Serial.print(datadr2);
    Serial.print(F(">"));
    Serial.print(dtostre(datum[datadr2], bufr, 6, (0x04 | 0x02 | 0x01)));
    /* scientific, big E, sign */
    /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */

    if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
      j = abs(round(datum[datadr2]));
      Serial.print(F(">"));
      Serial.print(dtostre(datum[j], bufr, 6, (0x04 | 0x02 | 0x01)));
      /* scientific, big E, sign */
      /* 4: E, not e, 1: plus or space, 2: of these plus and not space. */
    }
    Serial.println();


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

/* DEBUG */
/*   Serial.println("Entering decoder."); */

  if (opr == 0) {
  /* NOOP -- NO OPERATION: Do nothing. */
  /* Any non-implemented opcode will work as NOP. */
  /* This is also nice if you want to temporarily de-activate certain code. */

  } else if (opr == 1) {
  /* JUMP */
  /* JUMP IF THE FIRST ADR CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
  /* "Nearly" zero, because these are floats and repeated float operations */
  /* may render the results imprecise. */
  /* This can very neatly be translated into a high language's IF & WHILE. */
  /* Anyway, welcome to "spaghetti programming". You will use this sole */
  /* condition until you learn to LOVE IT! */

    if (datum[datadr1] <= TOLERANCE) {
      /* pc = abs(round(datum[datadr2])); */ /* However, such a default is */
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
  /* IADR -- INDIRECT ADRING: */
  /* SUBSTITUTE datadr1 AND datadr2 IN THE NEXT INSTRUCTION BY h AND k. */
  /* Whatever the next operation is, datum[datadr1] shall give */
  /* the next datadr1, and datum[datadr2] shall give the next datadr2. */
  /* This shall serve for a rather powerful "indirect addressing" */
  /* whenever such is needed, in a flexible manner. */

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
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


  } else if (opr == 4) {
  /* INPT -- write 1 for INPUT if you have a numeric display. */
  /* This instruction will read a range of numbers, unless */
  /* the second address is the same as the first address or 0, */
  /* indicating that only a single number is to be read. */

    Serial.print(F("DATA INPUT INTO RANGE "));
    Serial.print(datadr1);
    Serial.print(F(" TO "));
    Serial.print(datadr2);
    Serial.println();
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
      Serial.print(F("INPUT:  ADR="));
      Serial.print(datadr1);
      Serial.print(F(" DATUM="));

      while (!Serial.available()) {}
      f = atof((Serial.readString()).c_str());
      Serial.print("");
      Serial.print(dtostre(f, bufr, 6, (0x04 | 0x02 | 0x01)));
      Serial.println();
      Serial.flush();
      
      datum[datadr1] = f;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        Serial.print(F("INPUT:  ADR="));
        Serial.print(i);
        Serial.print(F(" DATUM="));
        
        while (!Serial.available()) {}
        f = atof((Serial.readString()).c_str());
        Serial.print("");
        Serial.print(dtostre(f, bufr, 6, (0x04 | 0x02 | 0x01)));
        Serial.println();
        Serial.flush();
        
        datum[i] = f;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        Serial.print(F("INPUT:  ADR="));
        Serial.print(i);
        Serial.print(F(" DATUM="));
        
        while (!Serial.available()) {}
        f = atof((Serial.readString()).c_str());
        Serial.print("");
        Serial.print(dtostre(f, bufr, 6, (0x04 | 0x02 | 0x01)));
        Serial.println();
        Serial.flush();
        
        datum[i] = f;
      }
    }

  } else if (opr == 5) {
  /* SADR -- SET ADR AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */
    /* DEBUG */ /*
    Serial.println("Entered SADR");
    Serial.flush(); */

    datum[datadr1] = 0.0 + datadr2;
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
    i = abs(round(datum[datadr1]));
    datum[i] = 0.0 + datadr2;

  } else if (opr == 8) {
  /* IVAS -- see above. */

    i = abs(round(datum[datadr1]));
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

    if (fabs(datum[datadr2]) != 0.0) {
      datum[datadr1] = datum[datadr1] / datum[datadr2];
    } else {
      datum[datadr1] = 0.0;
    }

  } else if (opr == 13) {
  /* POXY -- Power Of X: Y, that is, X ^ Y, BASED ON |X| WITH SIGN EX POST. */
  /* This is a rather powerful instruction, because 1/Y is the Yth root */
  /* (so Y = 0.5 is a square root, Y= 0.3333 is a cube root, and so on),
  /* Y = -1 sets X to 1/X, and so forth. - All this operates on the */
  /* ABSOLUTE VALUE OF X, and preserving the sign, to prevent imaginary */
  /* number results. Unfortunately, this has the funny effect that -3^2=-9. */

    /* A ^ B =  E ^ (B * ln(A)), but that is not even necessary due to pow. */
    if (datum[datadr1] == 0.0) {
      datum[datadr1] = 0.0;
      /* Wanton value because it is undefined for negative exponents */
      /* and for positive exponents, 0^whatever=0. */
    } else if (datum[datadr1] < 0.0) {
      datum[datadr1] = -1.0 * pow(fabs(datum[datadr1]), datum[datadr2]);
    } else {
      datum[datadr1] = pow(datum[datadr1], datum[datadr2]);
    }

  } else if (opr == 14) {
  /* LOXY -- LogY X, i.e. of X to the base of Y. X needs not be an integer. */
  /* This is based on the effect of loga (b) = ln (b) / ln (a), */
  /* b is in datadr1, a is in datadr2, and both are forced to be positive. */
  /* If any of the two numbers is 0.0, the result is set to be 0.0, too. */
    if ((abs (datum[datadr1]) != 0) && (abs (datum[datadr2]) != 0)) {
      datum[datadr1] = log(fabs(datum[datadr1])) / log(fabs(datum[datadr2]));
    } else {
      datum[datadr1] = 0.0;
    }

  } else if (opr == 15) {
  /* IFRA -- Integral and Fractional part of a number stored in datadr1. */
    datum[datadr2] = modf(datum[datadr1], &g); /* the fractional part */
    datum[datadr1] = g; /* the integral part */
    /* If you set datadr2 to be 0, you will thus turn a float to an int. */

  } else if (opr == 16) {
  /* REMN -- Remainer of the division between datum[datadr1] and */
  /* datum[datadr2], whereby 0 is assumed in case of division by 0. */

  /* OMITTED FOR SPACE REASONS */

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
    g = pow(1 + (g / 100), f);
    datum[datadr1] = g;
    /* you overwrite the period, but keep the percent untouched */
    f = 0.0;
    g = 0.0;

  } else if (opr == 18) {
  /* PERD -- Find the period, whereby */
  /* datadr1 contains the final amount */
  /* datadr2 contains the percent. */

    f = datum[datadr1];
    g = datum[datadr2];
    g = (1 + (g / 100));
    g = log(g);
    f = log(f);
    g = f / g;
    datum[datadr1] = g;
    /* you overwrite the final amount, but keep the percent untouched */
    f = 0.0;
    g = 0.0;

  } else if (opr == 19) {
  /* PCNT -- Find the percent, whereby */
  /* datadr1 contains the final amount */
  /* datadr2 contains the period */

    f = datum[datadr1];
    g = datum[datadr2];
    g = 1/g;
    g = 100 * (pow(f, g) - 1);
    datum[datadr1] = g;
    /* you overwrite the final amount, but keep the period untouched */
    f = 0.0;
    g = 0.0;

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

  /* OMITTED FOR SPACE REASONS */

  /* ---- FROM NOW ON FOLLOW COMMANDS FOR NUMERIC RANGE MANIPULATION ------ */
  /* Many of these actually have single-number application facilities, too. */

  } else if (opr == 22) {
  /* COPY -- COPY ADR RANGE: */
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
    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));

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

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 25) {
  /* SORT -- SORT RANGE */
  /* This is pretty naive and sacrifices run time to save memory. */
  /* Basically, each time when numbers are "unsorted", swap them, */
  /* and run through the whole array until no more swaps are done. */
  /* If nothing is left to swap - then necessarily all must be in order. */
  /* I am retaining this function despite the more interesting "coupled */
  /* sort" below, because it is simply quite easy to use. */

  /* OMITTED FOR SPACE REASONS */

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

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 27) {
  /* TURN -- TURN A RANGE UPSIDE DOWN. */
  /* If in the range the numbers were 1.0 3.0 -3.8 2.1, make them be */
  /* instead 2.1 -3.8 3.0 1.0. The range is assumed between datadr1 and */
  /* datadr2, both inclusive. */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 28) {
  /* SUMR -- SUM OF ADR RANGE - RESULT ALWAYS IN FIRST ADR. */
  /* Sum up the range signified by datum[datum[datadr1]] and */
  /* datum[datum[datadr2]], and place the result into datum[datadr1]. */
    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == k) || (k == 0)) { /* nothing to sum up if only one number */
        datum[datadr1] = datum[h];
    } else if (h < k) {
      datum[datadr1] = 0.0;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    } else if (h > k) {
      datum[datadr1] = 0.0;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }
    }
    h = 0;
    k = 0;

  } else if (opr == 29) {
  /* SUSQ -- SUM OF SQUARES AND SQUARE ROOTED SUM OF SQUARES */
  /* For vectors, statistics,... The range ADRES are held in */
  /* datadr1 and datadr2, i.e. this is again indirect, and after execution, */
  /* datadr1 has the sum of squares and datadr2 has the square root of it. */
  /* Only anything to do if the addresses really are in a RANGE: */
    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == k) || (k == 0)) {
        datum[datadr1] = fabs(datum[h]); /* sqrt(x^2) = abs(x) */
    } else if (h < k) {
      datum[datadr1] = 0.0;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
      }
    } else if (h > k) {
      datum[datadr1] = 0.0;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
      }
    }
      datum[datadr2] = sqrt(datum[datadr1]);
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
        if (datum[datadr1] != 0.0) {
          datum[datadr1] = 1.0/datum[datadr1];
        } else {
          datum[datadr1] = 0.0;
        }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if (datum[i] != 0.0) {
          datum[i] = 1.0/datum[i];
        } else {
          datum[i] = 0.0;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if (datum[i] != 0.0) {
          datum[i] = 1.0/datum[i];
        } else {
          datum[i] = 0.0;
        }
      }
    }

  } else if (opr == 31) {
  /* ABSR -- ABSOLUTE VALUE OF RANGE. */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 32) {
  /* SQRT -- SQUARE ROOT ABSOLUTES IN RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = sqrt(fabs(datum[datadr1]));
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = sqrt(fabs(datum[i]));
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = sqrt(fabs(datum[i]));
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

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 35) {
  /* CUBE RANGE */
  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

  /* OMITTED FOR SPACE REASONS */

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
        if (datum[datadr1] != 0.0) {
          datum[datadr1] = log(fabs(datum[datadr1]));
        } else {
          datum[datadr1] = 0.0;
        }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if (datum[i] != 0.0) {
          datum[i] = log(fabs(datum[i]));
        } else {
          datum[i] = 0.0;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if (datum[i] != 0.0) {
          datum[i] = log(fabs(datum[i]));
        } else {
          datum[i] = 0.0;
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
        datum[datadr1] = exp(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = exp(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = exp(datum[i]);
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
        datum[datadr1] = sin(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = sin(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = sin(datum[i]);
      }
    }

  } else if (opr == 41) {
  /* COSD -- Cosinus of degrees in a range.  */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = datum[datadr1] * PI / 180;
        datum[datadr1] = cos(datum[datadr1]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = cos(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = datum[i] * PI / 180;
        datum[i] = cos(datum[i]);
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
      if (cos(datum[datadr1]) != 0.0) {
        datum[datadr1] = sin(datum[datadr1])/cos(datum[datadr1]);
      } else {
        datum[datadr1] = 0.0;
      }
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        if (cos(datum[i]) != 0.0) {
          datum[i] = datum[i] * PI / 180;
          datum[i] = sin(datum[i])/cos(datum[i]);
        } else {
          datum[i] = 0.0;
        }
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        if (cos(datum[i]) != 0.0) {
          datum[i] = datum[i] * PI / 180;
          datum[i] = sin(datum[i])/cos(datum[i]);
        } else {
          datum[i] = 0.0;
        }
      }
    }

  } else if (opr == 43) {
  /* ASND -- Arcussinus of a range, giving degrees.  */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = asin(datum[datadr1]) * 180.0 / PI;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = asin(datum[i]) * 180.0 / PI;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = asin(datum[i]) * 180.0 / PI;
      }
    }

  } else if (opr == 44) {
  /* ACSD -- Arcuscosinus of a range, giving degrees. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = acos(datum[datadr1]) * 180.0 / PI;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = acos(datum[i]) * 180.0 / PI;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = acos(datum[i]) * 180.0 / PI;
      }
    }

  } else if (opr == 45) {
  /* ATND -- Arcustangens of a range, giving degrees. */

  /* The range is signified by datadr1 and datadr2, and if these are equal, */
  /* or if datadr2 is zero, then just perform that operation on the first */
  /* number, i.e. on datum[datadr1]. Same principle as above. */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[datadr1] = atan(datum[datadr1]) * 180.0 / PI;
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = atan(datum[i]) * 180.0 / PI;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = atan(datum[i]) * 180.0 / PI;
      }
    }

  } else if (opr == 46) {
  /* MSTD -- MEAN AND STANDARD DEVIATION ON A SAMPLE. */
  /* I.e. this is WITH "Bessel's correction" for the standard deviation. */
  /* A statistical function. datadr1 and datadr2 indicate a range, */
  /* datum[datum[datadr1]] till datum[datum[datadr2]]. After application, */
  /* the mean of the range is contained in datadr1 and the standard */
  /* deviation in datadr2. A Gaussian distribution is assumed. */

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    j = abs(h - k); /* how many cells minus 1*/
    if ((h == k) || (k == 0)) { /* "very funny" - no range */
      datum[datadr1] = datum[h]; /* No standard deviation can be given. */
    } else if (h < k) {
      datum[datadr1] = 0.0;
      datum[datadr2] = 0.0;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
        datum[datadr2] = datum[datadr2] + (datum[i] * datum[i]);
      }
    } else if (h > k) {
      datum[datadr1] = 0.0;
      datum[datadr2] = 0.0;
      for (i = k; i <= h; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
        datum[datadr2] = datum[datadr2] + (datum[i] * datum[i]);
      }
    }
      datum[datadr1] = datum[datadr1]/(j + 1); /* average or mean */
      datum[datadr2] = sqrt(datum[datadr2]/j); /* standard deviation */
      /* Note that "j" is quasi "n-1" in the usual mathematical notation. */
    h = 0;
    k = 0;

  } else if (opr == 47) {
  /* ZERO -- ZERO OUT A DATA RANGE. */
  /* While there are numerous ways to do it, this will be the most obvious */
  /* choice for the user. - As there is no provision for changing */
  /* instructions in a running program, there is no corresponding memory */
  /* clearance for instructions; this must be done manually at the REPL. */

  /* THIS SHORTHAND HAS BEEN OMITTED:
  /* NOTE THE SHORTHAND OF 0 0 TO CLEAR ALL MEMORY. This may be useful in */
  /* order to clear residual values or begin a calculation anew, e.g. */
  /* asking the user for input. */

  /* PARTS OMITTED FOR SPACE REASONS */

    if ((datadr1 == datadr2) || (datadr2 == 0)) {
    /* If JUST datadr2 is 0, see it as shorthand to just clear datadr1. */
        datum[datadr1] = 0.0;
    } else if (datadr1 <= datadr2) {
      for (i = datadr1; i <= datadr2; i++) { 
        datum[i] = 0.0;
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) { 
        datum[i] = 0.0;
      }
    }


  } else if (opr == 48) {
  /* RAND -- GENERATE RANDOM NUMBERS. */
  /* Whereby datadr1 contains the minimum allowed number and datadr2 */
  /* contains the maximum allowed number of the range. The numbers are then */
  /* generated within that range, eventually also overwriting the minimum */
  /* and the maximum values with random numbers within the desired limits. */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 49) {
  /* RUND -- ROUND RANGE. */

  /* This and the following two functions attempt to turn floats into */
  /* integers. Again, they can be applied to a single number or a range. */

    i = datadr1;
    if ((datadr1 == datadr2) || (datadr2 == 0)) {
        datum[i] = round(datum[i]);
    } else if (datadr1 < datadr2) {
      for (i = datadr1; i <= datadr2; i++) {
        datum[i] = round(datum[i]);
      }
    } else if (datadr1 > datadr2) {
      for (i = datadr2; i <= datadr1; i++) {
        datum[i] = round(datum[i]);
      }
    }

  } else if (opr == 50) {
  /* CEIL -- CEILING RANGE */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 51) {
  /* TANH -- TANH OF RANGE */
  /* tanh(x) = (e^x - e^(-x)) / (e^x + e^(-x)) */
  /* Just in case anybody does anything logistic or wants a neural network */
  /* to be programmed dynamically - this is a famous activation function. */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 52) {
  /* DTNH -- DERIVATIVE OF TANH */
  /* derivtanh(x) = 1 - tanh(x) * tanh(x) */
  /* This is usually used in conjunction with the above. */

  /* OMITTED FOR SPACE REASONS */

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

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] + datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADRES and cannot be used themselves */
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

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] - datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADRES and cannot be used themselves */
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

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] * datum[j];
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADRES and cannot be used themselves */
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

    h = abs(round(datum[datadr1]));
    k = abs(round(datum[datadr2]));
    if ((h == datadr1) || (k == datadr2)) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if ((h > datadr1) && (k > datadr2)) {
      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        if (datum[j] != 0.0) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0;
        }
        j++;
      }
    } else if ((h < datadr1) && (k < datadr2)) {
      /* still, datadr have the ADRES and cannot be used themselves */
      j = datadr2 - 1;
      for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
        if (datum[j] != 0.0) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0;
        }
        j--;
      }
    } else if ((h > datadr1) && (k < datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 - 1;
      for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
        if (datum[j] != 0.0) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0;
        }
        j--;
      }
    } else if ((h < datadr1) && (k > datadr2)) {
      /* the mixed modes give you a "reversed array" operation */
      j = datadr2 + 1;
      for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
        if (datum[j] != 0.0) {
          datum[i] = datum[i] / datum[j];
        } else {
          datum[i] = 0.0;
        }
        j++;
      }
    }

    h = 0;
    k = 0;

  /* ---- FROM NOW ON FOLLOW RANGE-WITH-SINGLE-NUMBER-OPERATIONS ---------- */
  } else if (opr == 57) {
  /* PLUN -- ADD A NUMBER TO A RANGE. */

    h = abs(round(datum[datadr1]));
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

    h = abs(round(datum[datadr1]));
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

    h = abs(round(datum[datadr1]));
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

    h = abs(round(datum[datadr1]));
    if (h == datadr1) {
      /* then simply do nothing, there is exactly NO range to operate on. */
    } else if (h > datadr1) {
      if (datum[datadr2] != 0.0) {
        for (i = datadr1 + 1; i <= h; i++) {
          datum[i] = datum[i] / datum[datadr2];
        }
      } else {
        for (i = datadr1 + 1; i <= h; i++) {
          datum[i] = 0.0;
        }
      }
    } else if (h < datadr1) {
      if (datum[datadr2] != 0.0) {
        for (i = datadr1 - 1; i >= h; i--) {
          datum[i] = datum[i] / datum[datadr2];
        }
      } else {
        for (i = datadr1 - 1; i >= h; i--) {
          datum[i] = 0.0;
        }
      }
    }

    h = 0;
    k = 0;

  /* ---- FROM NOW ON FOLLOW LOOK-UP TABLES FOR STATISTICAL PURPOSES ------ */
  /* The question to include them or not may be, trivially, flash space. */
  /* PHI Z LOOKUP TABLES - WORKING BOTH WAYS */
  /* FIRST KISS: ROUND TO NEAREST VALUE, DO NOT INTERPOLATE. */
  } else if (opr == 61) {

  /* PROB */
  /* FOR A GIVEN STANDARD DEVIATION DISTANCE, FIND THE PROBABILITY COVERED. */
  /* You have a range defined by datadr1 and datadr2, */
  /* containing the deviations. */
  /* After calling the function, you have the same range filled with the */
  /* probabilities covered at each respective deviation. */

  /* OMITTED FOR SPACE REASONS */

  } else if (opr == 62) {

  /* STDD */
  /* FOR A GIVEN COVERED PROBABILITY, FIND THE STANDARD DEVIATION DISTANCE.*/
  /* You have a range defined by datadr1 and datadr2, */
  /* containing the deviations. */
  /* After calling the function, you have the same range filled with the */
  /* probabilities covered at each respective deviation. */

  /* OMITTED FOR SPACE REASONS */

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

} /* end void-setup of Arduino */

void loop() {}
