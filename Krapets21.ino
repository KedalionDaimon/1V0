/* 1V0 */

/* - Pronounced as the name "Ivo", in honour of my father. */

// Tools -> Board -> ATTinyCore -> ATTiny85(Micronucleus/Digispark)

/* This is as barebones as I could get it, to run on a ATTINY85. */

/* https://github.com/SpenceKonde/ATTinyCore/blob/64ceae892d2dfb38a510cb7257dbcb87904cf28a/Installation.md */
/* http://drazzy.com/package_drazzy.com_index.json */
/* ATtinyCore */
/* OK, Spence Konde's core seems to be phenomenally BETTER than that Digistump thing, IF it works as advertised... */
/* He uses SoftwareSerial instead of SoftSerial. */

#include <EEPROM.h> /* https://github.com/PaulStoffregen/EEPROM */

#include <Arduino.h>
#include <TM1637Display.h>

int CLK = 3;
int DIO = 4;

// TM1637 tm(CLK,DIO);
TM1637Display tm(CLK,DIO);

/* These DATAMEM and EXECMEM hold data and instructions, respectively. */
#define DATAMEM 64

#define EXECMEM 252
/* But the above REQUIRES IADR jumps above 63, a frequent source of errors. */
/* You can activate it freely without any penalty, except said errors! */
/* If you want to jump anywhere without having to use IADR: */
// #define EXECMEM 64
// #define EXECMEM 100

#define INSTRUCTION_BYTE_LENGTH 2

long datum[DATAMEM];

unsigned int instr = 0;
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
long f = 0;


unsigned int runlimit = 9999;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */

/* needed for I/O */
char rd = 0;
char prerd = 0;
char pprerd = 0;
long p;
long pprev = 0;
long ppprev = 0;
char si;

long absvalu(long x) {
    if (x < 0) {
      return -x;
    } else {
      return x;
    }
}

void displayNumber(long longnum) {
  int num = 0;
    if (absvalu(longnum) > 9999) {
      num = longnum / 10000;
      num = num * 10000; // this zeroes the last four digits
      num = longnum - num; // save only the last four digits
      longnum = longnum / 10000; // shorten the overall number
    } else {
      num = longnum;
      longnum = 0;
    }

    // if (longnum != 0) {
      tm.clear(); delay(250);
      if (longnum < 0) {
        tm.showNumberDecEx(absvalu(longnum), 64, true);
      } else {
        tm.showNumberDecEx(absvalu(longnum), 0, true);
      } delay(1001); // ONE-SECOND DELAY TO MAKE IT CLEARER.
    // }

    tm.clear(); delay(250);
    if (num < 0) {
      tm.showNumberDecEx(absvalu(num), 64, true);
    } else {
      tm.showNumberDecEx(absvalu(num), 0, true);
    } delay(2001); // TWO-SECOND DELAY TO MAKE IT CLEARER.
  
//    tm.display(0,0); tm.display(1,0); tm.display(2,0); tm.display(3,0); 
//    tm.display(3, num % 10);   
//    tm.display(2, num / 10 % 10);   
//    tm.display(1, num / 100 % 10);   
//    tm.display(0, num / 1000 % 10);
}

