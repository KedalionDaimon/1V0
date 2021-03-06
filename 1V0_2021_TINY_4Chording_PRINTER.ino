/* 1V0 */

/* - Pronounced as the name "Ivo", in honour of my father. */

// Tools -> Board -> ATTinyCore -> ATTiny85(Micronucleus/Digispark)


/* This is as barebones as I could get it, to run on a ATTINY85. */

/* https://github.com/SpenceKonde/ATTinyCore/blob/64ceae892d2dfb38a510cb7257dbcb87904cf28a/Installation.md */
/* http://drazzy.com/package_drazzy.com_index.json */
/* ATtinyCore */
/* OK, Spence Konde's core seems to be phenomenally BETTER than that Digistump thing, IF it works as advertised... */
/* He uses SoftwareSerial instead of SoftSerial. */

/* For the three-button-keyboard below */
#define PIN_A 0
#define PIN_B 1
#define PIN_C 2
#define PIN_D 4

#include <EEPROM.h> /* https://github.com/PaulStoffregen/EEPROM */

// #define SRL Serial
//^^^^^^^^^^^^^^^^^^^ USE THIS IF YOU TEST ON UNO/MEGA

#include <SoftwareSerial.h>

/* -1 for the unused RX: */
 SoftwareSerial SRL(-1,3); // works on PIN2 & PIN3, whichechever you pick.
//^^^^^^^^^^^^^^^^^^ USE THIS FOR THE ATTINY85

/* These DATAMEM and EXECMEM hold data and instructions, respectively. */
#define DATAMEM 64

// #define EXECMEM 252
/* But the above REQUIRES IADR jumps above 63, a frequent source of errors. */
/* You can activate it freely without any penalty, except said errors! */
/* If you want to jump anywhere without having to use IADR: */
#define EXECMEM 64

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


unsigned int runlimit = 0;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */

/* needed for I/O */
char rd = 0;
char prerd = 0;
char pprerd = 0;
long p;
long pprev = 0;
long ppprev = 0;
char PosX = 0;
char PosY = 0;
char si;
char tabula = 0;

/* Now comes the "number reading function" of this approach.
   You are assumed to have a keyboard of three buttons. Pressing them in
   different sequences gives you different "output", i.e. it is a sort of
   "chord input" mechanism. The Digistump ATTINY85 has 6 pins, Nr. 5 for
   reset, Nr. 3 for "weird behaviour" due to an internal 1.5 kOhm resistor.
   So you are left with 4. Now, one you need for I/O, and Nr. 2 works
   nicely. You are thus left with 0, 1 and 4. You assign them to A, B and C.
   If you press A, B, A, it gives you "1". If you press "B, A, B" it gives
   you "2", and so forth. If you press three same, e.g. C, C, C it deletes
   the number you are entering. If you press two first same, e.g. B, B, C,
   it makes a backspace, i.e. deletes the previous digit. If you press two
   final same, e.g. A, C, C, it ignores the present input (if you mistype).
   If you press C, B, A it turns the number into its negative, and if you
   press A, B, C it accepts the number and terminates input. To know where
   you are with your input, it prints X, Y and Z to let you track position
   within the chord. What it looks like on the terminal:

   XCYAZB0XBYAZB2XCYAZB0XCYBZA-XAYBZC* -20
   (Note, the above started with a leading 0, normally not needed.)
   This leads to an entry of the number -20.

   XAYBZA1XAYBZA1XAYBZC* 11
   This is 11.

   Actual user experience is better - because you see it being constructed.
   
   ADJUSTMENT: This version needs only 2 keypresses, working on 4 keys. */