long readint() {
  rd = 0;
  si = 0;
  p = 0;
  int buttonpressed = 0;
  while (si != '*') { /* * is on phone and on numpad */

    delay(66);

    int analogbutton = analogRead(A1);
    if (analogbutton < 20) {
      rd = 6;
    } else if (analogbutton < 120) {
      rd = 7;
    } else if (analogbutton < 220 ) {
      rd = 8;
    } else if (analogbutton < 290 ) {
      rd = 9;
    } else if (analogbutton < 360 ) {
      rd = 10;
    } else if (analogbutton < 410 ) {
      rd = 5;
    } else if (analogbutton < 450 ) {
      rd = 4;
    } else if (analogbutton < 490 ) {
      rd = 3;
    } else if (analogbutton < 520 ) {
      rd = 2;
    } else if (analogbutton < 580 ) {
      rd = 1;
    } else if (analogbutton < 630 ) {
      rd = 11;
    } else if (analogbutton < 670 ) {
      rd = 12;
    } else if (analogbutton < 695 ) {
      rd = 13;
    } else if (analogbutton < 720) {
      rd = 14;
    } else {
      rd = 0;
    }

    if ((pprerd == prerd) && (rd == prerd) && (rd != 0)) {
      buttonpressed = 1;
      si = 0;
      if ((rd > 0) && (rd < 10)) { si = rd + 48; }
      else if (rd == 10) { si = '0'; }
      else if (rd == 11) { si = '*'; }
      else if (rd == 14) { si = '-'; }
      else if (rd == 12) { si = '#'; } // erase
      else if (rd == 13) { si = '?'; } // backspace
      else { buttonpressed = 0; } // safety
      prerd = 0; pprerd = 0; rd = 0;
    } else {
      pprerd = prerd; prerd = rd;
    }
    
    if (buttonpressed == 1) {
      if (si < 58 && si > 47) {
        ppprev = pprev;
        pprev = p;
        p = p * 10 + (si - 48); /*Translating ASCII */
      } else if (si == '-') {
        p = -p;
      } else if (si == '#') { /* Chance to "cancel" */
        /* This will not ALWAYS save you, but often. */
        /* for instance an immediately ordered SVAL */
        /* with 0 as second data address will still */
        /* execute. Same for division and multiplication. */
        /* But it will be nice to cancel the command */
        /* address, the operation, and the 1st datum. */
        p = 0;
        // break; // no break - USER decides does he want to ENTER that zero or start anew.
      } else if (si == '?') { /* Chance to "backspace" up to twice */
        p = pprev;
        pprev = ppprev;
      }
      // instead of:
      if ((si >= 0) && (si <= 9)) { displayNumber(si); } else { displayNumber(p); }
      // if (si == '-') { displayNumber(8888);}
      // if (si == '?') { displayNumber(4004);}
      // if (si == '#') { displayNumber(8008);}
      buttonpressed = 0;
      if (si == '*') { break; } // i.e. either display below or done
      // displayNumber(p);
      si = 0;
      pprerd = 0; prerd = 0; rd = 0;
    }


  }

//  displayNumber(p); // TURNED OFF, WAS REPETITIVE
  return p;
}

void setup() {

pinMode(A0, INPUT);

// tm.init();
// set brightness; 0-7
// tm.set(4);
tm.setBrightness(0x0f);

/* Make data zero: */
/* Ha, no, let the user do it! */


while (1) {
/* the highest level general loop which oscillates between command mode */
/* and general run mode */

/* ------------------------ SETUP AND MAINTENANCE ------------------------ */
/* Read in instruction orders: */
/* - positive addresses: program for later execution; */
/* - zero-command-address: immediate execution according to operators; */
/* - specific negative addresses: specific immediate actions, not covered */
/*   by the usual operators. */
/* This is a kind of REPL or user command interface prior to actually */
/* "running" any sort of longer program. Here, the user still has more */
/* immediate influence over the machine. */


while (1) {
pc = 0; /* safety: nuke the program counter and the immediate instruction */


/* Arduino: flush here any read-cache before reading instructions. */

/* First, get the command address - to determine is it +, 0 or - .*/
/* From there it will depend what action is to be undertaken. */
displayNumber(1111);
cmdadr = readint(); 
/* atof and atoi due to lack of scanf */

/* COMMAND SELECTION IN REPL MODE */
/* Positive or zero - give the details of the instruction to be executed. */
if (cmdadr >= 0) {
  /* GIVE INSTRUCTION */
  displayNumber(2222);
  opr = readint(); 

  displayNumber(3333);
  datadr1 = readint(); 

  displayNumber(4444);
  datadr2 = readint(); 

  /* Compress six instructions into remaining "free" opr-values below 16. */
  if (opr == 28) {
    opr = 3;
  } else if (opr == 53) {
    opr = 7;
  } else if (opr == 54) {
    opr = 13;
  } else if (opr == 55) {
    opr = 14;
  } else if (opr == 56) {
    opr = 15;
  }

  /* Having read the instruction, compose it and save it. */
  
  instr = ((opr & 15) << 12) | ((datadr1 & 63) << 6) | (datadr2 & 63);
  
  EEPROM.put((INSTRUCTION_BYTE_LENGTH * cmdadr) , instr);
  delay(10);

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
  /* LIST INSTRUCTION RANGE -- NOW SIMPLY LIST ONE INSTRUCTION*/
  /* Give an output of the entered instructions between two addresses, */
  /* these addresses considered inclusive. Like "LIST" in BASIC, */
  /* to "let you see what you did" or "what you are about to run". */

  displayNumber(5555);
  i = readint();

    EEPROM.get((INSTRUCTION_BYTE_LENGTH * i) , instr);
    delay(10);

    // displayNumber(i);
    /* as instructions and addresses are only positive, */
    /* the minus sign can be used as a visual separator. */
    // delay(333);
    displayNumber(-((int) ((instr >> 12) & 15)));
    displayNumber(-((int) ((instr >> 6) & 63)));
    displayNumber(-((int) (instr & 63)));
  /* End of the instruction print. */

} else if (cmdadr == -2) {
  /* LIST DATA RANGE -- NOW SIMPLY LIST EVERYTHING */
  /* Same as above, just this time, for data, not instructions. */
  /* Listing data is simpler, just the numbers are given */
  /* - without the pointer interpretation attempted above. */

  displayNumber(5555);
  i = readint();
  displayNumber(datum[i]);


} else if (cmdadr == -4) {
  /* ENTER AT AN ADDRESS ONE DATUM */
  /* Same as above, just this time, you enter data. */
  /* What is different: obviously, negative numbers are totally allowed. */
  /* So this time, the range is not flexible, but determined by two limits. */

  displayNumber(6666);
  datadr1 = readint();
  displayNumber(7777);
  f = readint();
  datum[datadr1] = f;

} else if (cmdadr == -5) {
  /* CLEAR INSTRUCTION RANGE */
  /* Simply erase to zero an entire section of program space, i.e. to */
  /* "no operation". */
  
  displayNumber(8888);
  instr = 0; /* Just to make sure it "understands" the size as 32bit. */
  for (i = 0; i < EXECMEM; i++) {
    EEPROM.put((INSTRUCTION_BYTE_LENGTH * i) , instr);
  }


} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0), not instructions. */

  displayNumber(9999);
  for (i = datadr1; i < DATAMEM; i++) {
    datum[i] = 0;
  }

} else if (cmdadr == -9) {
  displayNumber(runlimit);
  runlimit = readint();
} /* end of command selection */

} /* end of setup and maintenance */





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
    displayNumber(-8888); delay(2001);
    break; /* ejection if the runlimit is over, pc printed for re-entry */
  } else if (runlimit > 1) {
    runlimit--;
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
  datum[0] = 0; /* Useful for unconditional jumps. */

  EEPROM.get((INSTRUCTION_BYTE_LENGTH * pc) , instr);
  delay(10); /* For reliability. Yes, EEPROM can suffer such problems. */

  /* 63 operations possible, on 8191 places of data for each address; 26,13 */
  /* or: 255 operations, on 4095 data places for each address; 24,12 */
  /* Yeah, but here: 16 operations on 63 addresses. */

  opr = (instr >> 12) & 15;
  datadr1 = (instr >> 6) & 63;
  datadr2 = instr & 63;

  /* Extracting six instructions from remaining "free" opr-values below 16. */
  if (opr == 3) {
    opr = 28;
  } else if (opr == 7) {
    opr = 53;
  } else if (opr == 13) {
    opr = 54;
  } else if (opr == 14) {
    opr = 55;
  } else if (opr == 15) {
    opr = 56;
  }
 
  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) && (k > 0)) {
    datadr1 = h;
    datadr2 = k;
    h = 0;
    k = 0;
  }

  /* ALWAYS trace execution. */
  /*
  if (pc > 199) {
    displayNumber((pc - 200) * 100 + opr);
  } else if (pc > 99) {
    displayNumber((pc - 100) * 100 + opr);
  } else {
    displayNumber(pc * 100 + opr);
  }
  */
  /* Trace execution only if within "normal" addressing space. */
  if (pc < 64) {
    displayNumber(pc * 100 + opr);
  }
  

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
  /* JUMP IF THE FIRST ADR CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
  /* "Nearly" zero, because these are floats and repeated float operations */
  /* may render the results imprecise. */
  /* This can very neatly be translated into a high language's IF & WHILE. */
  /* Anyway, welcome to "spaghetti programming". You will use this sole */
  /* condition until you learn to LOVE IT! */

    if (datum[datadr1] <= 0) {
      /* pc = datum[datadr2] / 100; */ /* However, such a default is */
      /* NOT a good idea, actually: with a default COMPUTED GOTO, you */
      /* normally straight GOTO HELL, and have NO adequate way to trace it. */
      /* Instead, let the default be OBVIOUS: */

      pc = datadr2; 
      /* On purpose NOT "computed", but IADR still allows it. */
      /* In fact, on ATTINY85 you NEED IADR in order to make jumps */
      /* beyond instruction 63, because lo and behold, the way jump is */
      /* implemented, datadr2 CANNOT JUMP BEYOND! For IADR, set the */
      /* destination times 100, i.e. to jump to position 80, jump to 8000. */
      /* The obvious "deal with the devil" would be to make the jumps */
      /* computed by default, but that would be incompatible to any other */
      /* 1V0 virtual machine. Unfortunately, both computed jumps and IADR */
      /* mean a mixing of code and data - where code is responsible for */
      /* jumps - and I regard this as sufficiently dangerous to attempt to */
      /* abstain. - Another way to do it - however, incompatible - would */
      /* be to multiply datadr2 * 4 to give you the jump address. Thus, */
      /* You would "decrease jump resolution", but could still go */
      /* anywhere, 63*4 = 252, so you could totally do this, and a jump */
      /* "resolution" of 1, given that you have NOOP, is in no way */
      /* "strictly necessary". However, the incompatibility was not deemed */
      /* acceptable. */
      
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

    /* Here, I DID accept an incompatibility: in reality, the code */
    /* here should be divided by 100, to make up for the fact that */
    /* ALL the numbers here are divisible by 100 due to their */
    /* fixed point. Now, the trouble with this was, in practice it */
    /* was simply too error prone. And IN THE END, in jumps, it was */
    /* STILL "incompatible", because the address finally indicated */
    /* by k was read by the jump as ABSOLUTE. (Had I made jumps */
    /* computed, this would not be an issue, but here I go.) */
    /* So now to have "the one thing divisible by 100, but the other */
    /* one not" was incredibly error-prone. So better than puzzling */
    /* the user with a myriad adjustments, the rule is simple: */
    /* despite all number nature, addresses are always absolute */
    /* integers and they are NOT adjusted. - On the bright side: */
    /* while computations with addresses ARE POSSIBLE, they are */
    /* just a little discouraged, and they CAN be adjusted, */
    /* however, what is more: addresses within the data block */
    /* will now immediately jump into view, as nearly all other */
    /* numbers will normally be bigger than them. After all, who */
    /* will use a system with two decimal digits precision to do */
    /* very much maths below 1... */

    h = datum[datadr1];
    k = datum[datadr2];
    /* Thereby, h & k are "charged" and will trigger a modification of */
    /* the next instruction, substituting its addresses one time. */
    /* You can obviously cancel this with a NOP. */

  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */

    datum[datadr1] = datadr2; /* should have been times 100, but this is */
                              /* too much fuss. */
    /* obviously, if datadr2 == datadr1, this will make an address */
    /* "hold itself" */

  } else if (opr == 6) {
  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
  /* Did not call it EQUL or thelike, to help visually differentiate */
  /* values and addresses. */
    datum[datadr1] = datum[datadr2];
    
  } else if (opr == 8) {
  /* IVAS -- indirectly addressed assignment. */

    i = datum[datadr1];
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

    datum[datadr1] = datum[datadr1] * datum[datadr2] / 100;

  } else if (opr == 12) {
  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
  /* This makes loops much easier than "having to look out for the */
  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */

    if (datum[datadr2] != 0) {
      datum[datadr1] = datum[datadr1] * 100 / datum[datadr2];
    } else {
      datum[datadr1] = 0;
    }

  } else if (opr == 28) {
  /* SUMR -- SUM OF ADR RANGE - RESULT ALWAYS IN FIRST ADR. */
  /* Sum up the range signified by datum[datum[datadr1]] and */
  /* datum[datum[datadr2]], and place the result into datum[datadr1]. */
    h = datum[datadr1];
    k = datum[datadr2];

      datum[datadr1] = 0.0;
      for (i = h; i <= k; i++) {
        datum[datadr1] = datum[datadr1] + datum[i];
      }

    h = 0;
    k = 0;

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

    h = datum[datadr1];
    k = datum[datadr2];

      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] + datum[j];
        j++;
      }

    h = 0;
    k = 0;

  } else if (opr == 54) {
  /* MINR -- SUBTRACT A RANGE FROM A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. */

    h = datum[datadr1];
    k = datum[datadr2];

      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] - datum[j];
        j++;
      }

    h = 0;
    k = 0;

  } else if (opr == 55) {
  /* MULR -- MULTIPLY A RANGE WITH A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. */

    h = datum[datadr1];
    k = datum[datadr2];

      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        datum[i] = datum[i] * datum[j] / 100;
        j++;
      }


    h = 0;
    k = 0;

  } else if (opr == 56) {
  /* DIVR -- DIVIDE A RANGE BY A RANGE */
  /* If they are not of equal length, the operation will stop with the end */
  /* of the shorter range. A division by 0 nullifies the dividend, */
  /* as before on single-number-operations. */

    h = datum[datadr1];
    k = datum[datadr2];

      j = datadr2 + 1;
      for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
        if (datum[j] != 0) {
          datum[i] = datum[i] * 100 / datum[j];
        } else {
          datum[i] = 0;
        }
        j++;
      }


    h = 0;
    k = 0;

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