long readint() {
  rd = 0;
  si = 0;
  p = 0;
  SRL.print(F(":"));
  int buttonpressed = 0;
  while (si != '*') { /* * is on phone and on numpad */

    delay(66);

    if (digitalRead(PIN_A) == HIGH) {
      rd = 'A';
    } else if (digitalRead(PIN_B) == HIGH) {
      rd = 'B'; // backspace
    } else if (digitalRead(PIN_C) == HIGH) {
      rd = 'C';
    } else if (digitalRead(PIN_D) == HIGH) {
      rd = 'D';
    }

    if ((pprerd == prerd) && (rd == prerd) && ((rd == 'A') || (rd == 'B') || (rd == 'C') || (rd == 'D'))) {
      if (PosX == 0) { PosX = rd; SRL.write('X'); SRL.write(rd); rd = 0; prerd = 0; pprerd = 0;}
      else if (PosY == 0) { PosY = rd; SRL.write('Y'); SRL.write(rd); rd = 0; prerd = 0; pprerd = 0;}
    } else {
      pprerd = prerd; prerd = rd;
    }


    if ((PosX != 0) && (PosY != 0)) {
      buttonpressed = 1;
      si = 0;
      if ((PosX == 'A' ) && (PosY == 'B')) { si = '1'; }
      else if ((PosX == 'B' ) && (PosY == 'A')) { si = '2'; }
      else if ((PosX == 'A' ) && (PosY == 'C')) { si = '3'; }
      else if ((PosX == 'C' ) && (PosY == 'A')) { si = '4'; }
      else if ((PosX == 'A' ) && (PosY == 'D')) { si = '5'; }
      else if ((PosX == 'D' ) && (PosY == 'A')) { si = '6'; }
      else if ((PosX == 'B' ) && (PosY == 'C')) { si = '7'; }
      else if ((PosX == 'C' ) && (PosY == 'B')) { si = '8'; }
      else if ((PosX == 'B' ) && (PosY == 'D')) { si = '9'; }
      else if ((PosX == 'D' ) && (PosY == 'B')) { si = '0'; }
      else if ((PosX == 'C' ) && (PosY == 'D')) { si = '*'; }
      else if ((PosX == 'D' ) && (PosY == 'C')) { si = '-'; }
      else if ((PosX == PosY) && ((PosY == 'A') || (PosY == 'B'))) { si = '#'; } // erase
      else if ((PosX == PosY) && ((PosY == 'C') || (PosY == 'D'))) { si = '?'; } // backspace
      else { buttonpressed = 0; } // safety
      prerd = 0; pprerd = 0; rd = 0;
      PosX = 0; PosY = 0;
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
      if (si != 0) { SRL.write(si); }
      buttonpressed = 0;
      if (si == '*') { break; }
      si = 0;
      pprerd = 0; prerd = 0; rd = 0;
      delay(333);
    }


  }

  SRL.print(F(" "));
  SRL.println(p);
  SRL.end();
  delay(20);
  SRL.begin(19200);
  delay(20);
  return p;
}


void setup() {

  pinMode(PIN_A, INPUT);
  pinMode(PIN_B, INPUT);
  pinMode(PIN_C, INPUT);
  pinMode(PIN_D, INPUT);

SRL.begin(300);

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

runlimit = 20 * EXECMEM; /* set it with -9, 0 means infinite. */
/* Default at over 1000. Now, if you need more, you are either doing something */
/* very smart, or you are doing something very stupid. */
/* It is better to have a "fool-proof" default than 0. */
/* This is particularly useful on systems where you cannot save your */
/* instructions - such as your desktop computer - and do not wish to lose */
/* everything just because the machine went into an infinite loop. */

while (1) {
pc = 0; /* safety: nuke the program counter and the immediate instruction */


/* Arduino: flush here any read-cache before reading instructions. */

/* First, get the command address - to determine is it +, 0 or - .*/
/* From there it will depend what action is to be undertaken. */
SRL.println();
SRL.print(F("C"));
cmdadr = readint(); 
/* atof and atoi due to lack of scanf */

/* COMMAND SELECTION IN REPL MODE */
/* Positive or zero - give the details of the instruction to be executed. */
if (cmdadr >= 0) {
  /* GIVE INSTRUCTION */
  SRL.print(F("O"));
  opr = readint(); 

  datadr1 = readint(); 

  datadr2 = readint(); 

  /* Compress six instructions into remaining "free" opr-values below 16. */
  if (opr == 28) {
    opr = 3;
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
  /* LIST INSTRUCTION RANGE -- NOW SIMPLY LIST EVERYTHING*/
  /* Give an output of the entered instructions between two addresses, */
  /* these addresses considered inclusive. Like "LIST" in BASIC, */
  /* to "let you see what you did" or "what you are about to run". */

  for (i = 1; i < EXECMEM; i++) {
    EEPROM.get((INSTRUCTION_BYTE_LENGTH * i) , instr);
    delay(10);

    SRL.print(i);
    /* as instructions and addresses are only positive, */
    /* the minus sign can be used as a visual separator. */
//    Serial.print(F(" "));
    SRL.print(-((int) ((instr >> 12) & 15)));
//    Serial.print(F(" "));
    SRL.print(-((int) ((instr >> 6) & 63)));
//    Serial.print(F(" "));
    SRL.println(-((int) (instr & 63)));


  } /* End of the instruction print. */

} else if (cmdadr == -2) {
  /* LIST DATA RANGE -- NOW SIMPLY LIST EVERYTHING */
  /* Same as above, just this time, for data, not instructions. */
  /* Listing data is simpler, just the numbers are given */
  /* - without the pointer interpretation attempted above. */

  for (i = 1; i < DATAMEM; i++) {
    SRL.print(datum[i]);
    SRL.println(-i);
  }


} else if (cmdadr == -4) {
  /* ENTER DATA AS FLOATS WITHIN A PRE-SPECIFIED RANGE */
  /* Same as above, just this time, you enter data. */
  /* What is different: obviously, negative numbers are totally allowed. */
  /* So this time, the range is not flexible, but determined by two limits. */

  datadr1 = readint(); 

  datadr2 = readint(); 

  while (datadr1 <= datadr2) {

    SRL.print(datadr1);

    f = readint(); 
    
    datum[datadr1] = f;
    datadr1++;
  }


} else if (cmdadr == -5) {
  /* CLEAR INSTRUCTION RANGE */
  /* Simply erase to zero an entire section of program space, i.e. to */
  /* "no operation". */

  SRL.println(F("Z")); /* "ZAP" */
  datadr1 = readint(); 
  datadr2 = readint(); 

  instr = 0; /* Just to make sure it "understands" the size as 32bit. */
  for (i = datadr1; i <= datadr2; i++) {
    EEPROM.put((INSTRUCTION_BYTE_LENGTH * i) , instr);
  }


} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0), not instructions. */

  for (i = datadr1; i < DATAMEM; i++) {
    datum[i] = 0;
  }

} else if (cmdadr == -9) {
  runlimit = readint(); 
} /* end of command selection */

} /* end of setup and maintenance */







/* DEBUG */
/*

xx = (instr >> 12) & 15;
yy = (instr >> 6) & 63;
zz = instr & 63;

SRL.print("Mid-air: instr=");
SRL.print(instr);
SRL.print(" EEPROMLONG[0]=");
EEPROM.get(ZERO_OFFSET, p);
SRL.print(p);
SRL.print(" xx=");
SRL.print(xx);
SRL.print(" yy=");
SRL.print(yy);
SRL.print(" zz=");
SRL.print(zz);
SRL.print(" oprBefore=");
SRL.print(opr);
SRL.print(" d1Before=");
SRL.print(datadr1);
SRL.print(" d2Before=");
SRL.print(datadr2);
SRL.println();


opr = xx;
datadr1 = yy;
datadr2 = zz;
/* */










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
    SRL.println(F("X"));
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
  }

 
  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) && (k > 0)) {
    /* DEBUG */ /*
    Serial.print("executing IADR: ");
    Serial.print(datadr1);
    Serial.print("-->");
    Serial.print("h=");
    Serial.print(h);
    Serial.print("-->");
    Serial.print(datum[h]);
    Serial.print(" ");
    Serial.print(datadr2);
    Serial.print("-->");
    Serial.print("k=");
    Serial.print(k);
    Serial.print("-->");
    Serial.print(datum[k]);
    Serial.println();
    /* */

    datadr1 = h;
    datadr2 = k;
    h = 0;
    k = 0;
  }

  /* DEBUG */ /*
  SRL.print("h=");
  SRL.print(h);
  SRL.print(" k=");
  SRL.print(k);
  SRL.print(" opr=");
  SRL.print(opr);
  SRL.print(" d1=");
  SRL.print(datadr1);
  SRL.print(" d2=");
  SRL.print(datadr2);
  SRL.print(" pc=");
  SRL.print(pc);
  SRL.println(); /* */

  /* ALWAYS trace execution. */
  SRL.print(pc);
  /* as instructions and addresses are only positive, */
  /* the minus sign can be used as a visual separator. */
  SRL.print(-opr);

  SRL.print(-datadr1);

  SRL.print(-datadr2);

  // SRL.println();
  SRL.print(F(" ")); tabula++; if (tabula == 2) { tabula = 0; SRL.println(); delay(200); }


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
/*   SRL.println("Entering decoder."); */

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

    /* DEBUG */ /*
    Serial.print("setting IADR: ");
    Serial.print(datadr1);
    Serial.print("-->");
    Serial.print("h=");
    Serial.print(h);
    Serial.print("-->");
    Serial.print(datum[h]);
    Serial.print(" ");
    Serial.print(datadr2);
    Serial.print("-->");
    Serial.print("k=");
    Serial.print(k);
    Serial.print("-->");
    Serial.print(datum[k]);
    Serial.println();
   /* */

  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */
    /* DEBUG */ /*
    SRL.println("Entered SADR");
    SRL.flush(); */

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
